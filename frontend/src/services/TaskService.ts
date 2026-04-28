import apiClient from '@/services/ApiService'
import ClassroomService from '@/services/ClassroomService'
import { TaskType, type TaskDTO, type NewsChallengeContent, type PhotoChallengeContent, type ApiResponse, type TeacherPhotoTaskDraft, type TeacherTaskDraft } from '@/types'

export async function fetchAllTasks(): Promise<TaskDTO[]> {
  const { data } = await apiClient.get<ApiResponse<TaskDTO[]>>('/tasks')
  return data.data.map(task => ({
    ...task,
    content: parseTaskContent(task.content as string, task.taskType)
  }))
}

export async function fetchTasksByStopLocation(stopLocation: string): Promise<TaskDTO[]> {
  const { data } = await apiClient.get<ApiResponse<TaskDTO[]>>('/tasks')
  return data.data
    .filter(task => task.stopLocation === stopLocation)
    .map(task => ({
      ...task,
      content: parseTaskContent(task.content as string, task.taskType)
    }))
}

export async function fetchTaskById(id: string): Promise<TaskDTO> {
  const { data } = await apiClient.get<ApiResponse<TaskDTO>>(`/tasks/${id}`)
  const task = data.data
  return {
    ...task,
    content: parseTaskContent(task.content as string, task.taskType)
  }
}

export async function createTeacherTask(classroomId: string, draft: TeacherTaskDraft): Promise<TaskDTO> {
  const formData = new FormData()
  const content = draft.type === TaskType.NEWS
    ? buildNewsContent(draft)
    : buildPhotoContent(draft)

  const taskPayload = {
    title: draft.title.trim(),
    taskType: draft.type,
    stopLocation: draft.difficulty === 4
      ? 'datasenteret'
      : draft.type === TaskType.NEWS ? 'nyhetskvartalet' : 'fotografen',
    difficulty: draft.difficulty,
    content: JSON.stringify(content),
  }

  formData.append(
    'task',
    new Blob([JSON.stringify(taskPayload)], { type: 'application/json' })
  )

  const response = await apiClient.post<ApiResponse<TaskDTO>>('/tasks', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })

  const task = response.data.data
  const classroomTasks = await ClassroomService.getClassroomTasks(classroomId, false, false)
  const nextSortOrder = classroomTasks.length + 1
  await ClassroomService.assignTaskToClassroom(classroomId, task.id, nextSortOrder, true)

  return {
    ...task,
    content: parseTaskContent(task.content as string, task.taskType)
  }

  function buildNewsContent(newsDraft: Extract<TeacherTaskDraft, { type: TaskType.NEWS }>): NewsChallengeContent {
    return {
      question: newsDraft.question.trim(),
      hint: newsDraft.hint.trim(),
      articles: newsDraft.articles.map((article, index) => ({
        imagePath: toUploadReference(article.image, `news-article-${index + 1}`),
        isReal: article.isReal,
      })),
    }
  }

  function buildPhotoContent(photoDraft: TeacherPhotoTaskDraft): PhotoChallengeContent {
    return {
      description: photoDraft.description.trim(),
      hint: photoDraft.hint.trim(),
      alternatives: photoDraft.alternatives.map((alternative, index) => ({
        imagePath: toUploadReference(alternative.image, `photo-alternative-${index + 1}`),
        description: alternative.description.trim(),
        correctType: alternative.correctType,
        begrunnelser: alternative.begrunnelserText
          .split('\n')
          .map(item => item.trim())
          .filter(Boolean),
      })),
    }
  }

  function toUploadReference(file: File | null, fallbackName: string): string {
    if (!file) {
      throw new Error('Mangler et opplastet bilde')
    }
    const safeFilename = `${fallbackName}-${file.name.replace(/\s+/g, '-').replace(/[^a-zA-Z0-9._-]/g, '')}`
    formData.append('images', file, safeFilename)
    return `upload://${safeFilename}`
  }
}

export async function updateTeacherTask(
  taskId: string,
  updates: { title?: string; difficulty?: number }
): Promise<TaskDTO> {
  const { data } = await apiClient.patch<ApiResponse<TaskDTO>>(`/tasks/${taskId}`, {
    ...(updates.title !== undefined && { title: updates.title.trim() }),
    ...(updates.difficulty !== undefined && { difficulty: updates.difficulty }),
  })
  return data.data
}

export function parseTaskContent(contentString: string, taskType: string) {
  try {
    const content = typeof contentString === 'string' ? JSON.parse(contentString) : contentString
    const normalizedContent = normalizeTaskAssetUrls(content)

    if (taskType === 'NEWS') {
      return { news: normalizedContent as NewsChallengeContent }
    } else if (taskType === 'PHOTO') {
      return { photo: normalizedContent as PhotoChallengeContent }
    }
    return normalizedContent
  } catch (error) {
    console.error('Failed to parse task content:', error)
    return null
  }
}

export function getTasksByStopLocation(tasks: TaskDTO[], stopLocation: string): TaskDTO[] {
  return tasks.filter(task => task.stopLocation === stopLocation)
}

export function getTasksByType(tasks: TaskDTO[], taskType: string): TaskDTO[] {
  return tasks.filter(task => task.taskType === taskType)
}

function normalizeTaskAssetUrls(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(item => normalizeTaskAssetUrls(item))
  }

  if (value !== null && typeof value === 'object') {
    const record = value as Record<string, unknown>
    return Object.fromEntries(
      Object.entries(record).map(([key, entryValue]) => {
        if (key === 'imagePath' && typeof entryValue === 'string') {
          return [key, resolveTaskAssetUrl(entryValue)]
        }
        return [key, normalizeTaskAssetUrls(entryValue)]
      })
    )
  }

  return value
}

function resolveTaskAssetUrl(path: string): string {
  if (!path.startsWith('/uploads/')) {
    return path
  }

  const apiBaseUrl = import.meta.env.VITE_API_URL as string | undefined
  if (!apiBaseUrl) {
    return path
  }

  const backendOrigin = apiBaseUrl.replace(/\/api\/v1\/?$/, '')
  return `${backendOrigin}${path}`
}

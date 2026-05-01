import apiClient from '@/services/ApiService'
import ClassroomService from '@/services/ClassroomService'
import { TaskType, type TaskDTO, type NewsChallengeContent, type PhotoChallengeContent, type ApiResponse, type TeacherPhotoTaskDraft, type TeacherTaskDraft, type EditTaskDraft } from '@/types'

export async function fetchAllTasks(): Promise<TaskDTO[]> {
  const { data } = await apiClient.get<ApiResponse<TaskDTO[]>>('/tasks')
  return data.data.map(task => ({
    ...task,
    content: parseTaskContent(task.content as unknown as string, task.taskType) as Record<string, unknown>
  }))
}

export async function fetchTasksByStopLocation(stopLocation: string): Promise<TaskDTO[]> {
  const { data } = await apiClient.get<ApiResponse<TaskDTO[]>>('/tasks')
  return data.data
    .filter(task => task.stopLocation === stopLocation)
    .map(task => ({
      ...task,
      content: parseTaskContent(task.content as unknown as string, task.taskType) as Record<string, unknown>
    }))
}

export async function fetchTaskById(id: string): Promise<TaskDTO> {
  const { data } = await apiClient.get<ApiResponse<TaskDTO>>(`/tasks/${id}`)
  const task = data.data
  return {
    ...task,
    content: parseTaskContent(task.content as unknown as string, task.taskType) as Record<string, unknown>
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
    stopLocation: draft.stopLocation ?? (draft.type === TaskType.NEWS ? 'nyhetskvartalet' : 'fotografen'),
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
        correctType: alternative.correctType,
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

export async function updateTeacherTaskFull(taskId: string, draft: EditTaskDraft): Promise<TaskDTO> {
  const hasNewImages = draft.type === TaskType.NEWS
    ? draft.articles.some(a => a.image)
    : draft.alternatives.some(a => a.image)

  if (hasNewImages) {
    return updateTeacherTaskMultipart(taskId, draft)
  }

  // JSON path — no new images, send everything as plain JSON
  let content: object
  if (draft.type === TaskType.NEWS) {
    content = {
      question: draft.question.trim(),
      hint: draft.hint.trim(),
      articles: draft.articles.map(a => ({
        imagePath: toRelativePath(a.existingImagePath ?? ''),
        isReal: a.isReal,
      })),
    }
  } else {
    content = {
      description: draft.description.trim(),
      hint: draft.hint.trim(),
      alternatives: draft.alternatives.map(a => ({
        imagePath: toRelativePath(a.existingImagePath ?? ''),
        correctType: a.correctType,
      })),
    }
  }

  const { data } = await apiClient.patch<ApiResponse<TaskDTO>>(`/tasks/${taskId}`, {
    title: draft.title.trim(),
    taskType: draft.type,
    difficulty: draft.difficulty,
    stopLocation: draft.stopLocation,
    content: JSON.stringify(content),
  })
  return data.data
}

async function updateTeacherTaskMultipart(taskId: string, draft: EditTaskDraft): Promise<TaskDTO> {
  const formData = new FormData()

  let content: object
  if (draft.type === TaskType.NEWS) {
    content = {
      question: draft.question.trim(),
      hint: draft.hint.trim(),
      articles: draft.articles.map((article, index) => ({
        imagePath: resolveEditImageRef(formData, article.image, article.existingImagePath, `news-article-${index + 1}`),
        isReal: article.isReal,
      })),
    }
  } else {
    content = {
      description: draft.description.trim(),
      hint: draft.hint.trim(),
      alternatives: draft.alternatives.map((alt, index) => ({
        imagePath: resolveEditImageRef(formData, alt.image, alt.existingImagePath, `photo-alternative-${index + 1}`),
        correctType: alt.correctType,
      })),
    }
  }

  formData.append('task', new Blob([JSON.stringify({
    title: draft.title.trim(),
    taskType: draft.type,
    difficulty: draft.difficulty,
    stopLocation: draft.stopLocation,
    content: JSON.stringify(content),
  })], { type: 'application/json' }))

  const { data } = await apiClient.patch<ApiResponse<TaskDTO>>(`/tasks/${taskId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data.data
}

function resolveEditImageRef(formData: FormData, newFile: File | null, existingPath: string | null, name: string): string {
  if (newFile) {
    const safeFilename = `${name}-${newFile.name.replace(/\s+/g, '-').replace(/[^a-zA-Z0-9._-]/g, '')}`
    formData.append('images', newFile, safeFilename)
    return `upload://${safeFilename}`
  }
  if (!existingPath) throw new Error(`Mangler bilde for ${name}`)
  return toRelativePath(existingPath)
}

function toRelativePath(path: string): string {
  const apiBaseUrl = import.meta.env.VITE_API_URL as string | undefined
  if (apiBaseUrl) {
    const origin = apiBaseUrl.replace(/\/api\/v1\/?$/, '')
    if (path.startsWith(origin)) return path.slice(origin.length)
  }
  return path
}

type ParsedContent = { news: NewsChallengeContent } | { photo: PhotoChallengeContent } | NewsChallengeContent | PhotoChallengeContent | null

export function parseTaskContent(contentString: string, taskType: string): ParsedContent {
  try {
    const content = typeof contentString === 'string' ? JSON.parse(contentString) : contentString
    const normalizedContent = normalizeTaskAssetUrls(content)

    if (taskType === 'NEWS') {
      return { news: normalizedContent as NewsChallengeContent }
    } else if (taskType === 'PHOTO') {
      return { photo: normalizedContent as PhotoChallengeContent }
    }
    return normalizedContent as NewsChallengeContent | PhotoChallengeContent
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

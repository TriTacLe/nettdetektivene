import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  parseTaskContent,
  getTasksByStopLocation,
  getTasksByType,
  fetchAllTasks,
  fetchTaskById,
  fetchTasksByStopLocation,
  updateTeacherTask,
} from '../../services/TaskService'
import { TaskType } from '../../types'
import type { TaskDTO } from '../../types'

vi.mock('@/services/ApiService', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}))

vi.mock('@/services/ClassroomService', () => ({
  default: {
    getClassroomTasks: vi.fn(),
    assignTaskToClassroom: vi.fn(),
  },
}))

import apiClient from '../../services/ApiService'

const makeTask = (overrides: Partial<TaskDTO> = {}): TaskDTO => ({
  id: '1',
  title: 'Test task',
  taskType: TaskType.NEWS,
  stopLocation: 'nyhetskvartalet',
  difficulty: 1,
  isActive: true,
  content: {},
  ...overrides,
})

describe('parseTaskContent', () => {
  it('parses valid NEWS content string', () => {
    const content = JSON.stringify({ question: 'Q?', hint: 'H', articles: [] })
    const result = parseTaskContent(content, 'NEWS')
    expect(result).toMatchObject({ news: { question: 'Q?', hint: 'H' } })
  })

  it('parses valid PHOTO content string', () => {
    const content = JSON.stringify({ description: 'D', hint: 'H', alternatives: [] })
    const result = parseTaskContent(content, 'PHOTO')
    expect(result).toMatchObject({ photo: { description: 'D', hint: 'H' } })
  })

  it('parses unknown task type and returns raw content', () => {
    const content = JSON.stringify({ foo: 'bar' })
    const result = parseTaskContent(content, 'OTHER')
    expect(result).toMatchObject({ foo: 'bar' })
  })

  it('returns null on invalid JSON', () => {
    const result = parseTaskContent('not-json', 'NEWS')
    expect(result).toBeNull()
  })

  it('handles already-parsed object (non-string)', () => {
    const content = { question: 'Q?', hint: 'H', articles: [] } as unknown as string
    const result = parseTaskContent(content, 'NEWS')
    expect(result).toMatchObject({ news: { question: 'Q?' } })
  })

  it('resolves /uploads/ imagePaths to full URL', () => {
    vi.stubEnv('VITE_API_URL', 'http://localhost:8080/api/v1')
    const content = JSON.stringify({
      question: 'Q',
      hint: 'H',
      articles: [{ imagePath: '/uploads/img.png', isReal: true }],
    })
    const result = parseTaskContent(content, 'NEWS') as {
      news: { articles: { imagePath: string }[] }
    }
    expect(result.news.articles[0].imagePath).toBe('http://localhost:8080/uploads/img.png')
    vi.unstubAllEnvs()
  })

  it('leaves non-upload imagePaths unchanged', () => {
    const content = JSON.stringify({
      description: 'D',
      hint: 'H',
      alternatives: [{ imagePath: 'http://cdn.example.com/img.png', correctType: 'real' }],
    })
    const result = parseTaskContent(content, 'PHOTO') as {
      photo: { alternatives: { imagePath: string }[] }
    }
    expect(result.photo.alternatives[0].imagePath).toBe('http://cdn.example.com/img.png')
  })
})

describe('getTasksByStopLocation', () => {
  const tasks = [
    makeTask({ id: '1', stopLocation: 'nyhetskvartalet' }),
    makeTask({ id: '2', taskType: TaskType.PHOTO, stopLocation: 'fotografen' }),
    makeTask({ id: '3', stopLocation: 'nyhetskvartalet' }),
  ]

  it('returns only tasks matching the stop location', () => {
    const result = getTasksByStopLocation(tasks, 'nyhetskvartalet')
    expect(result).toHaveLength(2)
    expect(result.every((t: TaskDTO) => t.stopLocation === 'nyhetskvartalet')).toBe(true)
  })

  it('returns empty array when no tasks match', () => {
    expect(getTasksByStopLocation(tasks, 'unknown')).toHaveLength(0)
  })

  it('returns empty array for empty input', () => {
    expect(getTasksByStopLocation([], 'nyhetskvartalet')).toHaveLength(0)
  })
})

describe('getTasksByType', () => {
  const tasks = [
    makeTask({ id: '1', taskType: TaskType.NEWS }),
    makeTask({ id: '2', taskType: TaskType.PHOTO }),
    makeTask({ id: '3', taskType: TaskType.NEWS }),
  ]

  it('returns only tasks matching the task type', () => {
    const result = getTasksByType(tasks, TaskType.NEWS)
    expect(result).toHaveLength(2)
  })

  it('returns empty array for unknown type', () => {
    expect(getTasksByType(tasks, 'UNKNOWN')).toHaveLength(0)
  })
})

describe('fetchAllTasks', () => {
  beforeEach(() => vi.clearAllMocks())

  it('returns mapped tasks with parsed content', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: {
        data: [
          {
            id: '1',
            title: 'T',
            taskType: 'NEWS',
            stopLocation: 'nyhetskvartalet',
            difficulty: 1,
            content: JSON.stringify({ question: 'Q', hint: 'H', articles: [] }),
          },
        ],
      },
    } as never)

    const tasks = await fetchAllTasks()
    expect(tasks).toHaveLength(1)
    expect(tasks[0].id).toBe('1')
    expect(vi.mocked(apiClient.get)).toHaveBeenCalledWith('/tasks')
  })

  it('returns empty array when API returns no tasks', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: { data: [] } } as never)
    expect(await fetchAllTasks()).toHaveLength(0)
  })
})

describe('fetchTaskById', () => {
  beforeEach(() => vi.clearAllMocks())

  it('fetches and parses a single task', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: {
        data: {
          id: '42',
          title: 'Task 42',
          taskType: 'PHOTO',
          stopLocation: 'fotografen',
          difficulty: 2,
          content: JSON.stringify({ description: 'D', hint: 'H', alternatives: [] }),
        },
      },
    } as never)

    const task = await fetchTaskById('42')
    expect(task.id).toBe('42')
    expect(vi.mocked(apiClient.get)).toHaveBeenCalledWith('/tasks/42')
  })
})

describe('fetchTasksByStopLocation', () => {
  beforeEach(() => vi.clearAllMocks())

  it('filters tasks by stop location', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: {
        data: [
          {
            id: '1',
            taskType: 'NEWS',
            stopLocation: 'nyhetskvartalet',
            difficulty: 1,
            content: '{}',
          },
          { id: '2', taskType: 'PHOTO', stopLocation: 'fotografen', difficulty: 1, content: '{}' },
        ],
      },
    } as never)

    const tasks = await fetchTasksByStopLocation('nyhetskvartalet')
    expect(tasks).toHaveLength(1)
    expect(tasks[0].id).toBe('1')
  })
})

describe('updateTeacherTask', () => {
  beforeEach(() => vi.clearAllMocks())

  it('patches title and difficulty', async () => {
    vi.mocked(apiClient.patch).mockResolvedValueOnce({
      data: { data: { id: '5', title: 'Updated', taskType: 'NEWS', difficulty: 3 } },
    } as never)

    const result = await updateTeacherTask('5', { title: 'Updated', difficulty: 3 })
    expect(result.title).toBe('Updated')
    expect(vi.mocked(apiClient.patch)).toHaveBeenCalledWith('/tasks/5', {
      title: 'Updated',
      difficulty: 3,
    })
  })

  it('only patches title when difficulty is undefined', async () => {
    vi.mocked(apiClient.patch).mockResolvedValueOnce({
      data: { data: { id: '5', title: 'Only title', taskType: 'NEWS', difficulty: 1 } },
    } as never)

    await updateTeacherTask('5', { title: 'Only title' })
    expect(vi.mocked(apiClient.patch)).toHaveBeenCalledWith('/tasks/5', { title: 'Only title' })
  })
})

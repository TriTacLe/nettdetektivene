import apiClient from '@/services/ApiService'
import type { NotificationDTO, NotificationResponseDTO } from '@/types'
import { NotificationType } from '@/types'

const REF_TYPE_MAP: Record<string, NotificationType> = {
  mystery_submission: NotificationType.MYSTERY_SUBMITTED,
  stop_completed: NotificationType.STOP_COMPLETED,
  game_completed: NotificationType.CLASS_COMPLETED_GAME,
  stuck: NotificationType.STUCK,
  inappropriate_message: NotificationType.INAPPROPRIATE_MESSAGE,
  inappropriate_username: NotificationType.INAPPROPRIATE_USERNAME,
}

function mapToNotificationDTO(raw: NotificationResponseDTO, studentName: string, classroomName: string): NotificationDTO {
  return {
    ...raw,
    classroomId: raw.classroomId ?? '',
    type: REF_TYPE_MAP[raw.refType ?? ''] ?? NotificationType.MYSTERY_SUBMITTED,
    studentName,
    classroomName,
  }
}

// TODO: remove mock data and uncomment API calls when backend endpoints are ready
const MOCK_NOTIFICATIONS: NotificationDTO[] = [
  {
    id: '1',
    type: NotificationType.STOP_COMPLETED,
    recipientId: 'teacher-1',
    studentName: 'Lars Olsen',
    classroomId: 'class-1',
    classroomName: '3A',
    msg: 'Fullførte området Nyhetskvartalet',
    refType: 'stop_completed',
    refId: null,
    createdAt: new Date(Date.now() - 1000 * 60 * 3).toISOString(),
    isRead: false,
  },
  {
    id: '2',
    type: NotificationType.STUCK,
    recipientId: 'teacher-1',
    studentName: 'Mia Hansen',
    classroomId: 'class-1',
    classroomName: '3A',
    msg: 'Har ikke kommet videre på Fotografen på over 10 minutter',
    refType: 'stuck',
    refId: null,
    createdAt: new Date(Date.now() - 1000 * 60 * 10).toISOString(),
    isRead: false,
  },
  {
    id: '3',
    type: NotificationType.INAPPROPRIATE_USERNAME,
    recipientId: 'teacher-1',
    studentName: 'Ukjent',
    classroomId: 'class-2',
    classroomName: '6B',
    msg: 'Upassende brukernavn forsøkt registrert',
    refType: 'inappropriate_username',
    refId: null,
    createdAt: new Date(Date.now() - 1000 * 60 * 20).toISOString(),
    isRead: true,
  },
  {
    id: '4',
    type: NotificationType.MYSTERY_SUBMITTED,
    recipientId: 'teacher-1',
    studentName: 'Jonas Berg',
    classroomId: 'class-2',
    classroomName: '6B',
    msg: 'Sendte inn svar på ukens mysterium',
    refType: 'mystery_submission',
    refId: 'mystery-42',
    createdAt: new Date(Date.now() - 1000 * 60 * 35).toISOString(),
    isRead: false,
  },
  {
    id: '5',
    type: NotificationType.CLASS_COMPLETED_GAME,
    recipientId: 'teacher-1',
    studentName: '',
    classroomId: 'class-1',
    classroomName: '3A',
    msg: 'Klassen fullførte spillet!',
    refType: 'game_completed',
    refId: null,
    createdAt: new Date(Date.now() - 1000 * 60 * 60).toISOString(),
    isRead: true,
  },
  {
    id: '6',
    type: NotificationType.INAPPROPRIATE_MESSAGE,
    recipientId: 'teacher-1',
    studentName: 'Emma Nilsen',
    classroomId: 'class-2',
    classroomName: '6B',
    msg: 'Upassende tekst skrevet i notatblokken',
    refType: 'inappropriate_message',
    refId: null,
    createdAt: new Date(Date.now() - 1000 * 60 * 90).toISOString(),
    isRead: false,
  },
  {
    id: '7',
    type: NotificationType.STOP_COMPLETED,
    recipientId: 'teacher-1',
    studentName: 'Tobias Dahl',
    classroomId: 'class-1',
    classroomName: '3A',
    msg: 'Fullførte området Fotografen',
    refType: 'stop_completed',
    refId: null,
    createdAt: new Date(Date.now() - 1000 * 60 * 120).toISOString(),
    isRead: true,
  },
]

export async function getNotifications(): Promise<NotificationDTO[]> {
  return Promise.resolve([...MOCK_NOTIFICATIONS])
  // TODO: when backend is ready:
  // const { data } = await apiClient.get<NotificationResponseDTO[]>('/notifications')
  // return data.map((n) => mapToNotificationDTO(n, /* fetch student/classroom names */))
}

export async function getNotificationsByClassroom(classroomId: string): Promise<NotificationDTO[]> {
  return Promise.resolve(MOCK_NOTIFICATIONS.filter((n) => n.classroomId === classroomId))
  // TODO: when backend is ready:
  // const { data } = await apiClient.get<NotificationResponseDTO[]>(`/notifications?classroomId=${classroomId}`)
  // return data.map((n) => mapToNotificationDTO(n, /* fetch student/classroom names */))
}

export async function markNotificationAsRead(id: string): Promise<void> {
  // TODO: when backend is ready:
  // await apiClient.patch(`/notifications/${id}/read`)
}

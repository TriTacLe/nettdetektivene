export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export enum TaskType {
  NEWS = 'NEWS',
  PHOTO = 'PHOTO',
  EMAIL = 'EMAIL',
  PASSWORD = 'PASSWORD',
  MARKETPLACE = 'MARKETPLACE',
  SOCIAL = 'SOCIAL',
  BOSS = 'BOSS',
}

export enum UserRole {
  TEACHER = 'instructor',
  STUDENT = 'student',
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface FeideLoginRequest {
  displayName?: string
  email: string
  affiliation: 'student' | 'instructor'
  password: string
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  roles: string[];
}

export interface FeideAuthResponse {
  token: string;
  refreshToken: string;
  roles: string[];
  feideId: string;
}

export interface RefreshRequest {
  refreshToken: string;
  expiredToken?: string;
}

export interface UserDTO {
  id: string;
  displayName: string;
  email: string;
  isActive: boolean;
  roles: string[];
  createdAt: string;
}

export interface InstitutionDTO {
  id: string;
  instName: string;
}

export interface ClassroomDTO {
  id: string;
  title: string;
  classDec: string | null;
  joinCode: string;
  isGameEnabled: boolean;
  institution: InstitutionDTO | null;
  createdAt: string;
}

export interface TaskDTO {
  id: string;
  title: string;
  taskType: TaskType;
  difficulty: number;
  isActive: boolean;
  stopLocation: string | null;
  content: NewsChallengeContent | PhotoChallengeContent | unknown;
  createdAt?: string;
  updatedAt?: string;
}

export interface NewsChallengeContent {
  hint?: string;
  articles?: NewsArticle[];
  question?: string;
}

export interface PhotoChallengeContent {
  description?: string;
  hint?: string;
  alternatives?: PhotoAlternative[];
}

export interface NotebookEntryDTO {
  id: string;
  userId: string;
  classroomId: string;
  stopLocation: string;
  autoNotes: string | null;
  studentReflection: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface NotebookEntryRequest {
  stopLocation: string;
  autoNotes?: string;
  studentReflection?: string;
}

export interface NewsArticle {
  imagePath: string;
  isReal: boolean;
}

export interface NewsChallenge {
  articles: [NewsArticle, NewsArticle];
}

export type PhotoType = 'ekte' | 'ai-generert' | 'manipulert'

export interface PhotoAlternative {
  imagePath: string;
  correctType: PhotoType;
}

export interface FotografenChallenge {
  alternatives: PhotoAlternative[];
}

export interface TeacherNewsArticleDraft {
  image: File | null
  isReal: boolean
}

export interface TeacherPhotoAlternativeDraft {
  image: File | null
  correctType: PhotoType
}

export interface TeacherTaskBaseDraft {
  title: string
  difficulty: number
  stopLocation?: string
}

export interface TeacherNewsTaskDraft extends TeacherTaskBaseDraft {
  type: TaskType.NEWS
  question: string
  hint: string
  articles: TeacherNewsArticleDraft[]
}

export interface TeacherPhotoTaskDraft extends TeacherTaskBaseDraft {
  type: TaskType.PHOTO
  description: string
  hint: string
  alternatives: TeacherPhotoAlternativeDraft[]
}

export type TeacherTaskDraft = TeacherNewsTaskDraft | TeacherPhotoTaskDraft

export interface EditArticleDraft {
  existingImagePath: string | null
  image: File | null
  isReal: boolean
}

export interface EditAlternativeDraft {
  existingImagePath: string | null
  image: File | null
  correctType: PhotoType
}

export interface EditNewsTaskDraft {
  type: TaskType.NEWS
  title: string
  difficulty: number
  stopLocation: string
  question: string
  hint: string
  articles: EditArticleDraft[]
}

export interface EditPhotoTaskDraft {
  type: TaskType.PHOTO
  title: string
  difficulty: number
  stopLocation: string
  description: string
  hint: string
  alternatives: EditAlternativeDraft[]
}

export type EditTaskDraft = EditNewsTaskDraft | EditPhotoTaskDraft

export interface ClassroomResponseDTO {
  id: string
  title: string
  classDesc: string | null
  joinCode: string
  institutionId: string
  createdById: string
  isGameEnabled: boolean
  createdAt: string
  updatedAt: string
}

export interface ClassroomTaskResponseDTO {
  classroomId: string
  taskId: string
  sortOrder: number
  isActive: boolean
  isUnlocked: boolean
  task: TaskDTO
}

export interface ClassroomRequestDTO {
  title: string
  classDesc?: string
  institutionId: string
  isGameEnabled: boolean
}

export interface ClassroomInstructorResponseDTO {
  classroomId: string
  userId: string
  assignedAt: string
}

export interface ClassroomStudentResponseDTO {
  userId: string
  displayName: string
  isAllowed: boolean
  nameChangeStatus: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED'
  requestedDisplayName: string | null
  joinedAt: string
}

export interface JoinClassroomRequestDTO {
  joinCode: string
}

export interface StopTaskDTO {
  taskId: number
  uuid: string
  title: string
  taskType: string
  isUnlocked: boolean
  content?: NewsChallengeContent | PhotoChallengeContent | unknown
}

export interface NewsAnswers {
  selectedIndex: number
}

export interface PhotoSelectionAnswer {
  type: PhotoType
}

export interface PhotoAnswers {
  selections: PhotoSelectionAnswer[]
}

export type TaskAnswers = NewsAnswers | PhotoAnswers

export interface TaskAttemptRequestDTO {
  taskId: string
  classroomId: string
  answers: TaskAnswers
}

export interface TaskAttemptResponseDTO {
  id: string
  userId: string
  taskId: string
  classroomId: string
  score: number
  passed: boolean
  attemptedAt: string
}

export enum NotificationType {
  STOP_COMPLETED = 'STOP_COMPLETED',
  CLASS_COMPLETED_GAME = 'CLASS_COMPLETED_GAME',
  STUCK = 'STUCK',
  MYSTERY_SUBMITTED = 'MYSTERY_SUBMITTED',
  INAPPROPRIATE_MESSAGE = 'INAPPROPRIATE_MESSAGE',
  INAPPROPRIATE_USERNAME = 'INAPPROPRIATE_USERNAME',
  JOIN_REQUEST = 'JOIN_REQUEST',
  NAME_CHANGE = 'NAME_CHANGE',
  FAILED = 'FAILED',
  CORRECT = 'CORRECT',
}

export interface NotificationResponseDTO {
  id: string
  recipientId: string
  classroomId: string | null
  msg: string
  refType: string | null
  refId: string | null
  isRead: boolean
  createdAt: string
}

export interface NotificationDTO {
  id: string
  type: NotificationType
  recipientId: string
  studentName: string
  classroomId: string
  classroomName: string
  msg: string
  refType: string | null
  refId: string | null
  isRead: boolean
  createdAt: string
}

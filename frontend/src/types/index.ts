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
  content: any;
  createdAt?: string;
  updatedAt?: string;
}

export interface NewsChallengeContent {
  hint?: string;
  articles?: any[];
  question?: string;
}

export interface PhotoChallengeContent {
  description?: string;
  alternatives?: any[];
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
  description: string;
  correctType: PhotoType;
  begrunnelser: string[];
}

export interface FotografenChallenge {
  alternatives: PhotoAlternative[];
}

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
  title: string
  taskType: string
}

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
  TEACHER = 'TEACHER',
  STUDENT = 'STUDENT',
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface UserDTO {
  id: string;
  displayName: string;
  username: string;
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

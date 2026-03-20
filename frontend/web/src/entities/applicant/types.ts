export interface LoginRequest {
  email: string;
  password: string;
}

export interface JoinRequest {
  email: string;
  password: string;
  name: string;
}

export interface LoginResponse {
  uuid: string;
  email: string;
  name: string;
  accessToken: string;
  refreshToken?: string;
}

export interface ApplicantProfile {
  uuid: string;
  email: string;
  name: string;
  createdAt: string;
}

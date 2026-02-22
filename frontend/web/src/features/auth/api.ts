import { apiClient } from '@/shared/api';
import type { ApiResponse } from '@/shared/types/api';
import type { LoginRequest, JoinRequest, LoginResponse, ApplicantProfile } from '@/entities/applicant';

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<ApiResponse<LoginResponse>>('/applicant/login', data)
      .then((res) => res.data),

  join: (data: JoinRequest) =>
    apiClient.post<ApiResponse<null>>('/applicant/join', data)
      .then((res) => res.data),

  getProfile: () =>
    apiClient.get<ApiResponse<ApplicantProfile>>('/applicant/profile')
      .then((res) => res.data),
};

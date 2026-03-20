import { apiClient } from '@/shared/api';
import type { ApiResponse } from '@/shared/types/api';
import type { Resume, UploadResumeParams } from '@/entities/resume';

export const resumeApi = {
  getMyResumes: () =>
    apiClient.get<ApiResponse<Resume[]>>('/applicant/resume')
      .then((res) => res.data),

  uploadResume: ({ title, description, file }: UploadResumeParams) => {
    const formData = new FormData();
    formData.append('file', file);

    const params = new URLSearchParams();
    params.set('title', title);
    if (description) params.set('description', description);

    return apiClient.post<ApiResponse<Resume>>(
      `/applicant/resume?${params.toString()}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    ).then((res) => res.data);
  },

  deleteResume: (resumeSlug: string) =>
    apiClient.delete<ApiResponse<null>>(`/applicant/resume/${resumeSlug}`)
      .then((res) => res.data),
};

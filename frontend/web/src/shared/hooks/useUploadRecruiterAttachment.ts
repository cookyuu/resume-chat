import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/shared/api/client';
import toast from 'react-hot-toast';

interface UploadRecruiterAttachmentParams {
  sessionToken: string;
  file: File;
}

interface AttachmentUploadResponse {
  attachmentId: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  fileUrl: string;
  uploadedAt: string;
}

interface ApiResponse {
  success: boolean;
  data: AttachmentUploadResponse;
  error?: {
    code: string;
    message: string;
  };
}

/**
 * 채용담당자용 파일 첨부 훅
 * - 인증 불필요 (public API)
 * - 엔드포인트: /chat/session/{sessionToken}/attachments
 */
export function useUploadRecruiterAttachment(queryKey: readonly unknown[]) {
  const queryClient = useQueryClient();
  const [uploadProgress, setUploadProgress] = useState(0);

  const mutation = useMutation({
    mutationFn: async ({ sessionToken, file }: UploadRecruiterAttachmentParams) => {
      const formData = new FormData();
      formData.append('file', file);

      const { data } = await apiClient.post<ApiResponse>(
        `/chat/session/${sessionToken}/attachments`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
          onUploadProgress: (progressEvent) => {
            const progress = progressEvent.total
              ? Math.round((progressEvent.loaded * 100) / progressEvent.total)
              : 0;
            setUploadProgress(progress);
          },
        }
      );

      return data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setUploadProgress(0);
      toast.success('파일이 업로드되었습니다');
    },
    onError: (error: any) => {
      setUploadProgress(0);
      const errorMessage = error?.response?.data?.error?.message || '파일 업로드에 실패했습니다';
      toast.error(errorMessage);
    },
  });

  return {
    ...mutation,
    uploadProgress,
  };
}

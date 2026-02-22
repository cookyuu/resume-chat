import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { resumeApi } from './api';
import { queryKeys } from '@/shared/lib/queryKeys';
import type { UploadResumeParams } from '@/entities/resume';

/** 내 이력서 목록 조회 */
export function useMyResumes() {
  return useQuery({
    queryKey: queryKeys.resume.list,
    queryFn: () => resumeApi.getMyResumes(),
    select: (res) => res.data,
  });
}

/** 이력서 업로드 */
export function useUploadResume() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (params: UploadResumeParams) => resumeApi.uploadResume(params),
    onSuccess: () => {
      toast.success('이력서가 업로드되었습니다.');
      queryClient.invalidateQueries({ queryKey: queryKeys.resume.list });
    },
  });
}

/** 이력서 삭제 (관련 채팅 세션도 cascade 삭제되므로 chat 캐시도 무효화) */
export function useDeleteResume() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (resumeSlug: string) => resumeApi.deleteResume(resumeSlug),
    onSuccess: (_data, resumeSlug) => {
      toast.success('이력서가 삭제되었습니다.');
      queryClient.invalidateQueries({ queryKey: queryKeys.resume.list });
      queryClient.invalidateQueries({ queryKey: queryKeys.chat.sessions(resumeSlug) });
    },
  });
}

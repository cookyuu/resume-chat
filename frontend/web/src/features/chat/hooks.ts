import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { chatApi } from './api';
import { queryKeys } from '@/shared/lib/queryKeys';
import type { RecruiterEnterRequest, RecruiterSendMessageRequest, ApplicantSendMessageRequest } from '@/entities/chat';

// ── 채용담당자 (Public) ──

/** 채용담당자 채팅방 입장 (세션 조회/생성) */
export function useEnterRecruiterChat(resumeSlug: string) {
  return useMutation({
    mutationFn: (data: RecruiterEnterRequest) =>
      chatApi.enterRecruiterChat(resumeSlug, data),
  });
}

/** 채용담당자용 메시지 목록 조회 */
export function useRecruiterMessages(sessionToken: string | null) {
  return useQuery({
    queryKey: queryKeys.recruiterChat.messages(sessionToken ?? ''),
    queryFn: () => chatApi.getRecruiterMessages(sessionToken!),
    select: (res) => res.data,
    enabled: !!sessionToken,
  });
}

/** 채용담당자 메시지 전송 */
export function useSendRecruiterChatMessage(sessionToken: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: RecruiterSendMessageRequest) =>
      chatApi.sendRecruiterChatMessage(sessionToken, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.recruiterChat.messages(sessionToken) });
    },
  });
}

// ── 지원자 (Protected) ──

/** 이력서별 채팅 세션 목록 조회 */
export function useResumeChats(resumeSlug: string) {
  return useQuery({
    queryKey: queryKeys.chat.sessions(resumeSlug),
    queryFn: () => chatApi.getResumeChats(resumeSlug),
    select: (res) => res.data,
    enabled: !!resumeSlug,
  });
}

/** 채팅 세션 메시지 조회 */
export function useSessionMessages(sessionToken: string) {
  return useQuery({
    queryKey: queryKeys.chat.messages(sessionToken),
    queryFn: () => chatApi.getSessionMessages(sessionToken),
    select: (res) => res.data,
    enabled: !!sessionToken,
  });
}

/** 지원자 메시지 전송 */
export function useSendApplicantMessage(sessionToken: string, resumeSlug?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ApplicantSendMessageRequest) =>
      chatApi.sendApplicantMessage(sessionToken, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.chat.messages(sessionToken) });
      if (resumeSlug) {
        queryClient.invalidateQueries({ queryKey: queryKeys.chat.sessions(resumeSlug) });
      }
    },
  });
}

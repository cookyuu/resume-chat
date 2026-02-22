import { apiClient } from '@/shared/api';
import type { ApiResponse } from '@/shared/types/api';
import type {
  RecruiterEnterRequest,
  RecruiterEnterResponse,
  RecruiterSendMessageRequest,
  ApplicantSendMessageRequest,
  SendMessageResponse,
  ResumeChatList,
  SessionMessages,
} from '@/entities/chat';

export const chatApi = {
  // ── 채용담당자 (Public) ──
  enterRecruiterChat: (resumeSlug: string, data: RecruiterEnterRequest) =>
    apiClient.post<ApiResponse<RecruiterEnterResponse>>(`/chat/${resumeSlug}/enter`, data)
      .then((res) => res.data),

  getRecruiterMessages: (sessionToken: string) =>
    apiClient.get<ApiResponse<SessionMessages>>(`/chat/session/${sessionToken}/messages`)
      .then((res) => res.data),

  sendRecruiterChatMessage: (sessionToken: string, data: RecruiterSendMessageRequest) =>
    apiClient.post<ApiResponse<SendMessageResponse>>(`/chat/session/${sessionToken}/send`, data)
      .then((res) => res.data),

  // ── 지원자 (Protected) ──
  getResumeChats: (resumeSlug: string) =>
    apiClient.get<ApiResponse<ResumeChatList>>(`/applicant/resume/${resumeSlug}/chats`)
      .then((res) => res.data),

  getSessionMessages: (sessionToken: string) =>
    apiClient.get<ApiResponse<SessionMessages>>(`/applicant/chat/${sessionToken}/messages`)
      .then((res) => res.data),

  sendApplicantMessage: (sessionToken: string, data: ApplicantSendMessageRequest) =>
    apiClient.post<ApiResponse<SendMessageResponse>>(`/applicant/chat/${sessionToken}/send`, data)
      .then((res) => res.data),
};

export interface RecruiterEnterRequest {
  recruiterEmail: string;
  recruiterName: string;
  recruiterCompany: string;
}

export interface RecruiterEnterResponse {
  sessionToken: string;
  resumeTitle: string;
  recruiterName: string;
  recruiterCompany: string;
}

export interface RecruiterSendMessageRequest {
  message: string;
}

export interface ApplicantSendMessageRequest {
  message: string;
}

export interface SendMessageResponse {
  sessionToken: string;
  messageId: string;
  recruiterEmail?: string;
  recruiterName?: string;
  recruiterCompany?: string;
  message: string;
  sentAt: string;
}

export interface ChatSession {
  sessionToken: string;
  resumeSlug: string;
  resumeTitle: string;
  recruiterEmail: string;
  recruiterName: string;
  recruiterCompany: string;
  applicantEmail?: string;  // 지원자 이메일 (백엔드 추가 필요)
  applicantName?: string;   // 지원자 이름 (백엔드 추가 필요)
  totalMessages: number;
  unreadMessages: number;
  lastMessageAt: string;
  createdAt: string;
}

export interface ResumeChatList {
  resumeSlug: string;
  resumeTitle: string;
  sessions: ChatSession[];
}

export type SenderType = 'RECRUITER' | 'APPLICANT';

export interface ChatMessage {
  messageId: string;
  message: string;
  senderType: SenderType;
  readStatus: boolean;
  sentAt: string;
}

export interface SessionMessages {
  session: ChatSession;
  messages: ChatMessage[];
}

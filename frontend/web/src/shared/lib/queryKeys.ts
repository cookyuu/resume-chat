export const queryKeys = {
  applicant: {
    profile: ['applicant', 'profile'] as const,
  },
  resume: {
    list: ['resume', 'list'] as const,
  },
  chat: {
    sessions: (resumeSlug: string) => ['chat', 'sessions', resumeSlug] as const,
    messages: (sessionToken: string) => ['chat', 'messages', sessionToken] as const,
  },
  recruiterChat: {
    messages: (sessionToken: string) => ['recruiterChat', 'messages', sessionToken] as const,
  },
} as const;

import { useState, useRef, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useEnterRecruiterChat, useRecruiterMessages, useSendRecruiterChatMessage } from '@/features/chat';
import type { RecruiterEnterResponse } from '@/entities/chat';
import { Button, Input, Skeleton, EmptyState } from '@/shared/ui';
import { formatDateTime } from '@/shared/lib/date';

export function RecruiterChatPage() {
  const { resumeSlug } = useParams<{ resumeSlug: string }>();
  const [session, setSession] = useState<RecruiterEnterResponse | null>(null);

  return (
    <>
      {!session && <RecruiterEntryModal resumeSlug={resumeSlug!} onEnter={setSession} />}
      {session && <RecruiterChatRoom session={session} />}
    </>
  );
}

// ── 입장 모달 ──

function RecruiterEntryModal({
  resumeSlug,
  onEnter,
}: {
  resumeSlug: string;
  onEnter: (session: RecruiterEnterResponse) => void;
}) {
  const enterMutation = useEnterRecruiterChat(resumeSlug);

  const [form, setForm] = useState({
    recruiterEmail: '',
    recruiterName: '',
    recruiterCompany: '',
  });

  const [touched, setTouched] = useState({ recruiterName: false, recruiterCompany: false });
  const handleBlur = (field: keyof typeof touched) => () =>
    setTouched((prev) => ({ ...prev, [field]: true }));

  const nameError =
    touched.recruiterName && form.recruiterName.length > 0 && form.recruiterName.length < 2
      ? '이름은 2자 이상이어야 합니다'
      : undefined;
  const companyError =
    touched.recruiterCompany && form.recruiterCompany.length > 0 && form.recruiterCompany.length < 2
      ? '회사명은 2자 이상이어야 합니다'
      : undefined;

  const handleChange =
    (field: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: e.target.value }));
    };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    enterMutation.mutate(form, {
      onSuccess: (res) => onEnter(res.data),
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-md mx-4 p-6 bg-white rounded-xl shadow-lg">
        <h2 className="text-xl font-bold text-center mb-1">채용 담당자 채팅</h2>
        <p className="text-sm text-gray-500 text-center mb-5">
          정보를 입력하고 지원자와 대화를 시작하세요
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="이메일"
            type="email"
            placeholder="채용담당자 이메일"
            value={form.recruiterEmail}
            onChange={handleChange('recruiterEmail')}
            required
          />
          <Input
            label="이름"
            placeholder="채용담당자 이름 (2~50자)"
            value={form.recruiterName}
            onChange={handleChange('recruiterName')}
            onBlur={handleBlur('recruiterName')}
            error={nameError}
            required
            minLength={2}
            maxLength={50}
          />
          <Input
            label="회사명"
            placeholder="회사명 (2~100자)"
            value={form.recruiterCompany}
            onChange={handleChange('recruiterCompany')}
            onBlur={handleBlur('recruiterCompany')}
            error={companyError}
            required
            minLength={2}
            maxLength={100}
          />
          <Button type="submit" loading={enterMutation.isPending} className="w-full">
            채팅 시작
          </Button>
        </form>
      </div>
    </div>
  );
}

// ── 채팅방 ──

function RecruiterChatRoom({ session }: { session: RecruiterEnterResponse }) {
  const { data, isLoading, isError } = useRecruiterMessages(session.sessionToken);
  const sendMutation = useSendRecruiterChatMessage(session.sessionToken);

  const [message, setMessage] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [data?.messages]);

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!message.trim()) return;
    sendMutation.mutate(
      { message: message.trim() },
      { onSuccess: () => setMessage('') },
    );
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col">
        <div className="max-w-4xl w-full mx-auto p-6 space-y-4 flex-1">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className={`h-12 ${i % 2 === 0 ? 'w-2/3' : 'w-1/2 ml-auto'}`} />
          ))}
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-red-500">메시지를 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-screen bg-gray-50">
      {/* Header */}
      <div className="px-4 py-3 border-b bg-white flex items-center gap-4">
        <div className="min-w-0">
          <h2 className="font-semibold truncate">{session.resumeTitle}</h2>
          <p className="text-xs text-gray-500 truncate">
            {session.recruiterName} &middot; {session.recruiterCompany}
          </p>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {!data?.messages || data.messages.length === 0 ? (
          <EmptyState message="아직 메시지가 없습니다. 첫 메시지를 보내보세요!" />
        ) : (
          data.messages.map((msg) => {
            const isRecruiter = msg.senderType === 'RECRUITER';
            return (
              <div
                key={msg.messageId}
                className={`flex ${isRecruiter ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[70%] px-4 py-2.5 rounded-2xl ${
                    isRecruiter
                      ? 'bg-blue-600 text-white rounded-br-md'
                      : 'bg-white border rounded-bl-md'
                  }`}
                >
                  <p className="text-sm whitespace-pre-wrap">{msg.message}</p>
                  <p
                    className={`text-xs mt-1 ${
                      isRecruiter ? 'text-blue-200' : 'text-gray-400'
                    }`}
                  >
                    {formatDateTime(msg.sentAt)}
                  </p>
                </div>
              </div>
            );
          })
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <form onSubmit={handleSend} className="p-3 bg-white border-t flex items-center gap-2">
        <input
          type="text"
          className="flex-1 px-4 py-2.5 border rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="메시지를 입력하세요..."
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          maxLength={1000}
        />
        <Button type="submit" loading={sendMutation.isPending} className="rounded-full px-5">
          전송
        </Button>
      </form>
    </div>
  );
}

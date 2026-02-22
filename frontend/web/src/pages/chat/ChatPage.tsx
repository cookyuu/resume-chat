import { useState, useRef, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useSessionMessages, useSendApplicantMessage } from '@/features/chat';
import { Button, Skeleton, EmptyState } from '@/shared/ui';
import { formatDateTime } from '@/shared/lib/date';

export function ChatPage() {
  const { sessionToken } = useParams<{ sessionToken: string }>();
  const { data, isLoading, isError } = useSessionMessages(sessionToken!);
  const resumeSlug = data?.session?.resumeSlug;
  const sendMutation = useSendApplicantMessage(sessionToken!, resumeSlug);

  const [message, setMessage] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [data?.messages]);

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!message.trim()) return;
    sendMutation.mutate({ message: message.trim() }, {
      onSuccess: () => setMessage(''),
    });
  };

  if (isLoading) {
    return (
      <div className="p-6 space-y-4">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className={`h-12 ${i % 2 === 0 ? 'w-2/3' : 'w-1/2 ml-auto'}`} />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="p-6 max-w-4xl mx-auto">
        <Link to="/resumes" className="text-sm text-blue-600 hover:underline">&larr; 이력서 목록</Link>
        <p className="mt-4 text-red-500">채팅 메시지를 불러올 수 없습니다.</p>
      </div>
    );
  }

  const session = data?.session;

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] max-w-4xl mx-auto">
      {/* Header */}
      <div className="px-4 py-3 border-b bg-white flex items-center gap-4">
        <Link
          to={session ? `/resumes/${session.resumeSlug}/chats` : '/resumes'}
          className="text-sm text-blue-600 hover:underline shrink-0"
        >
          &larr; 돌아가기
        </Link>
        {session && (
          <div className="min-w-0">
            <h2 className="font-semibold truncate">{session.recruiterName}</h2>
            <p className="text-xs text-gray-500 truncate">
              {session.recruiterCompany} &middot; {session.resumeTitle}
            </p>
          </div>
        )}
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-gray-50">
        {!data?.messages || data.messages.length === 0 ? (
          <EmptyState message="아직 메시지가 없습니다" />
        ) : (
          data.messages.map((msg) => {
            const isApplicant = msg.senderType === 'APPLICANT';
            return (
              <div
                key={msg.messageId}
                className={`flex ${isApplicant ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[70%] px-4 py-2.5 rounded-2xl ${
                    isApplicant
                      ? 'bg-blue-600 text-white rounded-br-md'
                      : 'bg-white border rounded-bl-md'
                  }`}
                >
                  <p className="text-sm whitespace-pre-wrap">{msg.message}</p>
                  <p
                    className={`text-xs mt-1 ${
                      isApplicant ? 'text-blue-200' : 'text-gray-400'
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
        <Button type="submit" loading={sendMutation.isPending} className="rounded-full px-5">전송</Button>
      </form>
    </div>
  );
}

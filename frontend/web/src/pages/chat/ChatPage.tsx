import { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useSessionMessages, useSendApplicantMessage } from '@/features/chat';
import { Button, Skeleton, EmptyState, TypingIndicator } from '@/shared/ui';
import { formatDateTime } from '@/shared/lib/date';
import { getWebSocketClient, type ConnectionStatus } from '@/shared/api/websocket';
import { useChatWebSocket } from '@/shared/hooks/useChatWebSocket';
import { useTypingIndicator } from '@/shared/hooks/useTypingIndicator';
import { chatQueryKeys } from '@/shared/lib/queryKeys';
import { useDarkMode } from '@/shared/hooks/useDarkMode';
import { SunIcon, MoonIcon } from '@heroicons/react/24/outline';

export function ChatPage() {
  const { sessionToken } = useParams<{ sessionToken: string }>();
  const { data, isLoading, isError } = useSessionMessages(sessionToken!);
  const resumeSlug = data?.session?.resumeSlug;
  const sendMutation = useSendApplicantMessage(sessionToken!, resumeSlug);

  // 세션별 독립 다크모드
  const { isDark, toggleDarkMode } = useDarkMode({ sessionToken });

  const [message, setMessage] = useState('');
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('DISCONNECTED');
  const [recruiterOnline, setRecruiterOnline] = useState(false);
  const [recruiterTyping, setRecruiterTyping] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [data?.messages]);

  // ✅ queryKey를 메모이제이션하여 참조 동일성 유지
  const queryKey = useMemo(() => chatQueryKeys.messages(sessionToken!), [sessionToken]);

  // ✅ 콜백 함수를 useCallback으로 메모이제이션
  const handleStatusChange = useCallback((status: ConnectionStatus) => {
    setConnectionStatus(status);
  }, []);

  const handlePresenceChange = useCallback((online: boolean) => {
    setRecruiterOnline(online);
  }, []);

  // WebSocket 연결 및 구독 (커스텀 훅 사용)
  useChatWebSocket({
    sessionToken: sessionToken!,
    queryKey,
    onStatusChange: handleStatusChange,
    onPresenceChange: handlePresenceChange,
    counterpartType: 'RECRUITER',
  });

  // 타이핑 인디케이터
  const { handleInput: handleTypingInput } = useTypingIndicator({
    sessionToken: sessionToken!,
    senderType: 'APPLICANT',
    onCounterpartTyping: setRecruiterTyping,
  });

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
        <div className="mt-4 text-center">
          <p className="text-red-500 mb-4">채팅 메시지를 불러올 수 없습니다.</p>
          <Button onClick={() => window.location.reload()}>다시 시도</Button>
        </div>
      </div>
    );
  }

  const session = data?.session;

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] max-w-4xl mx-auto">
      {/* Header */}
      <div className="px-4 py-3 border-b dark:border-gray-700 bg-white dark:bg-gray-800 flex items-center gap-4 transition-colors">
        <Link
          to={session ? `/resumes/${session.resumeSlug}/chats` : '/resumes'}
          className="text-sm text-blue-600 hover:underline shrink-0"
        >
          &larr; 돌아가기
        </Link>
        {session && (
          <div className="min-w-0 flex-1">
            <h2 className="font-semibold truncate dark:text-white">{session.recruiterName}</h2>
            <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
              {session.recruiterCompany} &middot; {session.resumeTitle}
            </p>
          </div>
        )}
        {/* 접속 상태 표시 */}
        <div className="flex items-center gap-1.5 text-xs shrink-0">
          <div
            className={`w-2 h-2 rounded-full ${
              connectionStatus === 'CONNECTED' && recruiterOnline
                ? 'bg-green-500'
                : connectionStatus === 'CONNECTING' || connectionStatus === 'RECONNECTING'
                ? 'bg-yellow-500 animate-pulse'
                : 'bg-gray-400'
            }`}
          />
          <span className="text-gray-600 dark:text-gray-400">
            {connectionStatus === 'CONNECTING'
              ? '연결 중...'
              : connectionStatus === 'RECONNECTING'
              ? '재연결 중...'
              : connectionStatus === 'CONNECTED'
              ? recruiterOnline
                ? '접속중'
                : '미접속'
              : '오프라인'}
          </span>
        </div>
        {/* 다크모드 토글 */}
        <button
          onClick={toggleDarkMode}
          className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors shrink-0"
          aria-label="다크모드 토글"
        >
          {isDark ? (
            <SunIcon className="w-5 h-5 text-gray-600 dark:text-gray-300" />
          ) : (
            <MoonIcon className="w-5 h-5 text-gray-600 dark:text-gray-300" />
          )}
        </button>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-gray-50 dark:bg-gray-900 transition-colors">
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
                  className={`max-w-[70%] px-4 py-2.5 rounded-2xl transition-colors ${
                    isApplicant
                      ? 'bg-blue-600 dark:bg-blue-500 text-white rounded-br-md'
                      : 'bg-white dark:bg-gray-800 border dark:border-gray-700 dark:text-white rounded-bl-md'
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
        {/* 타이핑 인디케이터 */}
        {recruiterTyping && <TypingIndicator name="채용담당자" />}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <form onSubmit={handleSend} className="p-3 bg-white dark:bg-gray-800 border-t dark:border-gray-700 flex items-center gap-2 transition-colors">
        <input
          type="text"
          className="flex-1 px-4 py-2.5 border dark:border-gray-600 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-colors"
          placeholder="메시지를 입력하세요..."
          value={message}
          onChange={(e) => {
            setMessage(e.target.value);
            handleTypingInput(); // 타이핑 이벤트 발행
          }}
          maxLength={1000}
        />
        <Button type="submit" loading={sendMutation.isPending} className="rounded-full px-5">전송</Button>
      </form>

      {/* 재연결 버튼 (연결 실패 시에만 표시) */}
      {connectionStatus === 'DISCONNECTED' && (
        <div className="fixed bottom-20 left-1/2 transform -translate-x-1/2 bg-red-500 text-white px-4 py-2 rounded-lg shadow-lg flex items-center gap-3">
          <span className="text-sm">연결이 끊어졌습니다</span>
          <Button
            onClick={() => getWebSocketClient().manualReconnect()}
            className="bg-white text-red-500 hover:bg-gray-100 text-sm px-3 py-1"
          >
            다시 연결
          </Button>
        </div>
      )}
    </div>
  );
}

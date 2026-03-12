import { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useEnterRecruiterChat, useRecruiterMessages, useSendRecruiterChatMessage } from '@/features/chat';
import type { RecruiterEnterResponse } from '@/entities/chat';
import { Button, Input, Skeleton, EmptyState, TypingIndicator } from '@/shared/ui';
import { formatDateTime } from '@/shared/lib/date';
import { getWebSocketClient, type ConnectionStatus } from '@/shared/api/websocket';
import { useChatWebSocket } from '@/shared/hooks/useChatWebSocket';
import { useTypingIndicator } from '@/shared/hooks/useTypingIndicator';
import { recruiterChatQueryKeys } from '@/shared/lib/queryKeys';
import { useDarkMode } from '@/shared/hooks/useDarkMode';
import { SunIcon, MoonIcon } from '@heroicons/react/24/outline';

const STORAGE_KEY_PREFIX = 'recruiter_session_';

export function RecruiterChatPage() {
  // 다크모드 초기화 (AppLayout 없는 public 페이지)
  useDarkMode();

  const { resumeSlug } = useParams<{ resumeSlug: string }>();
  const [session, setSession] = useState<RecruiterEnterResponse | null>(() => {
    // localStorage에서 세션 복원 시도
    try {
      const stored = localStorage.getItem(`${STORAGE_KEY_PREFIX}${resumeSlug}`);
      if (stored) {
        const parsed = JSON.parse(stored);
        // 세션 유효성 간단 체크 (필요시 만료 시간 체크 추가 가능)
        if (parsed.sessionToken && parsed.resumeSlug === resumeSlug) {
          return parsed;
        }
      }
    } catch (error) {
      console.error('[RecruiterChatPage] Failed to restore session:', error);
    }
    return null;
  });

  const handleEnter = (newSession: RecruiterEnterResponse) => {
    setSession(newSession);
    // localStorage에 세션 저장
    try {
      localStorage.setItem(`${STORAGE_KEY_PREFIX}${resumeSlug}`, JSON.stringify(newSession));
    } catch (error) {
      console.error('[RecruiterChatPage] Failed to save session:', error);
    }
  };

  return (
    <>
      {!session && <RecruiterEntryModal resumeSlug={resumeSlug!} onEnter={handleEnter} />}
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
      <div className="w-full max-w-md mx-4 p-6 bg-white dark:bg-gray-800 rounded-xl shadow-lg transition-colors">
        <h2 className="text-xl font-bold text-center mb-1 dark:text-white">채용 담당자 채팅</h2>
        <p className="text-sm text-gray-500 dark:text-gray-400 text-center mb-5">
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
  const { isDark, toggleDarkMode } = useDarkMode();

  const [message, setMessage] = useState('');
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('DISCONNECTED');
  const [applicantOnline, setApplicantOnline] = useState(false);
  const [applicantTyping, setApplicantTyping] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [data?.messages]);

  // ✅ queryKey를 메모이제이션하여 참조 동일성 유지
  const queryKey = useMemo(
    () => recruiterChatQueryKeys.messages(session.sessionToken),
    [session.sessionToken]
  );

  // ✅ 콜백 함수를 useCallback으로 메모이제이션
  const handleStatusChange = useCallback((status: ConnectionStatus) => {
    setConnectionStatus(status);
  }, []);

  const handlePresenceChange = useCallback((online: boolean) => {
    setApplicantOnline(online);
  }, []);

  // WebSocket 연결 및 구독 (커스텀 훅 사용)
  useChatWebSocket({
    sessionToken: session.sessionToken,
    queryKey,
    onStatusChange: handleStatusChange,
    onPresenceChange: handlePresenceChange,
    counterpartType: 'APPLICANT',
  });

  // 타이핑 인디케이터
  const { handleInput: handleTypingInput } = useTypingIndicator({
    sessionToken: session.sessionToken,
    senderType: 'RECRUITER',
    onCounterpartTyping: setApplicantTyping,
  });

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
        <div className="text-center">
          <p className="text-red-500 mb-4">메시지를 불러올 수 없습니다.</p>
          <Button onClick={() => window.location.reload()}>다시 시도</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-screen bg-gray-50 dark:bg-gray-900 transition-colors">
      {/* Header */}
      <div className="px-4 py-3 border-b dark:border-gray-700 bg-white dark:bg-gray-800 flex items-center gap-4 transition-colors">
        <div className="min-w-0 flex-1">
          <h2 className="font-semibold truncate dark:text-white">{session.resumeTitle}</h2>
          <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
            {session.recruiterName} &middot; {session.recruiterCompany}
          </p>
        </div>
        {/* 접속 상태 표시 */}
        <div className="flex items-center gap-1.5 text-xs shrink-0">
          <div
            className={`w-2 h-2 rounded-full ${
              connectionStatus === 'CONNECTED' && applicantOnline
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
              ? applicantOnline
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
                  className={`max-w-[70%] px-4 py-2.5 rounded-2xl transition-colors ${
                    isRecruiter
                      ? 'bg-blue-600 dark:bg-blue-500 text-white rounded-br-md'
                      : 'bg-white dark:bg-gray-800 border dark:border-gray-700 dark:text-white rounded-bl-md'
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
        {/* 타이핑 인디케이터 */}
        {applicantTyping && <TypingIndicator name="지원자" />}
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
        <Button type="submit" loading={sendMutation.isPending} className="rounded-full px-5">
          전송
        </Button>
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

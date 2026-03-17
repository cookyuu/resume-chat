import { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useSessionMessages, useSendApplicantMessage } from '@/features/chat';
import { Button, Skeleton, EmptyState, TypingIndicator, AttachmentDisplay, ExportModal } from '@/shared/ui';
import { formatDateTime } from '@/shared/lib/date';
import { getWebSocketClient, type ConnectionStatus } from '@/shared/api/websocket';
import { useChatWebSocket } from '@/shared/hooks/useChatWebSocket';
import { useTypingIndicator } from '@/shared/hooks/useTypingIndicator';
import { useUploadAttachment } from '@/shared/hooks/useUploadAttachment';
import { useSearchMessages } from '@/shared/hooks/useSearchMessages';
import { useExportChat } from '@/shared/hooks/useExportChat';
import { chatQueryKeys } from '@/shared/lib/queryKeys';
import { highlightKeyword } from '@/shared/lib/highlight';
import { PaperClipIcon, MagnifyingGlassIcon, XMarkIcon, ArrowDownTrayIcon } from '@heroicons/react/24/outline';

export function ChatPage() {
  const { sessionToken } = useParams<{ sessionToken: string }>();
  const { data, isLoading, isError } = useSessionMessages(sessionToken!);
  const resumeSlug = data?.session?.resumeSlug;
  const sendMutation = useSendApplicantMessage(sessionToken!, resumeSlug);

  const [message, setMessage] = useState('');
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('DISCONNECTED');
  const [recruiterOnline, setRecruiterOnline] = useState(false);
  const [recruiterTyping, setRecruiterTyping] = useState(false);
  const [showSearch, setShowSearch] = useState(false);
  const [showExportModal, setShowExportModal] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

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

  // 파일 업로드
  const { mutate: uploadFile, isPending: isUploading, uploadProgress } = useUploadAttachment(queryKey);

  // 세션 정보
  const session = data?.session;

  // 메시지 검색
  const { searchKeyword, setSearchKeyword, filteredMessages, searchResultCount, isSearching } = useSearchMessages(data?.messages);

  const displayMessages = isSearching ? filteredMessages : data?.messages || [];

  // 채팅 내보내기
  const { exportChat, isExporting } = useExportChat(data?.messages, {
    recruiterName: session?.recruiterName,
    recruiterCompany: session?.recruiterCompany,
  });

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // 파일 타입 체크
    const allowedTypes = [
      'application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document', // docx
      'application/msword', // doc
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // xlsx
      'application/vnd.ms-excel', // xls
      'application/vnd.openxmlformats-officedocument.presentationml.presentation', // pptx
      'image/jpeg',
      'image/png',
      'image/gif',
      'application/zip',
    ];

    if (!allowedTypes.includes(file.type)) {
      toast.error('지원하지 않는 파일 형식입니다.');
      return;
    }

    // 파일 크기 체크
    const isImage = file.type.startsWith('image/');
    const isZip = file.type === 'application/zip';
    const maxSize = isZip ? 20 * 1024 * 1024 : isImage ? 5 * 1024 * 1024 : 10 * 1024 * 1024;

    if (file.size > maxSize) {
      const maxSizeMB = maxSize / (1024 * 1024);
      toast.error(`파일 크기는 ${maxSizeMB}MB를 초과할 수 없습니다.`);
      return;
    }

    // 파일 업로드 성공 시 자동으로 메시지 전송
    uploadFile(
      { sessionToken: sessionToken!, file },
      {
        onSuccess: (attachment) => {
          console.log('파일 업로드 성공:', attachment);
          // 첨부파일ID를 포함한 메시지 자동 전송
          sendMutation.mutate({
            message: '', // 파일만 전송
            attachmentId: attachment.attachmentId,
          });
        },
      }
    );

    // input 초기화
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

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
        {/* 검색 & 내보내기 & 접속 상태 */}
        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={() => {
              setShowSearch(!showSearch);
              if (showSearch) {
                setSearchKeyword('');
              }
            }}
            className="p-1.5 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-colors"
            title="검색"
          >
            <MagnifyingGlassIcon className="w-5 h-5 text-gray-600 dark:text-gray-400" />
          </button>
          <button
            onClick={() => setShowExportModal(true)}
            className="p-1.5 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-colors"
            title="내보내기"
          >
            <ArrowDownTrayIcon className="w-5 h-5 text-gray-600 dark:text-gray-400" />
          </button>
          <div className="flex items-center gap-1.5 text-xs">
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
        </div>
      </div>

      {/* Search Bar */}
      {showSearch && (
        <div className="px-4 py-3 bg-white dark:bg-gray-800 border-b dark:border-gray-700 transition-colors">
          <div className="flex items-center gap-2">
            <input
              type="text"
              className="flex-1 px-3 py-2 border dark:border-gray-600 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              placeholder="메시지 검색..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              autoFocus
            />
            {isSearching && (
              <span className="text-sm text-gray-500 dark:text-gray-400 whitespace-nowrap">
                {searchResultCount}개 발견
              </span>
            )}
            <button
              onClick={() => {
                setShowSearch(false);
                setSearchKeyword('');
              }}
              className="p-1.5 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-colors"
            >
              <XMarkIcon className="w-5 h-5 text-gray-600 dark:text-gray-400" />
            </button>
          </div>
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-gray-50 dark:bg-gray-900 transition-colors">
        {!displayMessages || displayMessages.length === 0 ? (
          <EmptyState message={isSearching ? "검색 결과가 없습니다" : "아직 메시지가 없습니다"} />
        ) : (
          displayMessages.map((msg) => {
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
                  {msg.message && (
                    <p
                      className="text-sm whitespace-pre-wrap"
                      dangerouslySetInnerHTML={{
                        __html: isSearching ? highlightKeyword(msg.message, searchKeyword) : msg.message,
                      }}
                    />
                  )}
                  {msg.attachment && (
                    <div className={msg.message ? 'mt-2' : ''}>
                      <AttachmentDisplay
                        attachmentId={msg.attachment.attachmentId}
                        fileName={msg.attachment.fileName}
                        fileSize={msg.attachment.fileSize}
                        fileType={msg.attachment.fileType}
                        fileUrl={msg.attachment.fileUrl}
                      />
                    </div>
                  )}
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
      <form onSubmit={handleSend} className="p-3 bg-white dark:bg-gray-800 border-t dark:border-gray-700 transition-colors">
        {isUploading && (
          <div className="mb-2 px-3">
            <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
              <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                <div
                  className="bg-blue-500 h-2 rounded-full transition-all duration-300"
                  style={{ width: `${uploadProgress}%` }}
                />
              </div>
              <span className="text-xs">{uploadProgress}%</span>
            </div>
          </div>
        )}
        <div className="flex items-center gap-2">
          <input
            ref={fileInputRef}
            type="file"
            className="hidden"
            onChange={handleFileSelect}
            accept=".pdf,.doc,.docx,.xls,.xlsx,.pptx,.jpg,.jpeg,.png,.gif,.zip"
          />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploading}
            className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-colors disabled:opacity-50"
            title="파일 첨부"
          >
            <PaperClipIcon className="w-5 h-5 text-gray-600 dark:text-gray-400" />
          </button>
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
        </div>
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

      {/* Export Modal */}
      {showExportModal && (
        <ExportModal
          onExport={(options) => {
            exportChat(options);
            setShowExportModal(false);
          }}
          onClose={() => setShowExportModal(false)}
          isExporting={isExporting}
        />
      )}
    </div>
  );
}

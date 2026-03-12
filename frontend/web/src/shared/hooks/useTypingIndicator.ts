import { useEffect, useRef, useCallback } from 'react';
import { getWebSocketClient } from '@/shared/api/websocket';

export interface UseTypingIndicatorOptions {
  /** 채팅 세션 토큰 */
  sessionToken: string;
  /** 전송자 타입 (RECRUITER 또는 APPLICANT) */
  senderType: 'RECRUITER' | 'APPLICANT';
  /** 타이핑 상태 변경 핸들러 */
  onTypingChange?: (isTyping: boolean) => void;
  /** 타이핑 이벤트 수신 핸들러 (상대방의 타이핑 상태) */
  onCounterpartTyping?: (isTyping: boolean) => void;
}

/**
 * 타이핑 인디케이터 훅
 * - 입력 감지 및 타이핑 이벤트 WebSocket 발행
 * - 3초 타임아웃 자동 해제
 * - 상대방 타이핑 상태 구독
 */
export function useTypingIndicator({
  sessionToken,
  senderType,
  onTypingChange,
  onCounterpartTyping,
}: UseTypingIndicatorOptions) {
  const wsClient = useRef(getWebSocketClient());
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isTypingRef = useRef(false);

  /**
   * 타이핑 시작 이벤트 발행
   */
  const startTyping = useCallback(() => {
    if (isTypingRef.current) return; // 이미 타이핑 중이면 중복 발행 방지

    isTypingRef.current = true;
    onTypingChange?.(true);

    // WebSocket으로 타이핑 시작 이벤트 전송
    wsClient.current.publish(`/app/chat/${sessionToken}/typing`, {
      senderType,
      typing: true,
    });

    console.log('[useTypingIndicator] Typing started');
  }, [sessionToken, senderType, onTypingChange]);

  /**
   * 타이핑 종료 이벤트 발행
   */
  const stopTyping = useCallback(() => {
    if (!isTypingRef.current) return; // 이미 타이핑 안 하고 있으면 중복 발행 방지

    isTypingRef.current = false;
    onTypingChange?.(false);

    // WebSocket으로 타이핑 종료 이벤트 전송
    wsClient.current.publish(`/app/chat/${sessionToken}/typing`, {
      senderType,
      typing: false,
    });

    console.log('[useTypingIndicator] Typing stopped');
  }, [sessionToken, senderType, onTypingChange]);

  /**
   * 입력 이벤트 핸들러
   * - 입력 시 타이핑 시작 이벤트 발행
   * - 3초 후 자동으로 타이핑 종료
   */
  const handleInput = useCallback(() => {
    // 기존 타임아웃 제거
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    // 타이핑 시작
    startTyping();

    // 3초 후 자동 타이핑 종료
    typingTimeoutRef.current = setTimeout(() => {
      stopTyping();
    }, 3000);
  }, [startTyping, stopTyping]);

  /**
   * 상대방 타이핑 상태 구독
   */
  useEffect(() => {
    if (!sessionToken || !onCounterpartTyping) return;

    const wsClient = getWebSocketClient();

    // 연결될 때까지 대기
    const waitForConnection = setInterval(() => {
      if (wsClient.isConnected()) {
        clearInterval(waitForConnection);

        // 타이핑 상태 구독
        const unsubscribe = wsClient.subscribe(
          `/topic/chat/${sessionToken}/typing`,
          (message) => {
            try {
              const typingEvent: {
                senderType: 'RECRUITER' | 'APPLICANT';
                typing: boolean;
              } = JSON.parse(message.body);

              // 상대방의 타이핑 상태만 처리 (자신의 이벤트는 무시)
              if (typingEvent.senderType !== senderType) {
                onCounterpartTyping(typingEvent.typing);
                console.log(
                  '[useTypingIndicator] Counterpart typing:',
                  typingEvent.typing
                );
              }
            } catch (error) {
              console.error('[useTypingIndicator] Failed to parse typing event:', error);
            }
          }
        );

        return () => {
          unsubscribe();
        };
      }
    }, 100);

    return () => {
      clearInterval(waitForConnection);
    };
  }, [sessionToken, senderType, onCounterpartTyping]);

  /**
   * Cleanup: 컴포넌트 언마운트 시 타이핑 종료
   */
  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
      if (isTypingRef.current) {
        stopTyping();
      }
    };
  }, [stopTyping]);

  return {
    handleInput,
    startTyping,
    stopTyping,
  };
}

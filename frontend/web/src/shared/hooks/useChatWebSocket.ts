import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getWebSocketClient, type ConnectionStatus } from '@/shared/api/websocket';
import type { ChatMessage } from '@/entities/chat';

export interface UseChatWebSocketOptions {
  /** 채팅 세션 토큰 */
  sessionToken: string;
  /** React Query 캐시 키 (메시지 목록) */
  queryKey: readonly unknown[];
  /** 연결 상태 변경 핸들러 */
  onStatusChange?: (status: ConnectionStatus) => void;
  /** 상대방 접속 상태 변경 핸들러 */
  onPresenceChange?: (online: boolean) => void;
  /** 상대방 타입 (RECRUITER 또는 APPLICANT) */
  counterpartType: 'RECRUITER' | 'APPLICANT';
}

/**
 * 채팅 WebSocket 연결 및 구독 관리 커스텀 훅
 * - WebSocket 자동 연결
 * - 메시지 실시간 구독
 * - 접속 상태 구독
 * - React Query 캐시 자동 업데이트
 * - Cleanup 자동 처리
 */
export function useChatWebSocket({
  sessionToken,
  queryKey,
  onStatusChange,
  onPresenceChange,
  counterpartType,
}: UseChatWebSocketOptions) {
  const queryClient = useQueryClient();
  const wsClientRef = useRef(getWebSocketClient());
  const isSubscribedRef = useRef(false);
  const unsubscribersRef = useRef<Array<() => void>>([]);

  useEffect(() => {
    if (!sessionToken) return;

    const wsClient = wsClientRef.current;

    // 연결 상태 리스너
    const removeStatusListener = onStatusChange
      ? wsClient.onStatusChange(onStatusChange)
      : () => {};

    // WebSocket 연결 (sessionToken을 인증 토큰으로 사용)
    if (!wsClient.isConnected()) {
      wsClient.connect(sessionToken);
    }

    // 연결 대기 및 구독
    const waitForConnection = setInterval(() => {
      if (wsClient.isConnected() && !isSubscribedRef.current) {
        // ✅ 중복 구독 방지: 먼저 플래그 설정
        isSubscribedRef.current = true;
        clearInterval(waitForConnection);

        console.log('[useChatWebSocket] Starting subscriptions for session:', sessionToken);

        // 메시지 구독
        const unsubscribeMessages = wsClient.subscribe(
          `/topic/chat/${sessionToken}`,
          (message) => {
            console.log('[useChatWebSocket] 🔔 Raw message received:', message.body);
            try {
              const newMessage: ChatMessage = JSON.parse(message.body);
              console.log('[useChatWebSocket] ✅ Parsed message:', newMessage);

              // React Query 캐시에 새 메시지 추가
              queryClient.setQueryData(queryKey, (oldData: any) => {
                if (!oldData) {
                  console.warn('[useChatWebSocket] ⚠️ No existing data in cache');
                  return oldData;
                }

                console.log('[useChatWebSocket] 📝 Updating cache with new message');
                const updated = {
                  ...oldData,
                  data: {
                    ...oldData.data,
                    messages: [...(oldData.data.messages || []), newMessage],
                  },
                };
                console.log('[useChatWebSocket] ✅ Cache updated. Total messages:', updated.data.messages.length);
                return updated;
              });
            } catch (error) {
              console.error('[useChatWebSocket] ❌ Failed to parse message:', error);
            }
          }
        );

        // 접속 상태 구독
        const unsubscribePresence = wsClient.subscribe(
          `/topic/chat/${sessionToken}/presence`,
          (message) => {
            try {
              const presence: { senderType: 'RECRUITER' | 'APPLICANT'; online: boolean } =
                JSON.parse(message.body);

              // 상대방의 접속 상태만 추적
              if (presence.senderType === counterpartType) {
                onPresenceChange?.(presence.online);
              }
            } catch (error) {
              console.error('[useChatWebSocket] Failed to parse presence:', error);
            }
          }
        );

        // ✅ 구독 해제 함수를 ref에 저장
        unsubscribersRef.current = [unsubscribeMessages, unsubscribePresence];
      }
    }, 100);

    // ✅ cleanup: 모든 리소스 정리
    return () => {
      console.log('[useChatWebSocket] Cleanup for session:', sessionToken);
      clearInterval(waitForConnection);
      removeStatusListener();

      // ✅ 모든 구독 해제
      unsubscribersRef.current.forEach((unsub) => unsub());
      unsubscribersRef.current = [];
      isSubscribedRef.current = false;
    };
  }, [sessionToken, queryKey, onStatusChange, onPresenceChange, counterpartType, queryClient]);
}

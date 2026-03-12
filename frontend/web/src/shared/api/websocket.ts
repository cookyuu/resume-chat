import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/shared/store/auth';

export type ConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING';

type MessageHandler = (message: IMessage) => void;
type StatusChangeHandler = (status: ConnectionStatus) => void;

/**
 * WebSocket 클라이언트 유틸리티
 * - SockJS + STOMP 프로토콜 사용
 * - 자동 재연결 (Exponential Backoff: 1s → 2s → 4s → 8s → 16s → 30s max)
 * - 최대 5회 재연결 시도
 * - 인증 토큰 자동 포함
 */
export class WebSocketClient {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private status: ConnectionStatus = 'DISCONNECTED';
  private statusHandlers: Set<StatusChangeHandler> = new Set();
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private isManualDisconnect = false;
  private lastToken: string | undefined = undefined;
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;

  constructor(private endpoint: string = '/api/ws') {}

  /**
   * WebSocket 연결
   * @param customToken 커스텀 토큰 (예: sessionToken). 없으면 accessToken 사용
   */
  connect(customToken?: string): void {
    // ✅ 이미 연결되어 있거나 연결 중이면 중단
    if (this.client?.connected || this.status === 'CONNECTING') {
      console.warn('[WebSocket] Already connected or connecting. Status:', this.status);
      return;
    }

    this.isManualDisconnect = false;
    this.setStatus('CONNECTING');

    const token = customToken || useAuthStore.getState().accessToken;
    this.lastToken = customToken; // 재연결 시 사용하기 위해 저장

    this.client = new Client({
      webSocketFactory: () => new SockJS(this.endpoint) as WebSocket,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log('[STOMP Debug]', str);
        }
      },
      reconnectDelay: 0, // 자체 재연결 로직 사용
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    this.client.onConnect = (frame) => {
      console.log('[WebSocket] ✅ Connected successfully');
      console.log('[WebSocket] Connection details:', {
        command: frame.command,
        headers: frame.headers,
      });
      this.setStatus('CONNECTED');
      this.clearReconnectTimer();
      this.reconnectAttempts = 0; // 연결 성공 시 재시도 카운터 리셋
    };

    this.client.onDisconnect = () => {
      console.log('[WebSocket] Disconnected');
      this.setStatus('DISCONNECTED');
      this.subscriptions.clear();

      // 수동 종료가 아니면 재연결 시도
      if (!this.isManualDisconnect) {
        this.scheduleReconnect();
      }
    };

    this.client.onStompError = (frame) => {
      console.error('[WebSocket] STOMP Error:', frame.headers['message']);
      console.error('[WebSocket] Details:', frame.body);
    };

    this.client.onWebSocketError = (event) => {
      console.error('[WebSocket] Error:', event);
      this.setStatus('DISCONNECTED');
      if (!this.isManualDisconnect) {
        this.scheduleReconnect();
      }
    };

    this.client.activate();
  }

  /**
   * WebSocket 연결 종료
   */
  disconnect(): void {
    this.isManualDisconnect = true;
    this.clearReconnectTimer();

    if (this.client?.connected) {
      this.client.deactivate();
    }

    this.subscriptions.clear();
    this.setStatus('DISCONNECTED');
  }

  /**
   * 토픽 구독
   * @param destination 구독할 목적지 (예: /topic/chat/1234)
   * @param handler 메시지 수신 핸들러
   * @returns 구독 해제 함수
   */
  subscribe(destination: string, handler: MessageHandler): () => void {
    if (!this.client?.connected) {
      console.warn('[WebSocket] ⚠️ Not connected. Cannot subscribe to', destination);
      return () => {};
    }

    // 이미 구독 중이면 기존 구독 해제
    if (this.subscriptions.has(destination)) {
      console.warn('[WebSocket] ⚠️ Already subscribed to', destination, '- unsubscribing old one');
      this.subscriptions.get(destination)?.unsubscribe();
    }

    const subscription = this.client.subscribe(destination, (message) => {
      console.log('[WebSocket] 📨 Message received on', destination);
      handler(message);
    });
    this.subscriptions.set(destination, subscription);

    console.log('[WebSocket] ✅ Subscribed to', destination);
    console.log('[WebSocket] Total active subscriptions:', this.subscriptions.size);

    // 구독 해제 함수 반환
    return () => {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
      console.log('[WebSocket] ❌ Unsubscribed from', destination);
    };
  }

  /**
   * 메시지 발행
   * @param destination 발행할 목적지 (예: /app/chat/send)
   * @param body 메시지 본문 (객체는 자동으로 JSON 변환)
   */
  publish(destination: string, body: unknown): void {
    if (!this.client?.connected) {
      console.warn('[WebSocket] Not connected. Cannot publish to', destination);
      return;
    }

    this.client.publish({
      destination,
      body: typeof body === 'string' ? body : JSON.stringify(body),
    });

    console.log('[WebSocket] Published to', destination, body);
  }

  /**
   * 연결 상태 변경 리스너 등록
   */
  onStatusChange(handler: StatusChangeHandler): () => void {
    this.statusHandlers.add(handler);

    // 초기 상태 즉시 전달
    handler(this.status);

    // 리스너 제거 함수 반환
    return () => {
      this.statusHandlers.delete(handler);
    };
  }

  /**
   * 현재 연결 상태 반환
   */
  getStatus(): ConnectionStatus {
    return this.status;
  }

  /**
   * 연결 여부 확인
   */
  isConnected(): boolean {
    return this.status === 'CONNECTED';
  }

  /**
   * 재연결 스케줄링 (Exponential Backoff)
   * - 1초 → 2초 → 4초 → 8초 → 16초 → 30초 (최대)
   * - 최대 5회 시도
   */
  private scheduleReconnect(): void {
    // 최대 재연결 시도 횟수 초과
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[WebSocket] Maximum reconnection attempts reached. Please reconnect manually.');
      this.setStatus('DISCONNECTED');
      return;
    }

    this.clearReconnectTimer();
    this.setStatus('RECONNECTING');

    // Exponential Backoff 계산 (1000ms * 2^attempts, 최대 30초)
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    this.reconnectAttempts++;

    console.log(
      `[WebSocket] Reconnecting in ${delay}ms (Attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})...`
    );

    this.reconnectTimer = setTimeout(() => {
      this.connect(this.lastToken);
    }, delay);
  }

  /**
   * 재연결 타이머 제거
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  /**
   * 수동 재연결 (재시도 카운터 리셋)
   */
  manualReconnect(): void {
    this.reconnectAttempts = 0;
    this.isManualDisconnect = false;
    this.clearReconnectTimer();

    if (this.client?.connected) {
      this.disconnect();
    }

    this.connect(this.lastToken);
  }

  /**
   * 상태 변경 및 리스너 호출
   */
  private setStatus(status: ConnectionStatus): void {
    this.status = status;
    this.statusHandlers.forEach((handler) => handler(status));
  }
}

// ── 싱글톤 인스턴스 ──
let websocketClient: WebSocketClient | null = null;

/**
 * WebSocket 클라이언트 싱글톤 인스턴스 반환
 */
export function getWebSocketClient(): WebSocketClient {
  if (!websocketClient) {
    websocketClient = new WebSocketClient();
  }
  return websocketClient;
}

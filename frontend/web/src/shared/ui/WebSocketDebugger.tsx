import { useState, useEffect } from 'react';
import { getWebSocketClient } from '@/shared/api/websocket';

/**
 * WebSocket 디버깅 컴포넌트
 * - 개발 환경에서만 사용
 * - 우측 하단에 WebSocket 상태 표시
 */
export function WebSocketDebugger() {
  const [logs, setLogs] = useState<string[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [subscriptions, setSubscriptions] = useState<string[]>([]);

  useEffect(() => {
    if (import.meta.env.PROD) return; // 프로덕션에서는 비활성화

    const wsClient = getWebSocketClient();

    // 기존 console.log를 가로채서 WebSocket 관련 로그만 캡처
    const originalLog = console.log;
    const originalWarn = console.warn;
    const originalError = console.error;

    const captureLog = (level: 'log' | 'warn' | 'error', ...args: any[]) => {
      const message = args.join(' ');

      // WebSocket 관련 로그만 캡처
      if (
        message.includes('[WebSocket]') ||
        message.includes('[useChatWebSocket]') ||
        message.includes('[STOMP')
      ) {
        const timestamp = new Date().toLocaleTimeString();
        const logEntry = `[${timestamp}] [${level.toUpperCase()}] ${message}`;

        setLogs((prev) => [...prev.slice(-50), logEntry]); // 최대 50개만 유지

        // 구독 정보 업데이트
        if (message.includes('Subscribed to')) {
          const match = message.match(/Subscribed to (.+)/);
          if (match) {
            setSubscriptions((prev) => [...new Set([...prev, match[1]])]);
          }
        }
        if (message.includes('Unsubscribed from')) {
          const match = message.match(/Unsubscribed from (.+)/);
          if (match) {
            setSubscriptions((prev) => prev.filter((s) => s !== match[1]));
          }
        }
      }
    };

    console.log = (...args: any[]) => {
      captureLog('log', ...args);
      originalLog(...args);
    };

    console.warn = (...args: any[]) => {
      captureLog('warn', ...args);
      originalWarn(...args);
    };

    console.error = (...args: any[]) => {
      captureLog('error', ...args);
      originalError(...args);
    };

    return () => {
      console.log = originalLog;
      console.warn = originalWarn;
      console.error = originalError;
    };
  }, []);

  if (import.meta.env.PROD) return null;

  const wsClient = getWebSocketClient();
  const status = wsClient.getStatus();

  return (
    <>
      {/* 토글 버튼 */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="fixed bottom-4 right-4 z-[9999] bg-blue-600 text-white px-4 py-2 rounded-full shadow-lg hover:bg-blue-700 text-sm font-medium"
      >
        🐛 WS Debug {isOpen ? '✕' : `(${logs.length})`}
      </button>

      {/* 디버깅 패널 */}
      {isOpen && (
        <div className="fixed bottom-20 right-4 z-[9999] bg-white border-2 border-blue-500 rounded-lg shadow-2xl w-[500px] max-h-[600px] flex flex-col">
          {/* 헤더 */}
          <div className="bg-blue-600 text-white px-4 py-2 rounded-t-lg flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="font-bold">WebSocket Debugger</span>
              <span
                className={`px-2 py-0.5 rounded text-xs ${
                  status === 'CONNECTED'
                    ? 'bg-green-500'
                    : status === 'CONNECTING' || status === 'RECONNECTING'
                    ? 'bg-yellow-500'
                    : 'bg-red-500'
                }`}
              >
                {status}
              </span>
            </div>
            <button
              onClick={() => setLogs([])}
              className="text-xs bg-white/20 hover:bg-white/30 px-2 py-1 rounded"
            >
              Clear
            </button>
          </div>

          {/* 구독 정보 */}
          <div className="px-4 py-2 bg-gray-50 border-b">
            <div className="text-xs font-semibold text-gray-700 mb-1">
              Active Subscriptions ({subscriptions.length})
            </div>
            {subscriptions.length === 0 ? (
              <div className="text-xs text-gray-500 italic">No active subscriptions</div>
            ) : (
              <div className="space-y-1">
                {subscriptions.map((sub, idx) => (
                  <div key={idx} className="text-xs font-mono bg-white px-2 py-1 rounded border">
                    {sub}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 로그 */}
          <div className="flex-1 overflow-y-auto p-4 space-y-1 font-mono text-xs">
            {logs.length === 0 ? (
              <div className="text-gray-400 italic">No logs yet...</div>
            ) : (
              logs.map((log, idx) => {
                const isError = log.includes('[ERROR]');
                const isWarn = log.includes('[WARN]');
                const isSuccess =
                  log.includes('✅') || log.includes('Connected') || log.includes('Subscribed');
                const isReceived = log.includes('📨') || log.includes('🔔');

                return (
                  <div
                    key={idx}
                    className={`p-1 rounded ${
                      isError
                        ? 'bg-red-50 text-red-800'
                        : isWarn
                        ? 'bg-yellow-50 text-yellow-800'
                        : isSuccess
                        ? 'bg-green-50 text-green-800'
                        : isReceived
                        ? 'bg-blue-50 text-blue-800 font-bold'
                        : 'text-gray-700'
                    }`}
                  >
                    {log}
                  </div>
                );
              })
            )}
          </div>

          {/* 테스트 버튼 */}
          <div className="px-4 py-2 bg-gray-50 border-t flex gap-2">
            <button
              onClick={() => {
                console.log('[WebSocket] 🧪 Manual test message');
                wsClient.manualReconnect();
              }}
              className="flex-1 bg-blue-500 text-white px-3 py-1 rounded text-xs hover:bg-blue-600"
            >
              Reconnect
            </button>
            <button
              onClick={() => {
                const currentUrl = window.location.href;
                const sessionToken = currentUrl.match(/session\/([^/]+)/)?.[1] ||
                                     currentUrl.match(/chat\/([^/]+)/)?.[1];
                console.log('[WebSocket] 🔍 Current URL:', currentUrl);
                console.log('[WebSocket] 🔍 Extracted sessionToken:', sessionToken);
                console.log('[WebSocket] 🔍 WebSocket status:', wsClient.getStatus());
              }}
              className="flex-1 bg-gray-500 text-white px-3 py-1 rounded text-xs hover:bg-gray-600"
            >
              Log Info
            </button>
          </div>
        </div>
      )}
    </>
  );
}

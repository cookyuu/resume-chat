interface TypingIndicatorProps {
  /** 표시할 이름 (예: "채용담당자", "지원자") */
  name?: string;
}

/**
 * 타이핑 인디케이터 컴포넌트
 * - "상대방이 입력 중..." 메시지
 * - 애니메이션 효과 (점 3개)
 */
export function TypingIndicator({ name = '상대방' }: TypingIndicatorProps) {
  return (
    <div className="flex items-center gap-2 px-4 py-2">
      <div className="flex items-center gap-1 px-4 py-2.5 rounded-2xl bg-gray-100 dark:bg-gray-800">
        <span className="text-sm text-gray-600 dark:text-gray-400">{name}이 입력 중</span>
        <div className="flex gap-1 ml-1">
          <span className="w-1.5 h-1.5 bg-gray-400 dark:bg-gray-500 rounded-full animate-bounce [animation-delay:-0.3s]" />
          <span className="w-1.5 h-1.5 bg-gray-400 dark:bg-gray-500 rounded-full animate-bounce [animation-delay:-0.15s]" />
          <span className="w-1.5 h-1.5 bg-gray-400 dark:bg-gray-500 rounded-full animate-bounce" />
        </div>
      </div>
    </div>
  );
}

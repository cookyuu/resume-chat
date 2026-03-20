import { InboxIcon } from '@heroicons/react/24/outline';

interface EmptyStateProps {
  message?: string;
}

export function EmptyState({ message = '데이터가 없습니다' }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-gray-400 dark:text-gray-500">
      <InboxIcon className="w-16 h-16 mb-4" />
      <p className="text-sm">{message}</p>
    </div>
  );
}

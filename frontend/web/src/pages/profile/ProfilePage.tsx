import { useProfile } from '@/features/auth';
import { Skeleton } from '@/shared/ui';
import { formatDateTime } from '@/shared/lib/date';

export function ProfilePage() {
  const { data: profile, isLoading, isError } = useProfile();

  if (isLoading) {
    return (
      <div className="p-6 max-w-2xl mx-auto space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="p-6 max-w-2xl mx-auto">
        <p className="text-red-500 dark:text-red-400">프로필 정보를 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6 dark:text-white">내 프로필</h1>
      <div className="bg-white dark:bg-gray-800 border dark:border-gray-700 rounded-lg overflow-hidden transition-colors">
        <dl className="divide-y dark:divide-gray-700">
          <div className="px-6 py-4 grid grid-cols-3 gap-4">
            <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">이름</dt>
            <dd className="col-span-2 text-sm text-gray-900 dark:text-white">{profile.name}</dd>
          </div>
          <div className="px-6 py-4 grid grid-cols-3 gap-4">
            <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">이메일</dt>
            <dd className="col-span-2 text-sm text-gray-900 dark:text-white">{profile.email}</dd>
          </div>
          <div className="px-6 py-4 grid grid-cols-3 gap-4">
            <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">가입일</dt>
            <dd className="col-span-2 text-sm text-gray-600 dark:text-gray-300">{formatDateTime(profile.createdAt)}</dd>
          </div>
        </dl>
      </div>
    </div>
  );
}

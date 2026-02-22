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
        <p className="text-red-500">프로필 정보를 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">내 프로필</h1>
      <div className="bg-white border rounded-lg overflow-hidden">
        <dl className="divide-y">
          <div className="px-6 py-4 grid grid-cols-3 gap-4">
            <dt className="text-sm font-medium text-gray-500">이름</dt>
            <dd className="col-span-2 text-sm text-gray-900">{profile.name}</dd>
          </div>
          <div className="px-6 py-4 grid grid-cols-3 gap-4">
            <dt className="text-sm font-medium text-gray-500">이메일</dt>
            <dd className="col-span-2 text-sm text-gray-900">{profile.email}</dd>
          </div>
          <div className="px-6 py-4 grid grid-cols-3 gap-4">
            <dt className="text-sm font-medium text-gray-500">가입일</dt>
            <dd className="col-span-2 text-sm text-gray-600">{formatDateTime(profile.createdAt)}</dd>
          </div>
        </dl>
      </div>
    </div>
  );
}

import { useState } from 'react';
import { useBlockList, useAddBlock, useRemoveBlock } from '@/features/admin';
import { Button, Input, Skeleton, EmptyState } from '@/shared/ui';
import { formatDateTime } from '@/shared/lib/date';
import { ShieldExclamationIcon, TrashIcon } from '@heroicons/react/24/outline';

export function AdminBlockListPage() {
  const { data, isLoading, isError } = useBlockList();
  const addBlockMutation = useAddBlock();
  const removeBlockMutation = useRemoveBlock();

  const [showAddForm, setShowAddForm] = useState(false);
  const [email, setEmail] = useState('');
  const [reason, setReason] = useState('');

  const handleAddBlock = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !reason.trim()) return;

    addBlockMutation.mutate(
      { email: email.trim(), reason: reason.trim() },
      {
        onSuccess: () => {
          setEmail('');
          setReason('');
          setShowAddForm(false);
        },
      }
    );
  };

  const handleRemoveBlock = (id: string) => {
    if (window.confirm('정말 차단을 해제하시겠습니까?')) {
      removeBlockMutation.mutate(id);
    }
  };

  if (isLoading) {
    return (
      <div className="p-6 max-w-5xl mx-auto">
        <Skeleton className="h-8 w-48 mb-6" />
        <Skeleton className="h-10 w-full mb-2" />
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-14 w-full mb-1" />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="p-6 max-w-5xl mx-auto">
        <p className="text-red-500 dark:text-red-400">차단 목록을 불러올 수 없습니다.</p>
      </div>
    );
  }

  const blockedUsers = data?.blockedUsers || [];

  return (
    <div className="p-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <ShieldExclamationIcon className="w-8 h-8 text-red-500 dark:text-red-400" />
          <h1 className="text-2xl font-bold dark:text-white">악성 사용자 차단 관리</h1>
        </div>
        <Button onClick={() => setShowAddForm(!showAddForm)}>
          {showAddForm ? '취소' : '사용자 차단'}
        </Button>
      </div>

      {/* Add Block Form */}
      {showAddForm && (
        <form onSubmit={handleAddBlock} className="mb-6 p-5 bg-white dark:bg-gray-800 border dark:border-gray-700 rounded-lg space-y-4 transition-colors">
          <h2 className="font-semibold text-lg dark:text-white">사용자 차단 추가</h2>
          <Input
            label="이메일"
            type="email"
            placeholder="차단할 사용자 이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Input
            label="차단 사유"
            placeholder="차단 사유를 입력하세요 (최대 200자)"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            maxLength={200}
            required
          />
          <div className="flex gap-2">
            <Button type="submit" loading={addBlockMutation.isPending}>
              차단
            </Button>
            <Button variant="secondary" type="button" onClick={() => setShowAddForm(false)}>
              취소
            </Button>
          </div>
        </form>
      )}

      {/* Block List */}
      {blockedUsers.length === 0 ? (
        <EmptyState message="차단된 사용자가 없습니다" />
      ) : (
        <div className="bg-white dark:bg-gray-800 border dark:border-gray-700 rounded-lg overflow-hidden transition-colors">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 dark:bg-gray-900 border-b dark:border-gray-700 text-left text-gray-500 dark:text-gray-400">
                <th className="px-4 py-3 font-medium">이메일</th>
                <th className="px-4 py-3 font-medium">차단 사유</th>
                <th className="px-4 py-3 font-medium">차단 일시</th>
                <th className="px-4 py-3 font-medium">차단 관리자</th>
                <th className="px-4 py-3 font-medium text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y dark:divide-gray-700">
              {blockedUsers.map((user) => (
                <tr key={user.id} className="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
                  <td className="px-4 py-3">
                    <p className="font-medium text-gray-900 dark:text-white">{user.email}</p>
                  </td>
                  <td className="px-4 py-3 text-gray-600 dark:text-gray-300">
                    <p className="max-w-md truncate">{user.reason}</p>
                  </td>
                  <td className="px-4 py-3 text-gray-500 dark:text-gray-400">
                    {formatDateTime(user.blockedAt)}
                  </td>
                  <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{user.blockedBy}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end">
                      <button
                        onClick={() => handleRemoveBlock(user.id)}
                        disabled={removeBlockMutation.isPending}
                        className="px-2.5 py-1.5 text-xs font-medium text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900 rounded hover:bg-red-100 dark:hover:bg-red-800 disabled:opacity-50 flex items-center gap-1"
                      >
                        <TrashIcon className="w-4 h-4" />
                        차단 해제
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* 안내 메시지 */}
      <div className="mt-6 p-4 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg">
        <p className="text-sm text-yellow-800 dark:text-yellow-200">
          <strong>주의:</strong> 차단된 사용자는 로그인 및 서비스 이용이 제한됩니다. 신중하게 처리해주세요.
        </p>
      </div>
    </div>
  );
}

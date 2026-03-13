import { useState, useEffect } from 'react';
import { useNotificationSettings, useUpdateNotificationSettings } from '@/features/settings';
import { Button, Skeleton } from '@/shared/ui';

export function NotificationSettingsPage() {
  const { data, isLoading, isError } = useNotificationSettings();
  const updateMutation = useUpdateNotificationSettings();

  const [emailNotification, setEmailNotification] = useState(false);
  const [pushNotification, setPushNotification] = useState(false);

  useEffect(() => {
    if (data?.data) {
      setEmailNotification(data.data.emailNotification);
      setPushNotification(data.data.pushNotification);
    }
  }, [data]);

  const handleSave = () => {
    updateMutation.mutate({
      emailNotification,
      pushNotification,
    });
  };

  if (isLoading) {
    return (
      <div className="p-6 max-w-2xl mx-auto space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="p-6 max-w-2xl mx-auto">
        <p className="text-red-500 dark:text-red-400">알림 설정을 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6 dark:text-white">알림 설정</h1>

      <div className="bg-white dark:bg-gray-800 border dark:border-gray-700 rounded-lg overflow-hidden transition-colors">
        <div className="divide-y dark:divide-gray-700">
          {/* 이메일 알림 */}
          <div className="px-6 py-4 flex items-center justify-between">
            <div>
              <h3 className="text-sm font-medium text-gray-900 dark:text-white">이메일 알림</h3>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                새로운 채팅 메시지를 이메일로 받습니다
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                className="sr-only peer"
                checked={emailNotification}
                onChange={(e) => setEmailNotification(e.target.checked)}
              />
              <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600"></div>
            </label>
          </div>

          {/* 푸시 알림 */}
          <div className="px-6 py-4 flex items-center justify-between">
            <div>
              <h3 className="text-sm font-medium text-gray-900 dark:text-white">푸시 알림</h3>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                브라우저 푸시 알림을 받습니다 (준비 중)
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                className="sr-only peer"
                checked={pushNotification}
                onChange={(e) => setPushNotification(e.target.checked)}
                disabled
              />
              <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600 opacity-50 cursor-not-allowed"></div>
            </label>
          </div>
        </div>

        {/* 저장 버튼 */}
        <div className="px-6 py-4 bg-gray-50 dark:bg-gray-900 border-t dark:border-gray-700">
          <Button
            onClick={handleSave}
            loading={updateMutation.isPending}
          >
            저장
          </Button>
        </div>
      </div>
    </div>
  );
}

import { apiClient } from '@/shared/api';
import type { ApiResponse } from '@/shared/types/api';
import type { NotificationSettings, UpdateNotificationSettingsRequest } from '@/entities/settings';

export const settingsApi = {
  getNotificationSettings: () =>
    apiClient.get<ApiResponse<NotificationSettings>>('/applicant/settings/notifications')
      .then((res) => res.data),

  updateNotificationSettings: (data: UpdateNotificationSettingsRequest) =>
    apiClient.put<ApiResponse<NotificationSettings>>('/applicant/settings/notifications', data)
      .then((res) => res.data),
};

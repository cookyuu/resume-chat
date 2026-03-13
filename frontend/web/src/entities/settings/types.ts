export interface NotificationSettings {
  emailNotification: boolean;
  pushNotification: boolean;
}

export interface UpdateNotificationSettingsRequest {
  emailNotification: boolean;
  pushNotification: boolean;
}

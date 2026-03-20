import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppLayout } from './layouts/AppLayout';
import { AuthGuard } from './layouts/AuthGuard';
import { LoginPage } from '@/pages/login/LoginPage';
import { JoinPage } from '@/pages/join/JoinPage';
import { ProfilePage } from '@/pages/profile/ProfilePage';
import { ResumesPage } from '@/pages/resume/ResumesPage';
import { ResumeChatsPage } from '@/pages/chat/ResumeChatsPage';
import { ChatPage } from '@/pages/chat/ChatPage';
import { RecruiterChatPage } from '@/pages/chat/RecruiterChatPage';
import { NotificationSettingsPage } from '@/pages/settings/NotificationSettingsPage';
import { AdminBlockListPage } from '@/pages/admin/AdminBlockListPage';

export const router = createBrowserRouter([
  // ── Public ──
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/join',
    element: <JoinPage />,
  },
  {
    path: '/chat/:resumeSlug',
    element: <RecruiterChatPage />,
  },
  // ── Protected ──
  {
    element: <AuthGuard />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            path: '/profile',
            element: <ProfilePage />,
          },
          {
            path: '/resumes',
            element: <ResumesPage />,
          },
          {
            path: '/resumes/:resumeSlug/chats',
            element: <ResumeChatsPage />,
          },
          {
            path: '/chat/:sessionToken/messages',
            element: <ChatPage />,
          },
          {
            path: '/settings/notifications',
            element: <NotificationSettingsPage />,
          },
          {
            path: '/admin/blocks',
            element: <AdminBlockListPage />,
          },
        ],
      },
    ],
  },
  // ── Fallback ──
  {
    path: '*',
    element: <Navigate to="/login" replace />,
  },
]);

import { useParams, Link } from 'react-router-dom';
import { useResumeChats } from '@/features/chat';
import { Skeleton, EmptyState } from '@/shared/ui';
import { formatDateTime } from '@/shared/lib/date';

export function ResumeChatsPage() {
  const { resumeSlug } = useParams<{ resumeSlug: string }>();
  const { data, isLoading, isError } = useResumeChats(resumeSlug!);

  if (isLoading) {
    return (
      <div className="p-6 space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-20 w-full" />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="p-6 max-w-4xl mx-auto">
        <Link to="/resumes" className="text-sm text-blue-600 dark:text-blue-400 hover:underline">&larr; 이력서 목록</Link>
        <p className="mt-4 text-red-500 dark:text-red-400">채팅 세션 목록을 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="mb-6">
        <Link to="/resumes" className="text-sm text-blue-600 dark:text-blue-400 hover:underline">&larr; 이력서 목록</Link>
        <h1 className="text-2xl font-bold mt-2 dark:text-white">{data?.resumeTitle ?? '채팅 세션'}</h1>
      </div>

      {!data?.sessions || data.sessions.length === 0 ? (
        <EmptyState message="아직 채팅 세션이 없습니다" />
      ) : (
        <div className="bg-white dark:bg-gray-800 border dark:border-gray-700 rounded-lg overflow-hidden transition-colors">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 dark:bg-gray-900 border-b dark:border-gray-700 text-left text-gray-500 dark:text-gray-400">
                <th className="px-4 py-3 font-medium">채용담당자</th>
                <th className="px-4 py-3 font-medium">회사</th>
                <th className="px-4 py-3 font-medium">이메일</th>
                <th className="px-4 py-3 font-medium text-center">메시지</th>
                <th className="px-4 py-3 font-medium">최근 메시지</th>
                <th className="px-4 py-3 font-medium">시작일</th>
              </tr>
            </thead>
            <tbody className="divide-y dark:divide-gray-700">
              {data.sessions.map((session) => (
                <tr key={session.sessionToken} className="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
                  <td className="px-4 py-3">
                    <Link
                      to={`/chat/${session.sessionToken}/messages`}
                      className="font-medium text-blue-600 dark:text-blue-400 hover:underline"
                    >
                      {session.recruiterName}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{session.recruiterCompany}</td>
                  <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{session.recruiterEmail}</td>
                  <td className="px-4 py-3 text-center">
                    <span className="text-gray-600 dark:text-gray-300">{session.totalMessages}</span>
                    {session.unreadMessages > 0 && (
                      <span className="ml-1.5 inline-block px-2 py-0.5 bg-blue-600 dark:bg-blue-500 text-white text-xs rounded-full">
                        +{session.unreadMessages}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-gray-500 dark:text-gray-400">{formatDateTime(session.lastMessageAt)}</td>
                  <td className="px-4 py-3 text-gray-500 dark:text-gray-400">{formatDateTime(session.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

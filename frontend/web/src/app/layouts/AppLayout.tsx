import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/shared/store/auth';

export function AppLayout() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="h-16 bg-white border-b flex items-center justify-between px-6">
        <Link to="/resumes" className="text-lg font-bold text-blue-600">Resume Chat</Link>
        {user && (
          <div className="flex items-center gap-4">
            <Link to="/profile" className="text-sm text-gray-600 hover:text-blue-600">{user.name}</Link>
            <button
              onClick={handleLogout}
              className="text-sm text-gray-500 hover:text-gray-700"
            >
              로그아웃
            </button>
          </div>
        )}
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
}

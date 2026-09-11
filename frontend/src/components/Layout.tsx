import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { DiceIcon } from './ui';

export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">
          <DiceIcon />
          Campaign Manager
        </Link>
        {user && (
          <nav className="topnav">
            <span className="user">
              <span className="avatar">{user.username.charAt(0).toUpperCase()}</span>
              {user.username}
            </span>
            <button className="button ghost small" onClick={handleLogout} style={{ color: '#f3e9d6', borderColor: '#6b5a4a' }}>
              Log out
            </button>
          </nav>
        )}
      </header>
      <main className="view">
        <Outlet />
      </main>
    </>
  );
}

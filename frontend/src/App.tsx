import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { NewCampaignPage } from './pages/NewCampaignPage';
import { CampaignPage } from './pages/CampaignPage';
import { ConstraintsPage } from './pages/ConstraintsPage';
import { NewCharacterPage } from './pages/NewCharacterPage';
import { CharacterPage } from './pages/CharacterPage';

function RequireUser() {
  const { user, loading } = useAuth();
  if (loading) return null;
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}

function RequireGuest() {
  const { user, loading } = useAuth();
  if (loading) return null;
  return user ? <Navigate to="/" replace /> : <Outlet />;
}

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route element={<RequireGuest />}>
          <Route path="/login" element={<LoginPage mode="login" />} />
          <Route path="/register" element={<LoginPage mode="register" />} />
        </Route>
        <Route element={<RequireUser />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/campaigns/new" element={<NewCampaignPage />} />
          <Route path="/campaigns/:id" element={<CampaignPage />} />
          <Route path="/campaigns/:id/constraints" element={<ConstraintsPage />} />
          <Route path="/characters/new" element={<NewCharacterPage />} />
          <Route path="/characters/:id" element={<CharacterPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}

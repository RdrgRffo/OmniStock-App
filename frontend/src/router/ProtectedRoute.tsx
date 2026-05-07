import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from 'features/auth/context/AuthContext';
import AppLayout from 'components/layout/AppLayout';
import { NotificationProvider } from 'features/notifications/context/NotificationContext';

const ProtectedRoute = () => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <NotificationProvider>
      <AppLayout>
        <Outlet />
      </AppLayout>
    </NotificationProvider>
  );
};

export default ProtectedRoute;


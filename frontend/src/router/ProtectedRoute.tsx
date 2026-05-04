import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from 'features/auth/context/AuthContext';
import AppLayout from 'components/layout/AppLayout';

const ProtectedRoute = () => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <AppLayout>
      <Outlet />
    </AppLayout>
  );
};

export default ProtectedRoute;


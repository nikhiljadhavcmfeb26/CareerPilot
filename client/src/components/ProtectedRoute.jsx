import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from './LoadingSpinner';

const ProtectedRoute = ({ children, roles }) => {
  const { user, loading, isAuthenticated } = useAuth();

  if (loading) return <LoadingSpinner fullPage />;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) {
    const dashboardPath = user.role === 'Admin' 
      ? '/admin/dashboard' 
      : user.role === 'Employer' 
      ? '/employer/dashboard' 
      : '/jobseeker/dashboard';
    return <Navigate to={dashboardPath} replace />;
  }

  return children;
};

export default ProtectedRoute;

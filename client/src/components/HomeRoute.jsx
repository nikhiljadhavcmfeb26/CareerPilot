import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Home from '../pages/public/Home';
import LoadingSpinner from './LoadingSpinner';

const HomeRoute = () => {
  const { user, loading, isAuthenticated } = useAuth();

  if (loading) return <LoadingSpinner fullPage />;

  if (isAuthenticated) {
    const dashboardPath = user?.role === 'Admin' 
      ? '/admin/dashboard' 
      : user?.role === 'Employer' 
      ? '/employer/dashboard' 
      : '/jobseeker/dashboard';
    return <Navigate to={dashboardPath} replace />;
  }

  return <Home />;
};

export default HomeRoute;

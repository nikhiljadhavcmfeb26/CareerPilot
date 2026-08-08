import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi, userApi, subscriptionApi } from '../api/services';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Subscription state lives here rather than in its own provider so that the
  // premium badge, the premium gate and the Premium page all read one source
  // of truth, refreshed in one place after a successful payment.
  const [subscription, setSubscription] = useState(null);

  const refreshSubscription = useCallback(async (role) => {
    // Admin has no Premium concept at all (Navbar hides the upgrade link, and
    // ai-service never gates an admin). Asking anyway produced a pointless
    // request on every admin page load and a misleading "FREE" plan in state.
    if (role === 'Admin') {
      setSubscription(null);
      return null;
    }
    try {
      const { data } = await subscriptionApi.getMy();
      if (data.success) {
        setSubscription(data.data);
        return data.data;
      }
    } catch {
      // A subscription lookup failure must never block the app - the user is
      // simply treated as not premium until it succeeds.
      setSubscription(null);
    }
    return null;
  }, []);

  useEffect(() => {
    const init = async () => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        try {
          const { data } = await userApi.getMe();
          if (data.success) {
            setUser(data.data);
            await refreshSubscription(data.data.role);
          }
        } catch {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
        }
      }
      setLoading(false);
    };
    init();
  }, [refreshSubscription]);

  const login = async (credentials) => {
    const { data } = await authApi.login(credentials);
    if (data.success) {
      localStorage.setItem('accessToken', data.data.accessToken);
      localStorage.setItem('refreshToken', data.data.refreshToken);
      setUser(data.data.user);
      await refreshSubscription(data.data.user?.role);
    }
    return data;
  };

  const register = async (userData) => {
    const { data } = await authApi.register(userData);
    if (data.success) {
      localStorage.setItem('accessToken', data.data.accessToken);
      localStorage.setItem('refreshToken', data.data.refreshToken);
      setUser(data.data.user);
      // login() and init() both load this; register() did not, so a brand-new
      // user's Premium page showed no subscription panel until a page reload.
      await refreshSubscription(data.data.user?.role);
    }
    return data;
  };

  const logout = async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    // Best effort: revoke server-side, but always clear locally even if the
    // network call fails - the user asked to be logged out.
    try { await authApi.logout(refreshToken); } catch { /* ignore */ }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    setUser(null);
    setSubscription(null);
  };

  return (
    <AuthContext.Provider value={{
      user,
      loading,
      login,
      register,
      logout,
      setUser,
      isAuthenticated: !!user,
      subscription,
      refreshSubscription,
      // Mirrors the backend gate exactly: auth-service only reports a
      // subscription as active when it is PAID and not yet expired.
      isPremium: !!subscription?.active
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

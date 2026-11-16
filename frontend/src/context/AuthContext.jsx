import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import * as authService from '../services/authService';
import { AUTH_EXPIRED_EVENT, clearSession, readSession, saveSession } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => readSession());

  const login = useCallback(async (email, password) => {
    const data = await authService.login(email, password);
    saveSession(data.token, data.user);
    setSession({ token: data.token, user: data.user });
    return data.user;
  }, []);

  const register = useCallback(async (fullName, email, password) => {
    const data = await authService.register(fullName, email, password);
    saveSession(data.token, data.user);
    setSession({ token: data.token, user: data.user });
    return data.user;
  }, []);

  // JWTs are stateless, so "logout" means discarding the token on the client.
  const logout = useCallback(() => {
    clearSession();
    setSession({ token: null, user: null });
  }, []);

  const updateUser = useCallback((user) => {
    setSession((current) => {
      saveSession(current.token, user);
      return { ...current, user };
    });
  }, []);

  // The Axios interceptor fires this event when the API answers 401 (expired token).
  useEffect(() => {
    const handleExpired = () => setSession({ token: null, user: null });
    window.addEventListener(AUTH_EXPIRED_EVENT, handleExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleExpired);
  }, []);

  const value = useMemo(
    () => ({
      user: session.user,
      token: session.token,
      isAuthenticated: Boolean(session.token),
      login,
      register,
      logout,
      updateUser,
    }),
    [session, login, register, logout, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}

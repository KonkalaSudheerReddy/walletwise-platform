import { useEffect, useMemo, useSyncExternalStore, type ReactNode } from 'react';
import {
  getSessionSnapshot,
  login,
  logout,
  registerAccount,
  restoreSession,
  subscribeSession
} from '../api/client';
import { AuthContext, type AuthContextValue } from './auth-context';
import { queryClient } from './query-client';

export function AuthProvider({ children }: { children: ReactNode }) {
  const snapshot = useSyncExternalStore(subscribeSession, getSessionSnapshot, getSessionSnapshot);

  useEffect(() => {
    void restoreSession();
  }, []);

  useEffect(() => {
    if (snapshot.status === 'anonymous') queryClient.clear();
  }, [snapshot.status]);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...snapshot,
      login: async (credentials) => {
        queryClient.clear();
        return login(credentials);
      },
      register: async (payload) => {
        queryClient.clear();
        return registerAccount(payload);
      },
      logout: async () => {
        await logout();
        queryClient.clear();
      }
    }),
    [snapshot]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

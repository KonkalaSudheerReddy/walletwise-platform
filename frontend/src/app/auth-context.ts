import { createContext, useContext } from 'react';
import type { login, registerAccount } from '../api/client';
import type { User } from '../api/types';

export interface AuthContextValue {
  status: 'restoring' | 'authenticated' | 'anonymous';
  user: User | null;
  login: typeof login;
  register: typeof registerAccount;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}

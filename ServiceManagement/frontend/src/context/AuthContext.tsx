import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { User } from '../types';
import { login as loginApi, register as registerApi } from '../api/services';

interface AuthContextValue {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (payload: {
    name: string;
    email: string;
    password: string;
    role: string;
    customerId?: number | null;
  }) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedToken = localStorage.getItem('keystone_token');
    const storedUser = localStorage.getItem('keystone_user');
    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  const persist = (t: string, u: User) => {
    localStorage.setItem('keystone_token', t);
    localStorage.setItem('keystone_user', JSON.stringify(u));
    setToken(t);
    setUser(u);
  };

  const login = async (email: string, password: string) => {
    const res = await loginApi(email, password);
    persist(res.token, res.user);
  };

  const register = async (payload: {
    name: string;
    email: string;
    password: string;
    role: string;
    customerId?: number | null;
  }) => {
    const res = await registerApi(payload);
    persist(res.token, res.user);
  };

  const logout = () => {
    localStorage.removeItem('keystone_token');
    localStorage.removeItem('keystone_user');
    setToken(null);
    setUser(null);
  };

  const value = useMemo(
    () => ({ user, token, loading, login, register, logout }),
    [user, token, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}

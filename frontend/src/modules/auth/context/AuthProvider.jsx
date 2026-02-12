import { useState } from 'react';
import { AuthContext } from './authContext';
import * as authApi from '../api/authApi';

const TOKEN_KEY = 'token';
const USER_KEY = 'user';

/** Reads the stored user from localStorage, or returns null. */
function getStoredUser() {
  const storedUser = localStorage.getItem(USER_KEY);
  const token = localStorage.getItem(TOKEN_KEY);
  if (storedUser && token) {
    try { return JSON.parse(storedUser); } catch { return null; }
  }
  return null;
}

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser);
  const [loading] = useState(false);

  /** Persists JWT and user data to localStorage. */
  const saveSession = (data) => {
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(data));
    setUser(data);
  };

  const login = async (email, password) => {
    const { data } = await authApi.loginUser(email, password);
    saveSession(data);
    return data;
  };

  const register = async (formData) => {
    const { data } = await authApi.registerUser(formData);
    return data;
  };

  const verifyOtp = async (email, otp) => {
    const { data } = await authApi.verifyOtp(email, otp);
    saveSession(data);
    return data;
  };

  const resendOtp = async (email) => {
    const { data } = await authApi.resendOtp(email);
    return data;
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, register, verifyOtp, resendOtp, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

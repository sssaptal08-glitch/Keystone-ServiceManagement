import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { extractErrorMessage } from '../api/client';
import { Wrench } from 'lucide-react';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('manager@keystone.dev');
  const [password, setPassword] = useState('Password123!');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-screen">
      <form className="auth-card" onSubmit={handleSubmit}>
        <div className="auth-brand">
          <Wrench size={20} color="#d97706" />
          <h1>KEYSTONE</h1>
        </div>
        <p className="subtitle">Field Service Management Platform</p>

        {error && <div className="alert alert-error">{error}</div>}

        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>

        <button className="btn btn-primary" type="submit" disabled={submitting} style={{ justifyContent: 'center' }}>
          {submitting ? 'Signing in...' : 'Sign in'}
        </button>

        <div className="demo-hint">
          <strong>Demo accounts</strong> (password: <code>Password123!</code>)
          <ul>
            <li>manager@keystone.dev — Manager</li>
            <li>dispatcher@keystone.dev — Dispatcher</li>
            <li>tech1@keystone.dev — Technician</li>
            <li>customer@keystone.dev — Customer</li>
          </ul>
        </div>
      </form>
    </div>
  );
}

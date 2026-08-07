import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { extractErrorMessage } from '../api/client';
import { Wrench, ShieldCheck, Zap, LayoutDashboard, BellRing } from 'lucide-react';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('manager@keystone.example');
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
      <div className="auth-layout">
        <section className="auth-hero">
          <div className="auth-badge">
            <Wrench size={16} />
            Keystone Service Cloud
          </div>
          <h1>Run every dispatch workflow from one premium control room.</h1>
          <p>
            Monitor live jobs, manage the SLA pipeline, coordinate technicians, and keep customers informed with a cleaner, faster field service experience.
          </p>

          <div className="auth-feature-grid">
            <div className="auth-feature-card">
              <ShieldCheck size={18} />
              <span>Role-aware access</span>
            </div>
            <div className="auth-feature-card">
              <Zap size={18} />
              <span>Live service updates</span>
            </div>
            <div className="auth-feature-card">
              <LayoutDashboard size={18} />
              <span>Executive dashboard</span>
            </div>
            <div className="auth-feature-card">
              <BellRing size={18} />
              <span>Instant notifications</span>
            </div>
          </div>
        </section>

        <form className="auth-card" onSubmit={handleSubmit}>
          <div className="auth-brand">
            <Wrench size={20} className="brand-mark" />
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
              <li>manager@keystone.example — Manager</li>
              <li>dispatcher@keystone.example — Dispatcher</li>
              <li>tom.tech@keystone.example — Technician</li>
              <li>carla@acme-mfg.example — Customer</li>
            </ul>
          </div>
        </form>
      </div>
    </div>
  );
}

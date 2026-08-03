import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';

export default function Settings() {
  const { user } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [timezone, setTimezone] = useState('UTC');

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Settings</h1>
          <p className="page-subtitle">Update your preferences, security settings, and display options.</p>
        </div>
      </div>

      <div className="panel-grid">
        <div className="panel">
          <h3>Profile</h3>
          <div className="form-grid">
            <label>Name<input value={user?.name ?? ''} disabled /></label>
            <label>Email<input value={user?.email ?? ''} disabled /></label>
            <label>Role<input value={user?.role ?? ''} disabled /></label>
          </div>
        </div>

        <div className="panel">
          <h3>Preferences</h3>
          <label>
            Theme mode
            <button className="btn btn-primary" type="button" onClick={toggleTheme}>
              Switch to {theme === 'light' ? 'dark' : 'light'} mode
            </button>
          </label>
          <label style={{ marginTop: '1rem' }}>
            Timezone
            <select value={timezone} onChange={(e) => setTimezone(e.target.value)}>
              <option value="UTC">UTC</option>
              <option value="Local">Local browser timezone</option>
            </select>
          </label>
        </div>
      </div>
    </div>
  );
}

import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { LayoutDashboard, ClipboardList, Building2, Boxes, Wrench, LogOut, Sun, Moon, History } from 'lucide-react';
import NotificationBell from './NotificationBell';

const navItemsByRole: Record<string, { to: string; label: string; icon: React.ElementType }[]> = {
  MANAGER: [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/work-orders', label: 'Work Orders', icon: ClipboardList },
    { to: '/customers', label: 'Customers', icon: Building2 },
    { to: '/parts', label: 'Inventory', icon: Boxes },
    { to: '/activity', label: 'Activity', icon: History }
  ],
  DISPATCHER: [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/work-orders', label: 'Work Orders', icon: ClipboardList },
    { to: '/customers', label: 'Customers', icon: Building2 },
    { to: '/parts', label: 'Inventory', icon: Boxes },
    { to: '/activity', label: 'Activity', icon: History }
  ],
  TECHNICIAN: [
    { to: '/', label: 'My Jobs', icon: LayoutDashboard },
    { to: '/work-orders', label: 'All Work Orders', icon: ClipboardList }
  ],
  CUSTOMER: [
    { to: '/', label: 'Overview', icon: LayoutDashboard },
    { to: '/work-orders', label: 'My Requests', icon: ClipboardList }
  ]
};

export default function Layout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  const items = user ? navItemsByRole[user.role] ?? [] : [];

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <Wrench size={18} className="brand-mark" />
          KEYSTONE
        </div>
        <nav className="nav">
          {items.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              >
                <Icon size={15} />
                {item.label}
              </NavLink>
            );
          })}
        </nav>
        <div className="user-menu">
          {user && <NotificationBell />}
          <button className="btn btn-ghost theme-toggle" onClick={toggleTheme} title="Toggle dark mode" aria-label="Toggle dark mode">
            {theme === 'light' ? <Moon size={15} /> : <Sun size={15} />}
          </button>
          {user && (
            <>
              <span className="user-name">
                {user.name} <span className="role-pill">{user.role}</span>
              </span>
              <button
                className="btn btn-ghost"
                onClick={() => {
                  logout();
                  navigate('/login');
                }}
              >
                <LogOut size={14} /> Log out
              </button>
            </>
          )}
        </div>
      </header>
      <main className="content">{children}</main>
    </div>
  );
}

import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import {
  LayoutDashboard,
  ClipboardList,
  Building2,
  Boxes,
  Wrench,
  LogOut,
  Sun,
  Moon,
  History,
  ChevronRight,
  BarChart3,
  Users,
  Bell,
  Settings2
} from 'lucide-react';
import NotificationBell from './NotificationBell';

const navItemsByRole: Record<string, { to: string; label: string; icon: React.ElementType }[]> = {
  MANAGER: [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/work-orders', label: 'Work Orders', icon: ClipboardList },
    { to: '/customers', label: 'Customers', icon: Building2 },
    { to: '/parts', label: 'Inventory', icon: Boxes },
    { to: '/activity', label: 'Activity', icon: History },
    { to: '/reports', label: 'Reports', icon: BarChart3 },
    { to: '/team', label: 'Team', icon: Users },
    { to: '/notifications', label: 'Notifications', icon: Bell },
    { to: '/settings', label: 'Settings', icon: Settings2 }
  ],
  DISPATCHER: [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/work-orders', label: 'Work Orders', icon: ClipboardList },
    { to: '/customers', label: 'Customers', icon: Building2 },
    { to: '/parts', label: 'Inventory', icon: Boxes },
    { to: '/activity', label: 'Activity', icon: History },
    { to: '/reports', label: 'Reports', icon: BarChart3 },
    { to: '/team', label: 'Team', icon: Users },
    { to: '/notifications', label: 'Notifications', icon: Bell },
    { to: '/settings', label: 'Settings', icon: Settings2 }
  ],
  TECHNICIAN: [
    { to: '/', label: 'My Jobs', icon: LayoutDashboard },
    { to: '/work-orders', label: 'All Work Orders', icon: ClipboardList },
    { to: '/notifications', label: 'Notifications', icon: Bell },
    { to: '/settings', label: 'Settings', icon: Settings2 }
  ],
  CUSTOMER: [
    { to: '/', label: 'Overview', icon: LayoutDashboard },
    { to: '/work-orders', label: 'My Requests', icon: ClipboardList },
    { to: '/notifications', label: 'Notifications', icon: Bell },
    { to: '/settings', label: 'Settings', icon: Settings2 }
  ]
};

export default function Layout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  const items = user ? navItemsByRole[user.role] ?? [] : [];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-icon">
            <Wrench size={18} />
          </div>
          <div>
            <div className="brand-title">KEYSTONE</div>
            <div className="brand-subtitle">Field Service OS</div>
          </div>
        </div>

        <div className="sidebar-group">
          <div className="sidebar-label">Navigation</div>
          <nav className="sidebar-nav">
            {items.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) => (isActive ? 'sidebar-link active' : 'sidebar-link')}
                >
                  <Icon size={16} />
                  <span>{item.label}</span>
                  <ChevronRight size={14} />
                </NavLink>
              );
            })}
          </nav>
        </div>

        <div className="sidebar-card">
          <div className="sidebar-card-title">Current role</div>
          <div className="sidebar-card-role">{user?.role ?? 'Guest'}</div>
          <div className="sidebar-card-body">Secure access, a live activity feed, and SLA-aware dispatch workflows are available in this workspace.</div>
        </div>
      </aside>

      <div className="workspace">
        <header className="topbar">
          <div className="topbar-title-wrap">
            <div className="topbar-title">Operations command center</div>
            <div className="topbar-subtitle">Track work, parts, customers, and live SLA risk from one place.</div>
          </div>

          <div className="user-menu">
            {user && <NotificationBell />}
            <button className="btn btn-ghost theme-toggle" onClick={toggleTheme} title="Toggle dark mode" aria-label="Toggle dark mode">
              {theme === 'light' ? <Moon size={15} /> : <Sun size={15} />}
            </button>
            {user && (
              <>
                <span className="user-name">
                  {user.name}
                  <span className="role-pill">{user.role}</span>
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
    </div>
  );
}

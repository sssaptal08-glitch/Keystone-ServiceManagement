import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import WorkOrders from './pages/WorkOrders';
import WorkOrderDetail from './pages/WorkOrderDetail';
import Customers from './pages/Customers';
import Parts from './pages/Parts';
import Activity from './pages/Activity';

export default function App() {
  return (
    <ThemeProvider>
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout><Dashboard /></Layout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/work-orders"
            element={
              <ProtectedRoute>
                <Layout><WorkOrders /></Layout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/work-orders/:id"
            element={
              <ProtectedRoute>
                <Layout><WorkOrderDetail /></Layout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/customers"
            element={
              <ProtectedRoute allowedRoles={['DISPATCHER', 'MANAGER']}>
                <Layout><Customers /></Layout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/parts"
            element={
              <ProtectedRoute allowedRoles={['DISPATCHER', 'MANAGER', 'TECHNICIAN']}>
                <Layout><Parts /></Layout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/activity"
            element={
              <ProtectedRoute allowedRoles={['DISPATCHER', 'MANAGER']}>
                <Layout><Activity /></Layout>
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
    </ThemeProvider>
  );
}

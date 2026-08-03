import React, { useEffect, useState } from 'react';
import { fetchTechnicians, fetchCustomers } from '../api/services';
import type { TechnicianWorkload, Customer } from '../types';
import { Search } from 'lucide-react';

export default function Team() {
  const [technicians, setTechnicians] = useState<TechnicianWorkload[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [search, setSearch] = useState('');

  useEffect(() => {
    fetchTechnicians().then(setTechnicians).catch(() => {});
    fetchCustomers({ size: 200 }).then((page) => setCustomers(page.content)).catch(() => {});
  }, []);

  const filteredTechs = technicians.filter((tech) => tech.name.toLowerCase().includes(search.toLowerCase()));

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Team</h1>
          <p className="page-subtitle">Technician workload, customer accounts, and active service capacity.</p>
        </div>
      </div>

      <div className="panel-grid">
        <div className="panel">
          <h3>Technician workload</h3>
          <div className="search-bar" style={{ marginBottom: '1rem' }}>
            <Search size={14} style={{ position: 'relative', top: 1 }} />
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search technicians…" />
          </div>
          <div className="team-table-wrap">
            <table className="table">
              <thead>
                <tr><th>Name</th><th>Email</th><th>Open jobs</th></tr>
              </thead>
              <tbody>
                {filteredTechs.map((tech) => (
                  <tr key={tech.id}>
                    <td>{tech.name}</td>
                    <td>{tech.email}</td>
                    <td>{tech.openJobCount}</td>
                  </tr>
                ))}
                {filteredTechs.length === 0 && (
                  <tr><td colSpan={3}>No technician data matches your search.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="panel">
          <h3>Customer roster</h3>
          <p className="panel-description">The customer list helps dispatchers and managers understand the active service base.</p>
          <div className="team-table-wrap">
            <table className="table">
              <thead><tr><th>Name</th><th>Email</th><th>Phone</th></tr></thead>
              <tbody>
                {customers.map((customer) => (
                  <tr key={customer.id}>
                    <td>{customer.name}</td>
                    <td>{customer.contactEmail ?? '—'}</td>
                    <td>{customer.contactPhone ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

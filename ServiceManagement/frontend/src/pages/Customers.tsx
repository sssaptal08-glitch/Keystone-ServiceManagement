import React, { useEffect, useState, useCallback } from 'react';
import { fetchCustomers, createCustomer, fetchSites, createSite } from '../api/services';
import type { Customer, Site } from '../types';
import { extractErrorMessage } from '../api/client';
import Pagination from '../components/Pagination';
import { Search } from 'lucide-react';

const PAGE_SIZE = 10;

export default function Customers() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    fetchCustomers({ search: search || undefined, page, size: PAGE_SIZE }).then((res) => {
      setCustomers(res.content);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    }).catch(() => {});
    fetchSites().then(setSites).catch(() => {});
  }, [search, page]);

  useEffect(load, [load]);
  useEffect(() => { setPage(0); }, [search]);

  return (
    <div>
      <h1>Customers & Sites</h1>
      {error && <div className="alert alert-error">{error}</div>}

      <div className="panel-grid">
        <div className="panel">
          <h3>Customers</h3>
          <div className="search-bar" style={{ marginBottom: '0.75rem' }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <Search size={13} style={{ position: 'absolute', left: 9, top: 9, color: 'var(--color-muted)' }} />
              <input style={{ paddingLeft: '1.8rem', width: '100%' }} placeholder="Search customers…" value={search} onChange={(e) => setSearch(e.target.value)} />
            </div>
          </div>
          <table className="table">
            <thead><tr><th>Name</th><th>Email</th><th>Phone</th></tr></thead>
            <tbody>
              {customers.map((c) => (
                <tr key={c.id}><td>{c.name}</td><td>{c.contactEmail}</td><td>{c.contactPhone}</td></tr>
              ))}
              {customers.length === 0 && <tr><td colSpan={3}>No customers found.</td></tr>}
            </tbody>
          </table>
          <Pagination page={page} totalPages={totalPages} totalElements={totalElements} onPageChange={setPage} />
          <NewCustomerForm onCreated={load} onError={(m) => setError(m)} />
        </div>

        <div className="panel">
          <h3>Sites</h3>
          <table className="table">
            <thead><tr><th>Name</th><th>Customer</th><th>Address</th></tr></thead>
            <tbody>
              {sites.map((s) => (
                <tr key={s.id}><td>{s.name}</td><td>{s.customerName}</td><td>{s.address}</td></tr>
              ))}
            </tbody>
          </table>
          <NewSiteForm customers={customers} onCreated={load} onError={(m) => setError(m)} />
        </div>
      </div>
    </div>
  );
}

function NewCustomerForm({ onCreated, onError }: { onCreated: () => void; onError: (m: string) => void }) {
  const [name, setName] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [contactPhone, setContactPhone] = useState('');

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createCustomer({ name, contactEmail, contactPhone });
      setName(''); setContactEmail(''); setContactPhone('');
      onCreated();
    } catch (err) {
      onError(extractErrorMessage(err));
    }
  };

  return (
    <form className="form-panel" onSubmit={submit}>
      <div className="form-grid">
        <label>Name<input value={name} onChange={(e) => setName(e.target.value)} required /></label>
        <label>Email<input value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} /></label>
        <label>Phone<input value={contactPhone} onChange={(e) => setContactPhone(e.target.value)} /></label>
      </div>
      <button className="btn btn-primary" type="submit">+ Add Customer</button>
    </form>
  );
}

function NewSiteForm({ customers, onCreated, onError }: { customers: Customer[]; onCreated: () => void; onError: (m: string) => void }) {
  const [customerId, setCustomerId] = useState<number | ''>('');
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [postalCode, setPostalCode] = useState('');

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!customerId) { onError('Please choose a customer'); return; }
    try {
      await createSite({ customerId: Number(customerId), name, address, city, state, postalCode });
      setName(''); setAddress(''); setCity(''); setState(''); setPostalCode('');
      onCreated();
    } catch (err) {
      onError(extractErrorMessage(err));
    }
  };

  return (
    <form className="form-panel" onSubmit={submit}>
      <div className="form-grid">
        <label>
          Customer
          <select value={customerId} onChange={(e) => setCustomerId(e.target.value ? Number(e.target.value) : '')} required>
            <option value="">Select…</option>
            {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        <label>Site name<input value={name} onChange={(e) => setName(e.target.value)} required /></label>
        <label>Address<input value={address} onChange={(e) => setAddress(e.target.value)} required /></label>
        <label>City<input value={city} onChange={(e) => setCity(e.target.value)} /></label>
        <label>State<input value={state} onChange={(e) => setState(e.target.value)} /></label>
        <label>Postal code<input value={postalCode} onChange={(e) => setPostalCode(e.target.value)} /></label>
      </div>
      <button className="btn btn-primary" type="submit">+ Add Site</button>
    </form>
  );
}

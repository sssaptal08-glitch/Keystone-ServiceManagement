import React, { useEffect, useState, useCallback } from 'react';
import { fetchParts, createPart, restockPart } from '../api/services';
import type { Part } from '../types';
import { extractErrorMessage } from '../api/client';
import Pagination from '../components/Pagination';
import { Search } from 'lucide-react';

const PAGE_SIZE = 10;

export default function Parts() {
  const [parts, setParts] = useState<Part[]>([]);
  const [search, setSearch] = useState('');
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    fetchParts({ search: search || undefined, lowStock: lowStockOnly || undefined, page, size: PAGE_SIZE })
      .then((res) => {
        setParts(res.content);
        setTotalPages(res.totalPages);
        setTotalElements(res.totalElements);
      })
      .catch(() => {});
  }, [search, lowStockOnly, page]);

  useEffect(load, [load]);
  useEffect(() => { setPage(0); }, [search, lowStockOnly]);

  const handleRestock = async (id: number) => {
    const qty = Number(window.prompt('How many units to add?', '10'));
    if (!qty || qty <= 0) return;
    try {
      await restockPart(id, qty);
      load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <div>
      <h1>Inventory</h1>
      {error && <div className="alert alert-error">{error}</div>}

      <div className="search-bar">
        <div style={{ position: 'relative', flex: 1, maxWidth: 320 }}>
          <Search size={14} style={{ position: 'absolute', left: 10, top: 10, color: 'var(--color-muted)' }} />
          <input style={{ paddingLeft: '2rem', width: '100%' }} placeholder="Search by name or SKU…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem' }}>
          <input type="checkbox" checked={lowStockOnly} onChange={(e) => setLowStockOnly(e.target.checked)} style={{ width: 'auto' }} />
          Low stock only
        </label>
      </div>

      <table className="table">
        <thead>
          <tr><th>SKU</th><th>Name</th><th>Unit Cost</th><th>On Hand</th><th>Reorder At</th><th></th></tr>
        </thead>
        <tbody>
          {parts.map((p) => (
            <tr key={p.id} className={p.lowStock ? 'row-overdue' : ''}>
              <td>{p.sku}</td>
              <td>{p.name}</td>
              <td>${p.unitCost.toFixed(2)}</td>
              <td>{p.quantityOnHand}{p.lowStock && ' ⚠️'}</td>
              <td>{p.reorderThreshold}</td>
              <td><button className="btn btn-ghost" onClick={() => handleRestock(p.id)}>Restock</button></td>
            </tr>
          ))}
          {parts.length === 0 && <tr><td colSpan={6}>No parts found.</td></tr>}
        </tbody>
      </table>

      <Pagination page={page} totalPages={totalPages} totalElements={totalElements} onPageChange={setPage} />

      <NewPartForm onCreated={load} onError={setError} />
    </div>
  );
}

function NewPartForm({ onCreated, onError }: { onCreated: () => void; onError: (m: string) => void }) {
  const [sku, setSku] = useState('');
  const [name, setName] = useState('');
  const [unitCost, setUnitCost] = useState('');
  const [quantityOnHand, setQuantityOnHand] = useState('0');
  const [reorderThreshold, setReorderThreshold] = useState('5');

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createPart({
        sku,
        name,
        unitCost: Number(unitCost),
        quantityOnHand: Number(quantityOnHand),
        reorderThreshold: Number(reorderThreshold)
      });
      setSku(''); setName(''); setUnitCost(''); setQuantityOnHand('0'); setReorderThreshold('5');
      onCreated();
    } catch (err) {
      onError(extractErrorMessage(err));
    }
  };

  return (
    <form className="panel form-panel" onSubmit={submit}>
      <h3>Add Part</h3>
      <div className="form-grid">
        <label>SKU<input value={sku} onChange={(e) => setSku(e.target.value)} required /></label>
        <label>Name<input value={name} onChange={(e) => setName(e.target.value)} required /></label>
        <label>Unit cost<input type="number" step="0.01" value={unitCost} onChange={(e) => setUnitCost(e.target.value)} required /></label>
        <label>Initial qty<input type="number" value={quantityOnHand} onChange={(e) => setQuantityOnHand(e.target.value)} /></label>
        <label>Reorder threshold<input type="number" value={reorderThreshold} onChange={(e) => setReorderThreshold(e.target.value)} /></label>
      </div>
      <button className="btn btn-primary" type="submit">+ Add Part</button>
    </form>
  );
}

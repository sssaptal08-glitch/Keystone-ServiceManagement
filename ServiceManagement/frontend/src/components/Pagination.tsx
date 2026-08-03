import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export default function Pagination({
  page, totalPages, totalElements, onPageChange
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;

  return (
    <div className="pagination-bar">
      <span className="pagination-info">
        Page {page + 1} of {totalPages} · {totalElements} total
      </span>
      <div className="pagination-buttons">
        <button className="btn btn-secondary" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft size={14} /> Prev
        </button>
        <button className="btn btn-secondary" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>
          Next <ChevronRight size={14} />
        </button>
      </div>
    </div>
  );
}

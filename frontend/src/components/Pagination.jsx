/** Pagination bar. `page` is 0-based (as in the API); the UI shows 1-based numbers. */
export default function Pagination({ page, size, totalElements, totalPages, onPageChange }) {
  if (totalElements === 0) return null;

  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  const windowStart = Math.max(0, Math.min(page - 2, totalPages - 5));
  const windowEnd = Math.min(totalPages, windowStart + 5);
  const pages = [];
  for (let p = windowStart; p < windowEnd; p += 1) pages.push(p);

  return (
    <nav className="pagination" aria-label="Pagination">
      <p className="pagination-summary">
        Showing {from}–{to} of {totalElements}
      </p>
      <div className="pagination-controls">
        <button type="button" className="btn btn-sm btn-secondary" onClick={() => onPageChange(page - 1)} disabled={page === 0}>
          Previous
        </button>
        {pages.map((p) => (
          <button
            key={p}
            type="button"
            className={`btn btn-sm ${p === page ? 'btn-primary' : 'btn-secondary'}`}
            aria-label={`Page ${p + 1}`}
            aria-current={p === page ? 'page' : undefined}
            onClick={() => onPageChange(p)}
          >
            {p + 1}
          </button>
        ))}
        <button
          type="button"
          className="btn btn-sm btn-secondary"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
        >
          Next
        </button>
      </div>
    </nav>
  );
}

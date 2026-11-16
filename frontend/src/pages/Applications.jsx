import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import ApplicationsTable from '../components/ApplicationsTable';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import ErrorMessage from '../components/ErrorMessage';
import Icon from '../components/Icon';
import Pagination from '../components/Pagination';
import Spinner from '../components/Spinner';
import { useToast } from '../context/ToastContext';
import useDebounce from '../hooks/useDebounce';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { deleteApplication, exportApplicationsCsv, searchApplications } from '../services/applicationService';
import { getErrorMessage } from '../services/api';
import { JOB_TYPE_OPTIONS, PAGE_SIZE_OPTIONS, SORT_OPTIONS, STATUS_OPTIONS } from '../utils/constants';

const INITIAL_FILTERS = { search: '', status: '', jobType: '', dateFrom: '', dateTo: '', sort: 'newest', size: 10 };

export default function Applications() {
  useDocumentTitle('Applications');
  const toast = useToast();
  const [filters, setFilters] = useState(INITIAL_FILTERS);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [exporting, setExporting] = useState(false);

  const debouncedSearch = useDebounce(filters.search, 400);
  const { status, jobType, dateFrom, dateTo, sort, size } = filters;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    searchApplications({ search: debouncedSearch.trim(), status, jobType, dateFrom, dateTo, sort, page, size })
      .then((data) => {
        if (!cancelled) setResult(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, 'Could not load applications.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    // Ignore a slow response if the filters changed in the meantime.
    return () => {
      cancelled = true;
    };
  }, [debouncedSearch, status, jobType, dateFrom, dateTo, sort, page, size, reloadKey]);

  const updateFilter = (name, value) => {
    setFilters((current) => ({ ...current, [name]: value }));
    setPage(0);
  };

  const hasActiveFilters = Boolean(filters.search || status || jobType || dateFrom || dateTo);

  const clearFilters = () => {
    setFilters((current) => ({ ...INITIAL_FILTERS, sort: current.sort, size: current.size }));
    setPage(0);
  };

  const confirmDelete = useCallback(async () => {
    setDeleting(true);
    try {
      await deleteApplication(toDelete.id);
      toast.success(`Deleted ${toDelete.companyName} — ${toDelete.jobTitle}`);
      setToDelete(null);
      // If that was the last row of the final page, step back one page.
      if (result && result.content.length === 1 && page > 0) setPage(page - 1);
      else setReloadKey((key) => key + 1);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not delete the application.'));
    } finally {
      setDeleting(false);
    }
  }, [toDelete, toast, result, page]);

  const handleExport = async () => {
    setExporting(true);
    try {
      await exportApplicationsCsv();
      toast.success('CSV export downloaded');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not export applications.'));
    } finally {
      setExporting(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Applications</h1>
          <p className="muted">Search, filter and manage every application.</p>
        </div>
        <div className="header-actions">
          <button type="button" className="btn btn-secondary" onClick={handleExport} disabled={exporting}>
            <Icon name="download" size={16} /> {exporting ? 'Exporting…' : 'Export CSV'}
          </button>
          <Link to="/applications/new" className="btn btn-primary">
            <Icon name="plus" size={16} /> Add application
          </Link>
        </div>
      </div>

      <div className="card filters" role="search" aria-label="Filter applications">
        <div className="field">
          <label htmlFor="search">Search company or job title</label>
          <input id="search" type="search" value={filters.search} placeholder="e.g. Globex or Developer"
            onChange={(event) => updateFilter('search', event.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="filterStatus">Status</label>
          <select id="filterStatus" value={filters.status} onChange={(event) => updateFilter('status', event.target.value)}>
            <option value="">All statuses</option>
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="filterJobType">Job type</label>
          <select id="filterJobType" value={filters.jobType} onChange={(event) => updateFilter('jobType', event.target.value)}>
            <option value="">All types</option>
            {JOB_TYPE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="dateFrom">Applied from</label>
          <input id="dateFrom" type="date" value={filters.dateFrom} max={filters.dateTo || undefined}
            onChange={(event) => updateFilter('dateFrom', event.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="dateTo">Applied to</label>
          <input id="dateTo" type="date" value={filters.dateTo} min={filters.dateFrom || undefined}
            onChange={(event) => updateFilter('dateTo', event.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="sort">Sort by</label>
          <select id="sort" value={filters.sort} onChange={(event) => updateFilter('sort', event.target.value)}>
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="pageSize">Per page</label>
          <select id="pageSize" value={filters.size} onChange={(event) => updateFilter('size', Number(event.target.value))}>
            {PAGE_SIZE_OPTIONS.map((option) => (
              <option key={option} value={option}>{option}</option>
            ))}
          </select>
        </div>
        {hasActiveFilters && (
          <div className="field field-end">
            <button type="button" className="btn btn-secondary" onClick={clearFilters}>
              Clear filters
            </button>
          </div>
        )}
      </div>

      <ErrorMessage message={error} onRetry={() => setReloadKey((key) => key + 1)} />
      {loading && !result && <Spinner label="Loading applications…" />}

      {result && (
        <div className={`card list-card${loading ? ' is-refreshing' : ''}`} aria-busy={loading}>
          {result.content.length === 0 ? (
            hasActiveFilters ? (
              <EmptyState
                title="No applications match your filters"
                description="Try a different search term or clear the filters."
                action={<button type="button" className="btn btn-secondary" onClick={clearFilters}>Clear filters</button>}
              />
            ) : (
              <EmptyState
                title="No applications yet"
                description="Start tracking your job search by adding your first application."
                action={<Link to="/applications/new" className="btn btn-primary">Add application</Link>}
              />
            )
          ) : (
            <>
              <ApplicationsTable applications={result.content} onDelete={setToDelete} />
              <Pagination page={result.page} size={result.size} totalElements={result.totalElements}
                totalPages={result.totalPages} onPageChange={setPage} />
            </>
          )}
        </div>
      )}

      {toDelete && (
        <ConfirmDialog
          title="Delete application?"
          message={`This permanently deletes "${toDelete.companyName} — ${toDelete.jobTitle}" together with its interviews and notes.`}
          onConfirm={confirmDelete}
          onCancel={() => setToDelete(null)}
          busy={deleting}
        />
      )}
    </>
  );
}

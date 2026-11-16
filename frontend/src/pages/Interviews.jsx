import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import ErrorMessage from '../components/ErrorMessage';
import InterviewForm from '../components/InterviewForm';
import InterviewList from '../components/InterviewList';
import Modal from '../components/Modal';
import Spinner from '../components/Spinner';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { getErrorMessage, getFieldErrors } from '../services/api';
import { searchApplications } from '../services/applicationService';
import { createInterview, deleteInterview, listInterviews, updateInterview } from '../services/interviewService';
import { nowLocalDateTime } from '../utils/format';

const TABS = [
  { id: 'upcoming', label: 'Upcoming' },
  { id: 'past', label: 'Past' },
  { id: 'all', label: 'All' },
];

export default function Interviews() {
  useDocumentTitle('Interviews');
  const toast = useToast();
  const [interviews, setInterviews] = useState([]);
  const [applications, setApplications] = useState([]);
  const [tab, setTab] = useState('upcoming');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [dialog, setDialog] = useState(null); // {} for new, { interview } for edit
  const [toDelete, setToDelete] = useState(null);
  const [saving, setSaving] = useState(false);
  const [formErrors, setFormErrors] = useState({});

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [interviewList, applicationPage] = await Promise.all([
        listInterviews(),
        searchApplications({ size: 100, sort: 'newest' }),
      ]);
      setInterviews(interviewList);
      setApplications(applicationPage.content);
    } catch (err) {
      setError(getErrorMessage(err, 'Could not load interviews.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // scheduledAt has no time zone ("2026-11-20T14:30:00"), so plain string comparison with the
  // current local time in the same format is correct and avoids time-zone surprises.
  const visible = useMemo(() => {
    const now = nowLocalDateTime();
    const sorted = [...interviews].sort((a, b) => a.scheduledAt.localeCompare(b.scheduledAt));
    if (tab === 'upcoming') return sorted.filter((i) => i.scheduledAt.slice(0, 16) >= now);
    if (tab === 'past') return sorted.filter((i) => i.scheduledAt.slice(0, 16) < now).reverse();
    return sorted.reverse();
  }, [interviews, tab]);

  const closeDialogs = () => {
    setDialog(null);
    setToDelete(null);
    setFormErrors({});
  };

  const handleSave = async (body, applicationId) => {
    setSaving(true);
    setFormErrors({});
    try {
      if (dialog.interview) {
        await updateInterview(dialog.interview.id, body);
        toast.success('Interview updated');
      } else {
        await createInterview(applicationId, body);
        toast.success('Interview scheduled');
      }
      closeDialogs();
      setInterviews(await listInterviews());
    } catch (err) {
      setFormErrors(getFieldErrors(err));
      toast.error(getErrorMessage(err, 'Could not save the interview.'));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setSaving(true);
    try {
      await deleteInterview(toDelete.id);
      toast.success('Interview deleted');
      closeDialogs();
      setInterviews(await listInterviews());
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not delete the interview.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Interviews</h1>
          <p className="muted">Every interview across all your applications.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setDialog({})} disabled={applications.length === 0}>
          Schedule interview
        </button>
      </div>

      {loading && <Spinner label="Loading interviews…" />}
      {!loading && error && <ErrorMessage message={error} onRetry={load} />}

      {!loading && !error && (
        <div className="card">
          <div className="tabs" role="tablist" aria-label="Interview filter">
            {TABS.map((item) => (
              <button key={item.id} type="button" role="tab" aria-selected={tab === item.id}
                className={`tab${tab === item.id ? ' tab-active' : ''}`} onClick={() => setTab(item.id)}>
                {item.label}
              </button>
            ))}
          </div>
          {applications.length === 0 ? (
            <EmptyState
              title="Add an application first"
              description="Interviews belong to an application."
              action={<Link to="/applications/new" className="btn btn-primary">Add application</Link>}
            />
          ) : visible.length === 0 ? (
            <EmptyState
              title={tab === 'past' ? 'No past interviews' : tab === 'upcoming' ? 'No upcoming interviews' : 'No interviews yet'}
              description="Schedule an interview to see it here."
            />
          ) : (
            <InterviewList interviews={visible} onEdit={(interview) => setDialog({ interview })} onDelete={setToDelete} />
          )}
        </div>
      )}

      {dialog && (
        <Modal title={dialog.interview ? 'Edit interview' : 'Schedule interview'} onClose={closeDialogs} wide>
          <InterviewForm initialValues={dialog.interview} applications={applications} onSubmit={handleSave}
            onCancel={closeDialogs} submitting={saving} serverErrors={formErrors} />
        </Modal>
      )}
      {toDelete && (
        <ConfirmDialog
          title="Delete interview?"
          message={`"${toDelete.roundName}" at ${toDelete.companyName} will be permanently deleted.`}
          onConfirm={handleDelete}
          onCancel={closeDialogs}
          busy={saving}
        />
      )}
    </>
  );
}

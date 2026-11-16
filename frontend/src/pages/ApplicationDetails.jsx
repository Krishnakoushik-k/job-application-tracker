import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ConfirmDialog from '../components/ConfirmDialog';
import ErrorMessage from '../components/ErrorMessage';
import InterviewForm from '../components/InterviewForm';
import InterviewList from '../components/InterviewList';
import Modal from '../components/Modal';
import NoteForm from '../components/NoteForm';
import Spinner from '../components/Spinner';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { getErrorMessage, getFieldErrors } from '../services/api';
import { deleteApplication, getApplication } from '../services/applicationService';
import { createInterview, deleteInterview, listApplicationInterviews, updateInterview } from '../services/interviewService';
import { createNote, deleteNote, listNotes, updateNote } from '../services/noteService';
import { jobTypeLabel, noteCategoryLabel } from '../utils/constants';
import { formatDate, formatInstant } from '../utils/format';

function DetailItem({ label, children }) {
  return (
    <div className="detail-item">
      <dt>{label}</dt>
      <dd>{children || <span className="muted">—</span>}</dd>
    </div>
  );
}

export default function ApplicationDetails() {
  useDocumentTitle('Application details');
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [application, setApplication] = useState(null);
  const [interviews, setInterviews] = useState([]);
  const [notes, setNotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // dialog state
  const [interviewDialog, setInterviewDialog] = useState(null); // { interview? }
  const [noteDialog, setNoteDialog] = useState(null); // { note? }
  const [confirm, setConfirm] = useState(null); // { kind, item }
  const [saving, setSaving] = useState(false);
  const [formErrors, setFormErrors] = useState({});

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [app, interviewList, noteList] = await Promise.all([
        getApplication(id),
        listApplicationInterviews(id),
        listNotes(id),
      ]);
      setApplication(app);
      setInterviews(interviewList);
      setNotes(noteList);
    } catch (err) {
      setError(getErrorMessage(err, 'Could not load the application.'));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  const refreshChildren = async () => {
    const [interviewList, noteList] = await Promise.all([listApplicationInterviews(id), listNotes(id)]);
    setInterviews(interviewList);
    setNotes(noteList);
  };

  const closeDialogs = () => {
    setInterviewDialog(null);
    setNoteDialog(null);
    setConfirm(null);
    setFormErrors({});
  };

  const saveInterview = async (body) => {
    setSaving(true);
    setFormErrors({});
    try {
      if (interviewDialog.interview) {
        await updateInterview(interviewDialog.interview.id, body);
        toast.success('Interview updated');
      } else {
        await createInterview(id, body);
        toast.success('Interview scheduled');
      }
      await refreshChildren();
      closeDialogs();
    } catch (err) {
      setFormErrors(getFieldErrors(err));
      toast.error(getErrorMessage(err, 'Could not save the interview.'));
    } finally {
      setSaving(false);
    }
  };

  const saveNote = async (body) => {
    setSaving(true);
    setFormErrors({});
    try {
      if (noteDialog.note) {
        await updateNote(noteDialog.note.id, body);
        toast.success('Note updated');
      } else {
        await createNote(id, body);
        toast.success('Note added');
      }
      await refreshChildren();
      closeDialogs();
    } catch (err) {
      setFormErrors(getFieldErrors(err));
      toast.error(getErrorMessage(err, 'Could not save the note.'));
    } finally {
      setSaving(false);
    }
  };

  const runDelete = async () => {
    setSaving(true);
    try {
      if (confirm.kind === 'application') {
        await deleteApplication(id);
        toast.success('Application deleted');
        navigate('/applications', { replace: true });
        return;
      }
      if (confirm.kind === 'interview') {
        await deleteInterview(confirm.item.id);
        toast.success('Interview deleted');
      } else {
        await deleteNote(confirm.item.id);
        toast.success('Note deleted');
      }
      await refreshChildren();
      closeDialogs();
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not delete.'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Spinner label="Loading application…" />;
  if (error) {
    return (
      <>
        <ErrorMessage message={error} onRetry={load} />
        <Link to="/applications" className="btn btn-secondary">Back to applications</Link>
      </>
    );
  }

  const confirmText = {
    application: `This permanently deletes "${application.companyName} — ${application.jobTitle}" with all its interviews and notes.`,
    interview: 'This interview will be permanently deleted.',
    note: 'This note will be permanently deleted.',
  };

  return (
    <>
      <div className="page-header">
        <div>
          <p className="breadcrumb"><Link to="/applications">Applications</Link> / {application.companyName}</p>
          <h1>{application.jobTitle}</h1>
          <p className="muted">
            {application.companyName} <StatusBadge status={application.status} />
          </p>
        </div>
        <div className="header-actions">
          <Link to={`/applications/${id}/edit`} className="btn btn-secondary">Edit</Link>
          <button type="button" className="btn btn-danger-outline" onClick={() => setConfirm({ kind: 'application' })}>
            Delete
          </button>
        </div>
      </div>

      <section className="card" aria-labelledby="details-heading">
        <h2 id="details-heading">Details</h2>
        <dl className="detail-grid">
          <DetailItem label="Job type">{jobTypeLabel(application.jobType)}</DetailItem>
          <DetailItem label="Applied on">{formatDate(application.applicationDate)}</DetailItem>
          <DetailItem label="Location">{application.location}</DetailItem>
          <DetailItem label="Salary range">{application.salaryRange}</DetailItem>
          <DetailItem label="Recruiter">{application.recruiterName}</DetailItem>
          <DetailItem label="Recruiter email">
            {application.recruiterEmail && <a href={`mailto:${application.recruiterEmail}`}>{application.recruiterEmail}</a>}
          </DetailItem>
          <DetailItem label="Job posting">
            {application.jobUrl && (
              <a href={application.jobUrl} target="_blank" rel="noopener noreferrer" className="break">
                {application.jobUrl}
              </a>
            )}
          </DetailItem>
          <DetailItem label="Last updated">{formatInstant(application.updatedAt)}</DetailItem>
        </dl>
        {application.jobDescription && (
          <>
            <h3>Job description</h3>
            <p className="pre-wrap">{application.jobDescription}</p>
          </>
        )}
        {application.notes && (
          <>
            <h3>Additional notes</h3>
            <p className="pre-wrap">{application.notes}</p>
          </>
        )}
      </section>

      <section className="card" aria-labelledby="interviews-heading">
        <div className="card-header">
          <h2 id="interviews-heading">Interviews</h2>
          <button type="button" className="btn btn-primary btn-sm" onClick={() => setInterviewDialog({})}>
            Schedule interview
          </button>
        </div>
        {interviews.length === 0 ? (
          <p className="muted">No interviews scheduled for this application.</p>
        ) : (
          <InterviewList
            interviews={interviews}
            showCompany={false}
            onEdit={(interview) => setInterviewDialog({ interview })}
            onDelete={(interview) => setConfirm({ kind: 'interview', item: interview })}
          />
        )}
      </section>

      <section className="card" aria-labelledby="notes-heading">
        <div className="card-header">
          <h2 id="notes-heading">Notes</h2>
          <button type="button" className="btn btn-primary btn-sm" onClick={() => setNoteDialog({})}>
            Add note
          </button>
        </div>
        {notes.length === 0 ? (
          <p className="muted">No notes yet. Record recruiter conversations, feedback and reminders here.</p>
        ) : (
          <ul className="note-list">
            {notes.map((note) => (
              <li key={note.id} className="note-item">
                <div className="note-head">
                  <span className="badge badge-note">{noteCategoryLabel(note.category)}</span>
                  <span className="muted small">{formatInstant(note.createdAt)}</span>
                </div>
                <p className="pre-wrap">{note.content}</p>
                <div className="row-actions">
                  <button type="button" className="btn btn-sm btn-secondary" onClick={() => setNoteDialog({ note })}>
                    Edit note
                  </button>
                  <button type="button" className="btn btn-sm btn-danger-outline"
                    onClick={() => setConfirm({ kind: 'note', item: note })}>
                    Delete note
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      {interviewDialog && (
        <Modal title={interviewDialog.interview ? 'Edit interview' : 'Schedule interview'} onClose={closeDialogs} wide>
          <InterviewForm initialValues={interviewDialog.interview} onSubmit={saveInterview} onCancel={closeDialogs}
            submitting={saving} serverErrors={formErrors} />
        </Modal>
      )}
      {noteDialog && (
        <Modal title={noteDialog.note ? 'Edit note' : 'Add note'} onClose={closeDialogs}>
          <NoteForm initialValues={noteDialog.note} onSubmit={saveNote} onCancel={closeDialogs}
            submitting={saving} serverErrors={formErrors} />
        </Modal>
      )}
      {confirm && (
        <ConfirmDialog
          title={`Delete ${confirm.kind}?`}
          message={confirmText[confirm.kind]}
          onConfirm={runDelete}
          onCancel={closeDialogs}
          busy={saving}
        />
      )}
    </>
  );
}

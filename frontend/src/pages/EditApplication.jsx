import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ApplicationForm from '../components/ApplicationForm';
import ErrorMessage from '../components/ErrorMessage';
import Spinner from '../components/Spinner';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { getErrorMessage, getFieldErrors } from '../services/api';
import { getApplication, updateApplication } from '../services/applicationService';

export default function EditApplication() {
  useDocumentTitle('Edit application');
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getApplication(id)
      .then((data) => !cancelled && setApplication(data))
      .catch((err) => !cancelled && setLoadError(getErrorMessage(err, 'Could not load the application.')))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [id]);

  const handleSubmit = async (body) => {
    setSubmitting(true);
    setError('');
    setFieldErrors({});
    try {
      await updateApplication(id, body);
      toast.success('Application updated');
      navigate(`/applications/${id}`);
    } catch (err) {
      setFieldErrors(getFieldErrors(err));
      setError(getErrorMessage(err, 'Could not save the application.'));
      setSubmitting(false);
    }
  };

  if (loading) return <Spinner label="Loading application…" />;
  if (loadError) {
    return (
      <>
        <ErrorMessage message={loadError} />
        <Link to="/applications" className="btn btn-secondary">Back to applications</Link>
      </>
    );
  }

  return (
    <>
      <div className="page-header">
        <div>
          <p className="breadcrumb">
            <Link to="/applications">Applications</Link> / <Link to={`/applications/${id}`}>{application.companyName}</Link> / Edit
          </p>
          <h1>Edit application</h1>
        </div>
      </div>
      <ErrorMessage message={error} />
      <ApplicationForm initialValues={application} onSubmit={handleSubmit} submitLabel="Save changes"
        submitting={submitting} serverErrors={fieldErrors} cancelTo={`/applications/${id}`} />
    </>
  );
}

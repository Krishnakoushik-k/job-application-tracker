import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import ApplicationForm from '../components/ApplicationForm';
import ErrorMessage from '../components/ErrorMessage';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { getErrorMessage, getFieldErrors } from '../services/api';
import { createApplication } from '../services/applicationService';

export default function AddApplication() {
  useDocumentTitle('Add application');
  const navigate = useNavigate();
  const toast = useToast();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const handleSubmit = async (body) => {
    setSubmitting(true);
    setError('');
    setFieldErrors({});
    try {
      const created = await createApplication(body);
      toast.success('Application added');
      navigate(`/applications/${created.id}`);
    } catch (err) {
      setFieldErrors(getFieldErrors(err));
      setError(getErrorMessage(err, 'Could not save the application.'));
      setSubmitting(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <p className="breadcrumb"><Link to="/applications">Applications</Link> / New</p>
          <h1>Add application</h1>
        </div>
      </div>
      <ErrorMessage message={error} />
      <ApplicationForm onSubmit={handleSubmit} submitLabel="Add application" submitting={submitting}
        serverErrors={fieldErrors} cancelTo="/applications" />
    </>
  );
}

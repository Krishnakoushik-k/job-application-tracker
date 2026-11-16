import { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { TextField } from '../components/FormFields';
import ErrorMessage from '../components/ErrorMessage';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { getErrorMessage } from '../services/api';
import { hasErrors, validateLogin } from '../utils/validators';

export default function Login() {
  useDocumentTitle('Log in');
  const { login, isAuthenticated } = useAuth();
  const location = useLocation();
  const [values, setValues] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // After a successful login isAuthenticated flips and this redirect sends the user
  // to the page they originally asked for (or the dashboard).
  if (isAuthenticated) return <Navigate to={location.state?.from || '/'} replace />;

  const handleChange = (event) => {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: undefined }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const validation = validateLogin(values);
    setErrors(validation);
    setApiError('');
    if (hasErrors(validation)) return;

    setSubmitting(true);
    try {
      await login(values.email.trim(), values.password);
    } catch (error) {
      setApiError(getErrorMessage(error, 'Login failed. Please try again.'));
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="brand brand-center">
          <span className="brand-mark" aria-hidden="true">JT</span>
          <span className="brand-name">JobTracker</span>
        </div>
        <h1>Welcome back</h1>
        <p className="muted">Log in to manage your job search.</p>
        <ErrorMessage message={apiError} />
        <form onSubmit={handleSubmit} noValidate>
          <TextField id="email" name="email" type="email" label="Email" autoComplete="email"
            value={values.email} onChange={handleChange} error={errors.email} />
          <TextField id="password" name="password" type="password" label="Password" autoComplete="current-password"
            value={values.password} onChange={handleChange} error={errors.password} />
          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Logging in…' : 'Log in'}
          </button>
        </form>
        <p className="auth-switch">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </div>
  );
}

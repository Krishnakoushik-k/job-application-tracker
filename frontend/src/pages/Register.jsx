import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { TextField } from '../components/FormFields';
import ErrorMessage from '../components/ErrorMessage';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { getErrorMessage, getFieldErrors } from '../services/api';
import { hasErrors, PASSWORD_RULE, validateRegister } from '../utils/validators';

export default function Register() {
  useDocumentTitle('Create account');
  const { register, isAuthenticated } = useAuth();
  const [values, setValues] = useState({ fullName: '', email: '', password: '', confirmPassword: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) return <Navigate to="/" replace />;

  const handleChange = (event) => {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: undefined }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const validation = validateRegister(values);
    setErrors(validation);
    setApiError('');
    if (hasErrors(validation)) return;

    setSubmitting(true);
    try {
      await register(values.fullName.trim(), values.email.trim(), values.password);
    } catch (error) {
      setErrors(getFieldErrors(error));
      setApiError(getErrorMessage(error, 'Registration failed. Please try again.'));
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
        <h1>Create your account</h1>
        <p className="muted">Keep every application, interview and note in one place.</p>
        <ErrorMessage message={apiError} />
        <form onSubmit={handleSubmit} noValidate>
          <TextField id="fullName" name="fullName" label="Full name" autoComplete="name"
            value={values.fullName} onChange={handleChange} error={errors.fullName} />
          <TextField id="email" name="email" type="email" label="Email" autoComplete="email"
            value={values.email} onChange={handleChange} error={errors.email} />
          <TextField id="password" name="password" type="password" label="Password" autoComplete="new-password"
            hint={PASSWORD_RULE} value={values.password} onChange={handleChange} error={errors.password} />
          <TextField id="confirmPassword" name="confirmPassword" type="password" label="Confirm password"
            autoComplete="new-password" value={values.confirmPassword} onChange={handleChange}
            error={errors.confirmPassword} />
          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>
        <p className="auth-switch">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </div>
    </div>
  );
}

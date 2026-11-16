import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { TextField } from '../components/FormFields';
import ErrorMessage from '../components/ErrorMessage';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { getErrorMessage, getFieldErrors } from '../services/api';
import { changePassword, updateProfile } from '../services/userService';
import { formatDate } from '../utils/format';
import { hasErrors, PASSWORD_RULE, validatePasswordChange, validateProfile } from '../utils/validators';

export default function Profile() {
  useDocumentTitle('Profile & settings');
  const { user, updateUser, logout } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [profile, setProfile] = useState({ fullName: user?.fullName ?? '', email: user?.email ?? '' });
  const [profileErrors, setProfileErrors] = useState({});
  const [profileApiError, setProfileApiError] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);

  const [password, setPassword] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [passwordErrors, setPasswordErrors] = useState({});
  const [passwordApiError, setPasswordApiError] = useState('');
  const [savingPassword, setSavingPassword] = useState(false);

  const handleProfileChange = (event) => {
    const { name, value } = event.target;
    setProfile((current) => ({ ...current, [name]: value }));
    setProfileErrors((current) => ({ ...current, [name]: undefined }));
  };

  const handlePasswordChange = (event) => {
    const { name, value } = event.target;
    setPassword((current) => ({ ...current, [name]: value }));
    setPasswordErrors((current) => ({ ...current, [name]: undefined }));
  };

  const submitProfile = async (event) => {
    event.preventDefault();
    const validation = validateProfile(profile);
    setProfileErrors(validation);
    setProfileApiError('');
    if (hasErrors(validation)) return;
    setSavingProfile(true);
    try {
      const updated = await updateProfile({ fullName: profile.fullName.trim(), email: profile.email.trim() });
      updateUser(updated);
      toast.success('Profile updated');
    } catch (err) {
      setProfileErrors(getFieldErrors(err));
      setProfileApiError(getErrorMessage(err, 'Could not update your profile.'));
    } finally {
      setSavingProfile(false);
    }
  };

  const submitPassword = async (event) => {
    event.preventDefault();
    const validation = validatePasswordChange(password);
    setPasswordErrors(validation);
    setPasswordApiError('');
    if (hasErrors(validation)) return;
    setSavingPassword(true);
    try {
      await changePassword(password.currentPassword, password.newPassword);
      setPassword({ currentPassword: '', newPassword: '', confirmPassword: '' });
      toast.success('Password changed');
    } catch (err) {
      setPasswordErrors(getFieldErrors(err));
      setPasswordApiError(getErrorMessage(err, 'Could not change your password.'));
    } finally {
      setSavingPassword(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Profile &amp; settings</h1>
          <p className="muted">Member since {formatDate(user?.createdAt)}</p>
        </div>
      </div>

      <div className="two-column">
        <form className="card" onSubmit={submitProfile} noValidate aria-labelledby="profile-heading">
          <h2 id="profile-heading">Profile</h2>
          <ErrorMessage message={profileApiError} />
          <TextField id="profileName" name="fullName" label="Full name" required value={profile.fullName}
            onChange={handleProfileChange} error={profileErrors.fullName} autoComplete="name" />
          <TextField id="profileEmail" name="email" type="email" label="Email" required value={profile.email}
            onChange={handleProfileChange} error={profileErrors.email} autoComplete="email" />
          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={savingProfile}>
              {savingProfile ? 'Saving…' : 'Save profile'}
            </button>
          </div>
        </form>

        <form className="card" onSubmit={submitPassword} noValidate aria-labelledby="password-heading">
          <h2 id="password-heading">Change password</h2>
          <ErrorMessage message={passwordApiError} />
          <TextField id="currentPassword" name="currentPassword" type="password" label="Current password" required
            value={password.currentPassword} onChange={handlePasswordChange} error={passwordErrors.currentPassword}
            autoComplete="current-password" />
          <TextField id="newPassword" name="newPassword" type="password" label="New password" required hint={PASSWORD_RULE}
            value={password.newPassword} onChange={handlePasswordChange} error={passwordErrors.newPassword}
            autoComplete="new-password" />
          <TextField id="confirmNewPassword" name="confirmPassword" type="password" label="Confirm new password" required
            value={password.confirmPassword} onChange={handlePasswordChange} error={passwordErrors.confirmPassword}
            autoComplete="new-password" />
          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={savingPassword}>
              {savingPassword ? 'Saving…' : 'Change password'}
            </button>
          </div>
        </form>
      </div>

      <div className="card">
        <h2>Session</h2>
        <p className="muted">Logging out removes your access token from this browser.</p>
        <button type="button" className="btn btn-secondary" onClick={handleLogout}>Log out</button>
      </div>
    </>
  );
}

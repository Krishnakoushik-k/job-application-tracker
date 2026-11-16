import { useState } from 'react';
import { SelectField, TextAreaField, TextField } from './FormFields';
import { INTERVIEW_TYPE_OPTIONS } from '../utils/constants';
import { blankToNull, toDateTimeInput } from '../utils/format';
import { hasErrors, validateInterview } from '../utils/validators';

/**
 * Create/edit form for one interview. Pass `applications` to show an application picker
 * (used on the Interviews page); omit it when the application is already known.
 */
export default function InterviewForm({ initialValues, applications, onSubmit, onCancel, submitting, serverErrors = {} }) {
  const [values, setValues] = useState({
    applicationId: applications ? String(initialValues?.applicationId ?? '') : undefined,
    roundName: initialValues?.roundName ?? '',
    interviewType: initialValues?.interviewType ?? 'ONLINE',
    scheduledAt: toDateTimeInput(initialValues?.scheduledAt),
    locationOrLink: initialValues?.locationOrLink ?? '',
    notes: initialValues?.notes ?? '',
  });
  const [clientErrors, setClientErrors] = useState({});
  const errors = { ...serverErrors, ...clientErrors };

  const handleChange = (event) => {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
    setClientErrors((current) => ({ ...current, [name]: undefined }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const validation = validateInterview(values);
    setClientErrors(validation);
    if (hasErrors(validation)) return;
    onSubmit(
      {
        roundName: values.roundName.trim(),
        interviewType: values.interviewType,
        scheduledAt: values.scheduledAt,
        locationOrLink: blankToNull(values.locationOrLink),
        notes: blankToNull(values.notes),
      },
      values.applicationId ? Number(values.applicationId) : undefined,
    );
  };

  const applicationOptions = (applications ?? []).map((application) => ({
    value: String(application.id),
    label: `${application.companyName} — ${application.jobTitle}`,
  }));

  return (
    <form onSubmit={handleSubmit} noValidate>
      {applications && (
        <SelectField id="applicationId" name="applicationId" label="Application" required placeholder="Select an application"
          options={applicationOptions} value={values.applicationId} onChange={handleChange} error={errors.applicationId}
          disabled={Boolean(initialValues?.id)} />
      )}
      <div className="form-grid">
        <TextField id="roundName" name="roundName" label="Round" required placeholder="e.g. Technical round 1"
          value={values.roundName} onChange={handleChange} error={errors.roundName} maxLength={100} />
        <SelectField id="interviewType" name="interviewType" label="Type" required options={INTERVIEW_TYPE_OPTIONS}
          value={values.interviewType} onChange={handleChange} error={errors.interviewType} />
        <TextField id="scheduledAt" name="scheduledAt" type="datetime-local" label="Date and time" required
          value={values.scheduledAt} onChange={handleChange} error={errors.scheduledAt} />
        <TextField id="locationOrLink" name="locationOrLink" label="Location or meeting link"
          value={values.locationOrLink} onChange={handleChange} error={errors.locationOrLink} maxLength={500} />
      </div>
      <TextAreaField id="interviewNotes" name="notes" label="Notes" rows={3} value={values.notes}
        onChange={handleChange} error={errors.notes} maxLength={5000} />
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving…' : initialValues?.id ? 'Save changes' : 'Schedule interview'}
        </button>
      </div>
    </form>
  );
}

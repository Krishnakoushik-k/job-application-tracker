import { useState } from 'react';
import { Link } from 'react-router-dom';
import { SelectField, TextAreaField, TextField } from './FormFields';
import { JOB_TYPE_OPTIONS, STATUS_OPTIONS } from '../utils/constants';
import { blankToNull, todayIso } from '../utils/format';
import { hasErrors, validateApplication } from '../utils/validators';

export const EMPTY_APPLICATION = {
  companyName: '',
  jobTitle: '',
  jobDescription: '',
  location: '',
  jobType: 'FULL_TIME',
  applicationDate: todayIso(),
  status: 'APPLIED',
  jobUrl: '',
  salaryRange: '',
  recruiterName: '',
  recruiterEmail: '',
  notes: '',
};

/** Turns API data (nulls) into form values (strings). */
export function toFormValues(application) {
  if (!application) return { ...EMPTY_APPLICATION, applicationDate: todayIso() };
  return Object.fromEntries(
    Object.keys(EMPTY_APPLICATION).map((key) => [key, application[key] ?? '']),
  );
}

/** Turns form values into the exact JSON the backend's ApplicationRequest expects. */
export function toRequestBody(values) {
  return {
    companyName: values.companyName.trim(),
    jobTitle: values.jobTitle.trim(),
    jobDescription: blankToNull(values.jobDescription),
    location: blankToNull(values.location),
    jobType: values.jobType,
    applicationDate: values.applicationDate,
    status: values.status,
    jobUrl: blankToNull(values.jobUrl),
    salaryRange: blankToNull(values.salaryRange),
    recruiterName: blankToNull(values.recruiterName),
    recruiterEmail: blankToNull(values.recruiterEmail),
    notes: blankToNull(values.notes),
  };
}

export default function ApplicationForm({ initialValues, onSubmit, submitLabel, submitting, serverErrors = {}, cancelTo }) {
  const [values, setValues] = useState(() => toFormValues(initialValues));
  const [clientErrors, setClientErrors] = useState({});
  const errors = { ...serverErrors, ...clientErrors };

  const handleChange = (event) => {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
    setClientErrors((current) => ({ ...current, [name]: undefined }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const validation = validateApplication(values);
    setClientErrors(validation);
    if (!hasErrors(validation)) onSubmit(toRequestBody(values));
  };

  return (
    <form className="card form-card" onSubmit={handleSubmit} noValidate>
      <div className="form-grid">
        <TextField id="companyName" name="companyName" label="Company name" required value={values.companyName}
          onChange={handleChange} error={errors.companyName} maxLength={150} />
        <TextField id="jobTitle" name="jobTitle" label="Job title" required value={values.jobTitle}
          onChange={handleChange} error={errors.jobTitle} maxLength={150} />
        <SelectField id="jobType" name="jobType" label="Job type" required options={JOB_TYPE_OPTIONS}
          value={values.jobType} onChange={handleChange} error={errors.jobType} />
        <SelectField id="status" name="status" label="Status" required options={STATUS_OPTIONS}
          value={values.status} onChange={handleChange} error={errors.status} />
        <TextField id="applicationDate" name="applicationDate" type="date" label="Application date" required
          value={values.applicationDate} onChange={handleChange} error={errors.applicationDate} />
        <TextField id="location" name="location" label="Location" value={values.location}
          onChange={handleChange} error={errors.location} maxLength={150} placeholder="City, country or Remote" />
        <TextField id="jobUrl" name="jobUrl" type="url" label="Job posting URL" value={values.jobUrl}
          onChange={handleChange} error={errors.jobUrl} maxLength={500} placeholder="https://" />
        <TextField id="salaryRange" name="salaryRange" label="Salary range" value={values.salaryRange}
          onChange={handleChange} error={errors.salaryRange} maxLength={100} placeholder="e.g. INR 10-14 LPA" />
        <TextField id="recruiterName" name="recruiterName" label="Recruiter name" value={values.recruiterName}
          onChange={handleChange} error={errors.recruiterName} maxLength={100} />
        <TextField id="recruiterEmail" name="recruiterEmail" type="email" label="Recruiter email" value={values.recruiterEmail}
          onChange={handleChange} error={errors.recruiterEmail} maxLength={255} />
      </div>
      <TextAreaField id="jobDescription" name="jobDescription" label="Job description" rows={5}
        value={values.jobDescription} onChange={handleChange} error={errors.jobDescription} maxLength={10000} />
      <TextAreaField id="notes" name="notes" label="Additional notes" rows={3} value={values.notes}
        onChange={handleChange} error={errors.notes} maxLength={5000} />

      <div className="form-actions">
        {cancelTo && (
          <Link to={cancelTo} className="btn btn-secondary">
            Cancel
          </Link>
        )}
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  );
}

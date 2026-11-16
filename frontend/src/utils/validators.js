// Client-side validation mirrors the backend rules so users get instant feedback.
// The backend remains the source of truth and its errors are displayed too.

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/;
export const PASSWORD_RULE = 'Password must be 8-72 characters and contain at least one letter and one digit';

const isBlank = (value) => value === undefined || value === null || String(value).trim() === '';

export function validateLogin({ email, password }) {
  const errors = {};
  if (isBlank(email)) errors.email = 'Email is required';
  else if (!EMAIL_PATTERN.test(email.trim())) errors.email = 'Enter a valid email address';
  if (isBlank(password)) errors.password = 'Password is required';
  return errors;
}

export function validateRegister({ fullName, email, password, confirmPassword }) {
  const errors = {};
  if (isBlank(fullName)) errors.fullName = 'Full name is required';
  else if (fullName.trim().length > 100) errors.fullName = 'Full name must be at most 100 characters';
  if (isBlank(email)) errors.email = 'Email is required';
  else if (!EMAIL_PATTERN.test(email.trim())) errors.email = 'Enter a valid email address';
  if (isBlank(password)) errors.password = 'Password is required';
  else if (!PASSWORD_PATTERN.test(password)) errors.password = PASSWORD_RULE;
  if (confirmPassword !== undefined && password !== confirmPassword) {
    errors.confirmPassword = 'Passwords do not match';
  }
  return errors;
}

export function validateProfile({ fullName, email }) {
  const errors = {};
  if (isBlank(fullName)) errors.fullName = 'Full name is required';
  if (isBlank(email)) errors.email = 'Email is required';
  else if (!EMAIL_PATTERN.test(email.trim())) errors.email = 'Enter a valid email address';
  return errors;
}

export function validatePasswordChange({ currentPassword, newPassword, confirmPassword }) {
  const errors = {};
  if (isBlank(currentPassword)) errors.currentPassword = 'Current password is required';
  if (isBlank(newPassword)) errors.newPassword = 'New password is required';
  else if (!PASSWORD_PATTERN.test(newPassword)) errors.newPassword = PASSWORD_RULE;
  if (newPassword !== confirmPassword) errors.confirmPassword = 'Passwords do not match';
  return errors;
}

export function validateApplication(values) {
  const errors = {};
  if (isBlank(values.companyName)) errors.companyName = 'Company name is required';
  else if (values.companyName.length > 150) errors.companyName = 'Company name must be at most 150 characters';
  if (isBlank(values.jobTitle)) errors.jobTitle = 'Job title is required';
  else if (values.jobTitle.length > 150) errors.jobTitle = 'Job title must be at most 150 characters';
  if (isBlank(values.jobType)) errors.jobType = 'Job type is required';
  if (isBlank(values.applicationDate)) errors.applicationDate = 'Application date is required';
  if (isBlank(values.status)) errors.status = 'Status is required';
  if (!isBlank(values.jobUrl) && !/^https?:\/\/\S+$/.test(values.jobUrl.trim())) {
    errors.jobUrl = 'Job URL must start with http:// or https://';
  }
  if (!isBlank(values.recruiterEmail) && !EMAIL_PATTERN.test(values.recruiterEmail.trim())) {
    errors.recruiterEmail = 'Recruiter email must be a valid email address';
  }
  if (values.salaryRange && values.salaryRange.length > 100) errors.salaryRange = 'Salary range must be at most 100 characters';
  if (values.notes && values.notes.length > 5000) errors.notes = 'Notes must be at most 5000 characters';
  return errors;
}

export function validateInterview(values) {
  const errors = {};
  if (values.applicationId !== undefined && isBlank(values.applicationId)) {
    errors.applicationId = 'Choose an application';
  }
  if (isBlank(values.roundName)) errors.roundName = 'Round name is required';
  else if (values.roundName.length > 100) errors.roundName = 'Round name must be at most 100 characters';
  if (isBlank(values.interviewType)) errors.interviewType = 'Interview type is required';
  if (isBlank(values.scheduledAt)) errors.scheduledAt = 'Date and time is required';
  return errors;
}

export function validateNote({ category, content }) {
  const errors = {};
  if (isBlank(category)) errors.category = 'Category is required';
  if (isBlank(content)) errors.content = 'Note content is required';
  else if (content.length > 5000) errors.content = 'Note must be at most 5000 characters';
  return errors;
}

export const hasErrors = (errors) => Object.keys(errors).length > 0;

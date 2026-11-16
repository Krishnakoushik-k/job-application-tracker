import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ApplicationForm from '../components/ApplicationForm';
import { renderWithProviders } from '../test/utils';

const renderForm = (props = {}) => {
  const onSubmit = vi.fn();
  renderWithProviders(<ApplicationForm onSubmit={onSubmit} submitLabel="Save application" submitting={false} {...props} />);
  return onSubmit;
};

describe('ApplicationForm', () => {
  it('requires company name and job title', async () => {
    const user = userEvent.setup();
    const onSubmit = renderForm();

    await user.click(screen.getByRole('button', { name: 'Save application' }));

    expect(screen.getByText('Company name is required')).toBeInTheDocument();
    expect(screen.getByText('Job title is required')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits the exact request body the API expects, sending blank optional fields as null', async () => {
    const user = userEvent.setup();
    const onSubmit = renderForm();

    await user.type(screen.getByLabelText('Company name'), '  Globex Technologies ');
    await user.type(screen.getByLabelText('Job title'), 'Full Stack Developer');
    fireEvent.change(screen.getByLabelText('Application date'), { target: { value: '2026-11-10' } });
    await user.selectOptions(screen.getByLabelText('Status'), 'INTERVIEW');
    await user.selectOptions(screen.getByLabelText('Job type'), 'INTERNSHIP');
    await user.click(screen.getByRole('button', { name: 'Save application' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({
      companyName: 'Globex Technologies',
      jobTitle: 'Full Stack Developer',
      jobDescription: null,
      location: null,
      jobType: 'INTERNSHIP',
      applicationDate: '2026-11-10',
      status: 'INTERVIEW',
      jobUrl: null,
      salaryRange: null,
      recruiterName: null,
      recruiterEmail: null,
      notes: null,
    });
  });

  it('validates the job URL and recruiter email', async () => {
    const user = userEvent.setup();
    const onSubmit = renderForm();

    await user.type(screen.getByLabelText('Company name'), 'Acme');
    await user.type(screen.getByLabelText('Job title'), 'Engineer');
    await user.type(screen.getByLabelText('Job posting URL'), 'ftp://example.com');
    await user.type(screen.getByLabelText('Recruiter email'), 'nope');
    await user.click(screen.getByRole('button', { name: 'Save application' }));

    expect(screen.getByText('Job URL must start with http:// or https://')).toBeInTheDocument();
    expect(screen.getByText('Recruiter email must be a valid email address')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('pre-fills the form when editing and converts nulls to empty fields', () => {
    renderForm({
      initialValues: {
        companyName: 'Initech',
        jobTitle: 'Backend Engineer',
        jobType: 'PART_TIME',
        status: 'OFFER',
        applicationDate: '2026-10-01',
        location: null,
        salaryRange: 'INR 10 LPA',
      },
    });

    expect(screen.getByLabelText('Company name')).toHaveValue('Initech');
    expect(screen.getByLabelText('Status')).toHaveValue('OFFER');
    expect(screen.getByLabelText('Job type')).toHaveValue('PART_TIME');
    expect(screen.getByLabelText('Location')).toHaveValue('');
    expect(screen.getByLabelText('Salary range')).toHaveValue('INR 10 LPA');
  });

  it('shows validation messages returned by the server next to the field', () => {
    renderForm({ serverErrors: { companyName: 'Company name must be at most 150 characters' } });
    expect(screen.getByText('Company name must be at most 150 characters')).toBeInTheDocument();
  });

  it('disables the submit button while saving', () => {
    renderForm({ submitting: true });
    expect(screen.getByRole('button', { name: 'Saving…' })).toBeDisabled();
  });
});

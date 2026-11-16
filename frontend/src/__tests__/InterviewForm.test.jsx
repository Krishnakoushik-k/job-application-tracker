import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import InterviewForm from '../components/InterviewForm';
import { renderWithProviders } from '../test/utils';

const applications = [
  { id: 5, companyName: 'Globex', jobTitle: 'Developer' },
  { id: 6, companyName: 'Initech', jobTitle: 'Analyst' },
];

describe('InterviewForm', () => {
  it('requires an application, a round and a date', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    renderWithProviders(<InterviewForm applications={applications} onSubmit={onSubmit} onCancel={() => {}} />);

    await user.click(screen.getByRole('button', { name: 'Schedule interview' }));

    expect(screen.getByText('Choose an application')).toBeInTheDocument();
    expect(screen.getByText('Round name is required')).toBeInTheDocument();
    expect(screen.getByText('Date and time is required')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits the request body and the chosen application id', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    renderWithProviders(<InterviewForm applications={applications} onSubmit={onSubmit} onCancel={() => {}} />);

    await user.selectOptions(screen.getByLabelText('Application'), '5');
    await user.type(screen.getByLabelText('Round'), 'Technical round 1');
    fireEvent.change(screen.getByLabelText('Date and time'), { target: { value: '2026-11-20T14:30' } });
    await user.click(screen.getByRole('button', { name: 'Schedule interview' }));

    expect(onSubmit).toHaveBeenCalledWith(
      {
        roundName: 'Technical round 1',
        interviewType: 'ONLINE',
        scheduledAt: '2026-11-20T14:30',
        locationOrLink: null,
        notes: null,
      },
      5,
    );
  });

  it('pre-fills values when editing an interview', () => {
    renderWithProviders(
      <InterviewForm
        initialValues={{ id: 9, roundName: 'HR round', interviewType: 'PHONE', scheduledAt: '2026-12-01T10:00:00', locationOrLink: 'Call' }}
        onSubmit={() => {}}
        onCancel={() => {}}
      />,
    );

    expect(screen.getByLabelText('Round')).toHaveValue('HR round');
    expect(screen.getByLabelText('Type')).toHaveValue('PHONE');
    expect(screen.getByLabelText('Date and time')).toHaveValue('2026-12-01T10:00');
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeInTheDocument();
  });
});

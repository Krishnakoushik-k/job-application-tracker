import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Applications from '../pages/Applications';
import * as applicationService from '../services/applicationService';
import { renderWithProviders } from '../test/utils';

vi.mock('../services/applicationService');

const rows = [
  { id: 1, companyName: 'Globex Technologies', jobTitle: 'Full Stack Developer', location: 'Bengaluru', jobType: 'FULL_TIME', applicationDate: '2026-11-10', status: 'APPLIED' },
  { id: 2, companyName: 'Initech', jobTitle: 'Backend Engineer', location: null, jobType: 'INTERNSHIP', applicationDate: '2026-10-02', status: 'OFFER' },
];

const page = (content, overrides = {}) => ({
  content,
  page: 0,
  size: 10,
  totalElements: content.length,
  totalPages: content.length ? 1 : 0,
  first: true,
  last: true,
  ...overrides,
});

describe('Applications page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    applicationService.searchApplications.mockResolvedValue(page(rows));
  });

  it('renders the applications returned by the API', async () => {
    renderWithProviders(<Applications />);

    expect(await screen.findByText('Globex Technologies')).toBeInTheDocument();
    expect(screen.getByText('Initech')).toBeInTheDocument();
    expect(screen.getByText('Full Stack Developer')).toBeInTheDocument();
    expect(screen.getByText('10 Nov 2026')).toBeInTheDocument();
    expect(screen.getByText('Showing 1–2 of 2')).toBeInTheDocument();
    expect(applicationService.searchApplications).toHaveBeenCalledWith(
      expect.objectContaining({ sort: 'newest', page: 0, size: 10 }),
    );
  });

  it('shows a loading indicator first', () => {
    applicationService.searchApplications.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<Applications />);
    expect(screen.getByRole('status')).toHaveTextContent('Loading applications');
  });

  it('shows an empty state when the user has no applications', async () => {
    applicationService.searchApplications.mockResolvedValue(page([]));
    renderWithProviders(<Applications />);
    expect(await screen.findByText('No applications yet')).toBeInTheDocument();
  });

  it('shows an error message with a retry button when loading fails', async () => {
    applicationService.searchApplications.mockRejectedValueOnce({ response: { data: { message: 'Server exploded' } } });
    const user = userEvent.setup();
    renderWithProviders(<Applications />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Server exploded');

    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByText('Globex Technologies')).toBeInTheDocument();
  });

  it('filters by status through the API', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Applications />);
    await screen.findByText('Globex Technologies');

    applicationService.searchApplications.mockResolvedValue(page([rows[1]]));
    await user.selectOptions(screen.getByLabelText('Status'), 'OFFER');

    await waitFor(() =>
      expect(applicationService.searchApplications).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'OFFER', page: 0 }),
      ),
    );
    await waitFor(() => expect(screen.queryByText('Globex Technologies')).not.toBeInTheDocument());
    expect(screen.getByText('Initech')).toBeInTheDocument();
  });

  it('filters by job type and changes the sort order', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Applications />);
    await screen.findByText('Globex Technologies');

    await user.selectOptions(screen.getByLabelText('Job type'), 'INTERNSHIP');
    await waitFor(() =>
      expect(applicationService.searchApplications).toHaveBeenLastCalledWith(expect.objectContaining({ jobType: 'INTERNSHIP' })),
    );

    await user.selectOptions(screen.getByLabelText('Sort by'), 'oldest');
    await waitFor(() =>
      expect(applicationService.searchApplications).toHaveBeenLastCalledWith(
        expect.objectContaining({ jobType: 'INTERNSHIP', sort: 'oldest' }),
      ),
    );
  });

  it('searches after the user stops typing (debounced)', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Applications />);
    await screen.findByText('Globex Technologies');

    await user.type(screen.getByLabelText('Search company or job title'), 'glo');

    await waitFor(
      () =>
        expect(applicationService.searchApplications).toHaveBeenLastCalledWith(
          expect.objectContaining({ search: 'glo', page: 0 }),
        ),
      { timeout: 2000 },
    );
  });

  it('shows a "no matches" state and lets the user clear the filters', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Applications />);
    await screen.findByText('Globex Technologies');

    applicationService.searchApplications.mockResolvedValue(page([]));
    await user.selectOptions(screen.getByLabelText('Status'), 'WITHDRAWN');
    expect(await screen.findByText('No applications match your filters')).toBeInTheDocument();

    applicationService.searchApplications.mockResolvedValue(page(rows));
    await user.click(screen.getAllByRole('button', { name: 'Clear filters' })[0]);

    expect(await screen.findByText('Globex Technologies')).toBeInTheDocument();
    expect(screen.getByLabelText('Status')).toHaveValue('');
  });

  it('requests the next page when Next is clicked', async () => {
    applicationService.searchApplications.mockResolvedValue(page(rows, { totalElements: 25, totalPages: 3 }));
    const user = userEvent.setup();
    renderWithProviders(<Applications />);
    await screen.findByText('Globex Technologies');

    await user.click(screen.getByRole('button', { name: 'Next' }));

    await waitFor(() =>
      expect(applicationService.searchApplications).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 })),
    );
  });

  it('asks for confirmation before deleting and only then calls the API', async () => {
    applicationService.deleteApplication.mockResolvedValue({});
    const user = userEvent.setup();
    renderWithProviders(<Applications />);
    await screen.findByText('Globex Technologies');

    await user.click(screen.getByRole('button', { name: 'Delete Globex Technologies Full Stack Developer' }));
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText(/permanently deletes/i)).toBeInTheDocument();
    expect(applicationService.deleteApplication).not.toHaveBeenCalled();

    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(applicationService.deleteApplication).toHaveBeenCalledWith(1));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('does not delete when the user cancels', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Applications />);
    await screen.findByText('Globex Technologies');

    await user.click(screen.getByRole('button', { name: 'Delete Initech Backend Engineer' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }));

    expect(applicationService.deleteApplication).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});

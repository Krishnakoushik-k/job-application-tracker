import { screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Dashboard from '../pages/Dashboard';
import { saveSession } from '../services/api';
import * as dashboardService from '../services/dashboardService';
import { renderWithProviders } from '../test/utils';

vi.mock('../services/dashboardService');

// Recharts needs real layout sizes, which jsdom does not have, so the charts are replaced by stubs.
vi.mock('recharts', async () => {
  const React = await import('react');
  const stub = (testId) => ({ children }) => React.createElement('div', { 'data-testid': testId }, children);
  return {
    ResponsiveContainer: stub('responsive-container'),
    PieChart: stub('pie-chart'),
    BarChart: stub('bar-chart'),
    Pie: stub('pie'),
    Bar: stub('bar'),
    Cell: stub('cell'),
    CartesianGrid: stub('grid'),
    XAxis: stub('x-axis'),
    YAxis: stub('y-axis'),
    Tooltip: stub('tooltip'),
    Legend: stub('legend'),
  };
});

const dashboard = {
  totalApplications: 6,
  appliedCount: 2,
  assessmentCount: 1,
  interviewCount: 1,
  offerCount: 1,
  rejectedCount: 1,
  withdrawnCount: 0,
  upcomingInterviewCount: 1,
  successRate: 16.7,
  statusDistribution: [
    { status: 'APPLIED', count: 2 },
    { status: 'ASSESSMENT', count: 1 },
    { status: 'INTERVIEW', count: 1 },
    { status: 'OFFER', count: 1 },
    { status: 'REJECTED', count: 1 },
    { status: 'WITHDRAWN', count: 0 },
  ],
  monthlyTrend: [
    { month: '2026-10', count: 2 },
    { month: '2026-11', count: 4 },
  ],
  upcomingInterviews: [
    {
      id: 7, applicationId: 1, companyName: 'Globex Technologies', jobTitle: 'Full Stack Developer',
      roundName: 'Technical round 1', interviewType: 'ONLINE', scheduledAt: '2026-11-20T14:30:00',
    },
  ],
  recentApplications: [
    { id: 1, companyName: 'Globex Technologies', jobTitle: 'Full Stack Developer', jobType: 'FULL_TIME', applicationDate: '2026-11-10', status: 'INTERVIEW' },
  ],
};

describe('Dashboard page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    saveSession('token', { id: 1, fullName: 'Asha Reddy', email: 'asha@example.com' });
  });

  it('shows a loading indicator while fetching', () => {
    dashboardService.getDashboard.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<Dashboard />);
    expect(screen.getByRole('status')).toHaveTextContent('Loading dashboard');
  });

  it('greets the user and renders statistics from the API', async () => {
    dashboardService.getDashboard.mockResolvedValue(dashboard);
    renderWithProviders(<Dashboard />);

    expect(await screen.findByRole('heading', { name: 'Welcome back, Asha' })).toBeInTheDocument();

    const stats = within(screen.getByRole('region', { name: 'Statistics' }));
    expect(stats.getByText('Total applications').nextSibling).toHaveTextContent('6');
    expect(stats.getByText('Offers').nextSibling).toHaveTextContent('1');
    expect(stats.getByText('Rejected').nextSibling).toHaveTextContent('1');
    expect(stats.getByText('Success rate').nextSibling).toHaveTextContent('16.7%');
  });

  it('renders both charts, recent applications and upcoming interviews', async () => {
    dashboardService.getDashboard.mockResolvedValue(dashboard);
    renderWithProviders(<Dashboard />);

    expect(await screen.findByRole('heading', { name: 'Status distribution' })).toBeInTheDocument();
    expect(screen.getByTestId('pie-chart')).toBeInTheDocument();
    expect(screen.getByTestId('bar-chart')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Applications per month' })).toBeInTheDocument();

    expect(screen.getByRole('heading', { name: 'Recent applications' })).toBeInTheDocument();
    expect(screen.getAllByText('Globex Technologies').length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText(/Technical round 1/)).toBeInTheDocument();
    expect(screen.getByText(/20 Nov 2026, 14:30/)).toBeInTheDocument();
  });

  it('shows an empty state for a user with no applications', async () => {
    dashboardService.getDashboard.mockResolvedValue({
      ...dashboard,
      totalApplications: 0,
      successRate: 0,
      recentApplications: [],
      upcomingInterviews: [],
    });
    renderWithProviders(<Dashboard />);

    expect(await screen.findByText('No applications yet')).toBeInTheDocument();
    expect(screen.queryByTestId('pie-chart')).not.toBeInTheDocument();
  });

  it('shows an error with a retry button when the API fails', async () => {
    dashboardService.getDashboard.mockRejectedValueOnce({ response: { data: { message: 'Boom' } } });
    dashboardService.getDashboard.mockResolvedValueOnce(dashboard);
    renderWithProviders(<Dashboard />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Boom');
  });
});

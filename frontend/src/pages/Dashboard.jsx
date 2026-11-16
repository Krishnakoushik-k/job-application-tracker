import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useAuth } from '../context/AuthContext';
import EmptyState from '../components/EmptyState';
import ErrorMessage from '../components/ErrorMessage';
import InterviewList from '../components/InterviewList';
import Spinner from '../components/Spinner';
import StatCard from '../components/StatCard';
import StatusBadge from '../components/StatusBadge';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { getDashboard } from '../services/dashboardService';
import { getErrorMessage } from '../services/api';
import { STATUS_COLORS, statusLabel } from '../utils/constants';
import { formatDate, formatMonth } from '../utils/format';

export default function Dashboard() {
  useDocumentTitle('Dashboard');
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setData(await getDashboard());
    } catch (err) {
      setError(getErrorMessage(err, 'Could not load the dashboard.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const firstName = user?.fullName?.split(' ')[0] ?? '';

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Welcome back, {firstName}</h1>
          <p className="muted">Here is where your job search stands today.</p>
        </div>
        <Link to="/applications/new" className="btn btn-primary">
          Add application
        </Link>
      </div>

      {loading && <Spinner label="Loading dashboard…" />}
      {!loading && error && <ErrorMessage message={error} onRetry={load} />}

      {!loading && data && data.totalApplications === 0 && (
        <EmptyState
          title="No applications yet"
          description="Add your first application and your statistics will appear here."
          action={
            <Link to="/applications/new" className="btn btn-primary">
              Add your first application
            </Link>
          }
        />
      )}

      {!loading && data && data.totalApplications > 0 && (
        <>
          <section aria-label="Statistics" className="stat-grid">
            <StatCard label="Total applications" value={data.totalApplications} />
            <StatCard label="Applied" value={data.appliedCount} />
            <StatCard label="Assessment" value={data.assessmentCount} />
            <StatCard label="Interviews" value={data.interviewCount} />
            <StatCard label="Offers" value={data.offerCount} tone="success" />
            <StatCard label="Rejected" value={data.rejectedCount} tone="danger" />
            <StatCard label="Upcoming interviews" value={data.upcomingInterviewCount} />
            <StatCard label="Success rate" value={`${data.successRate}%`} hint="Offers ÷ total applications" tone="success" />
          </section>

          <section className="chart-grid">
            <div className="card">
              <h2>Status distribution</h2>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={data.statusDistribution.filter((item) => item.count > 0).map((item) => ({ ...item, name: statusLabel(item.status) }))}
                      dataKey="count"
                      nameKey="name"
                      innerRadius={55}
                      outerRadius={95}
                      paddingAngle={2}
                    >
                      {data.statusDistribution
                        .filter((item) => item.count > 0)
                        .map((item) => (
                          <Cell key={item.status} fill={STATUS_COLORS[item.status]} />
                        ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>
            <div className="card">
              <h2>Applications per month</h2>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={data.monthlyTrend.map((item) => ({ ...item, label: formatMonth(item.month) }))}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" tick={{ fontSize: 12 }} interval="preserveStartEnd" />
                    <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                    <Tooltip />
                    <Bar dataKey="count" name="Applications" fill="#4f46e5" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </section>

          <section className="two-column">
            <div className="card">
              <div className="card-header">
                <h2>Recent applications</h2>
                <Link to="/applications">View all</Link>
              </div>
              {data.recentApplications.length === 0 ? (
                <p className="muted">Nothing here yet.</p>
              ) : (
                <ul className="simple-list">
                  {data.recentApplications.map((application) => (
                    <li key={application.id}>
                      <div>
                        <Link to={`/applications/${application.id}`} className="table-link">
                          {application.companyName}
                        </Link>
                        <p className="muted small">
                          {application.jobTitle} · {formatDate(application.applicationDate)}
                        </p>
                      </div>
                      <StatusBadge status={application.status} />
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <div className="card">
              <div className="card-header">
                <h2>Upcoming interviews</h2>
                <Link to="/interviews">Manage</Link>
              </div>
              {data.upcomingInterviews.length === 0 ? (
                <p className="muted">No upcoming interviews scheduled.</p>
              ) : (
                <InterviewList interviews={data.upcomingInterviews} />
              )}
            </div>
          </section>
        </>
      )}
    </>
  );
}

import { Link } from 'react-router-dom';
import StatusBadge from './StatusBadge';
import { jobTypeLabel } from '../utils/constants';
import { formatDate } from '../utils/format';

export default function ApplicationsTable({ applications, onDelete }) {
  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Company</th>
            <th scope="col">Role</th>
            <th scope="col">Type</th>
            <th scope="col">Applied</th>
            <th scope="col">Status</th>
            <th scope="col">
              <span className="sr-only">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {applications.map((application) => (
            <tr key={application.id}>
              <td data-label="Company">
                <Link to={`/applications/${application.id}`} className="table-link">
                  {application.companyName}
                </Link>
                {application.location && <div className="muted small">{application.location}</div>}
              </td>
              <td data-label="Role">{application.jobTitle}</td>
              <td data-label="Type">{jobTypeLabel(application.jobType)}</td>
              <td data-label="Applied">{formatDate(application.applicationDate)}</td>
              <td data-label="Status">
                <StatusBadge status={application.status} />
              </td>
              <td className="row-actions">
                <Link
                  to={`/applications/${application.id}/edit`}
                  className="btn btn-sm btn-secondary"
                  aria-label={`Edit ${application.companyName} ${application.jobTitle}`}
                >
                  Edit
                </Link>
                <button
                  type="button"
                  className="btn btn-sm btn-danger-outline"
                  aria-label={`Delete ${application.companyName} ${application.jobTitle}`}
                  onClick={() => onDelete(application)}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

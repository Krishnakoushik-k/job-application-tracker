import { Link } from 'react-router-dom';
import { interviewTypeLabel } from '../utils/constants';
import { formatDateTime } from '../utils/format';

/** Compact list of interviews. Pass onEdit/onDelete to show action buttons. */
export default function InterviewList({ interviews, onEdit, onDelete, showCompany = true }) {
  return (
    <ul className="interview-list">
      {interviews.map((interview) => (
        <li key={interview.id} className="interview-item">
          <div className="interview-main">
            <p className="interview-title">
              {interview.roundName}
              {showCompany && (
                <>
                  {' · '}
                  <Link to={`/applications/${interview.applicationId}`} className="table-link">
                    {interview.companyName}
                  </Link>
                </>
              )}
            </p>
            <p className="muted small">
              {formatDateTime(interview.scheduledAt)} · {interviewTypeLabel(interview.interviewType)}
              {showCompany && ` · ${interview.jobTitle}`}
            </p>
            {interview.locationOrLink && <p className="small break">{interview.locationOrLink}</p>}
            {interview.notes && <p className="small note-text">{interview.notes}</p>}
          </div>
          {(onEdit || onDelete) && (
            <div className="row-actions">
              {onEdit && (
                <button type="button" className="btn btn-sm btn-secondary" onClick={() => onEdit(interview)}
                  aria-label={`Edit ${interview.roundName} at ${interview.companyName}`}>
                  Edit
                </button>
              )}
              {onDelete && (
                <button type="button" className="btn btn-sm btn-danger-outline" onClick={() => onDelete(interview)}
                  aria-label={`Delete ${interview.roundName} at ${interview.companyName}`}>
                  Delete
                </button>
              )}
            </div>
          )}
        </li>
      ))}
    </ul>
  );
}

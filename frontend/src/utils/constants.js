export const STATUS_OPTIONS = [
  { value: 'APPLIED', label: 'Applied' },
  { value: 'ASSESSMENT', label: 'Assessment' },
  { value: 'INTERVIEW', label: 'Interview' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'WITHDRAWN', label: 'Withdrawn' },
];

export const JOB_TYPE_OPTIONS = [
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'PART_TIME', label: 'Part-time' },
  { value: 'INTERNSHIP', label: 'Internship' },
];

export const INTERVIEW_TYPE_OPTIONS = [
  { value: 'ONLINE', label: 'Online' },
  { value: 'PHONE', label: 'Phone' },
  { value: 'IN_PERSON', label: 'In person' },
];

export const NOTE_CATEGORY_OPTIONS = [
  { value: 'RECRUITER_CONVERSATION', label: 'Recruiter conversation' },
  { value: 'INTERVIEW_FEEDBACK', label: 'Interview feedback' },
  { value: 'FOLLOW_UP', label: 'Follow-up reminder' },
  { value: 'PREPARATION', label: 'Preparation' },
  { value: 'GENERAL', label: 'General' },
];

export const SORT_OPTIONS = [
  { value: 'newest', label: 'Newest first' },
  { value: 'oldest', label: 'Oldest first' },
];

export const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

// Colours chosen to stay distinguishable for colour-blind users and to match the badge CSS.
export const STATUS_COLORS = {
  APPLIED: '#4f46e5',
  ASSESSMENT: '#0891b2',
  INTERVIEW: '#d97706',
  OFFER: '#16a34a',
  REJECTED: '#dc2626',
  WITHDRAWN: '#6b7280',
};

function labelFrom(options, value) {
  const match = options.find((option) => option.value === value);
  return match ? match.label : value ?? '';
}

export const statusLabel = (value) => labelFrom(STATUS_OPTIONS, value);
export const jobTypeLabel = (value) => labelFrom(JOB_TYPE_OPTIONS, value);
export const interviewTypeLabel = (value) => labelFrom(INTERVIEW_TYPE_OPTIONS, value);
export const noteCategoryLabel = (value) => labelFrom(NOTE_CATEGORY_OPTIONS, value);

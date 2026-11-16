-- ============================================================================
-- Development sample data (OPTIONAL - never load this into production)
--
-- This script does not insert users, because a user needs a real BCrypt hash.
-- Register the demo account first (through the UI, Swagger UI or Postman):
--
--     email:    demo@example.com
--     password: any password you choose (min 8 chars, with a letter and a digit)
--
-- Then run this file. Every statement is INSERT ... SELECT against that user,
-- so if the demo user does not exist, nothing is inserted.
-- Dates are relative to "today", so the dashboard always looks current.
-- ============================================================================

USE job_tracker;

-- ---------------------------------------------------------------------------
-- Applications
-- ---------------------------------------------------------------------------
INSERT INTO job_applications
    (user_id, company_name, job_title, job_description, location, job_type, application_date, status, job_url, salary_range, recruiter_name, recruiter_email, notes)
SELECT u.id, 'Acme Software', 'Junior Java Developer',
       'Build REST APIs with Spring Boot and MySQL.', 'Hyderabad, India', 'FULL_TIME',
       DATE_SUB(CURDATE(), INTERVAL 45 DAY), 'REJECTED', 'https://example.com/jobs/acme-junior-java',
       'INR 6-8 LPA', 'Priya Sharma', 'priya@acme.example', 'Rejected after the technical round.'
FROM users u WHERE u.email = 'demo@example.com';

INSERT INTO job_applications
    (user_id, company_name, job_title, job_description, location, job_type, application_date, status, job_url, salary_range, recruiter_name, recruiter_email, notes)
SELECT u.id, 'Northwind Labs', 'Backend Engineer Intern',
       'Six-month internship on the payments platform.', 'Remote', 'INTERNSHIP',
       DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'OFFER', 'https://example.com/jobs/northwind-intern',
       'INR 30k / month', 'Rahul Verma', 'rahul@northwind.example', 'Offer received. Reply by the end of next week.'
FROM users u WHERE u.email = 'demo@example.com';

INSERT INTO job_applications
    (user_id, company_name, job_title, job_description, location, job_type, application_date, status, job_url, salary_range, recruiter_name, recruiter_email, notes)
SELECT u.id, 'Globex Technologies', 'Full Stack Developer',
       'React frontend and Spring Boot backend for an HR product.', 'Bengaluru, India', 'FULL_TIME',
       DATE_SUB(CURDATE(), INTERVAL 21 DAY), 'INTERVIEW', 'https://example.com/jobs/globex-fullstack',
       'INR 10-14 LPA', 'Anita Rao', 'anita@globex.example', NULL
FROM users u WHERE u.email = 'demo@example.com';

INSERT INTO job_applications
    (user_id, company_name, job_title, job_description, location, job_type, application_date, status, job_url, salary_range, recruiter_name, recruiter_email, notes)
SELECT u.id, 'Initech', 'Software Engineer',
       'Maintain internal tooling written in Java.', 'Pune, India', 'FULL_TIME',
       DATE_SUB(CURDATE(), INTERVAL 14 DAY), 'ASSESSMENT', 'https://example.com/jobs/initech-swe',
       NULL, NULL, NULL, 'Online coding assessment due this week.'
FROM users u WHERE u.email = 'demo@example.com';

INSERT INTO job_applications
    (user_id, company_name, job_title, job_description, location, job_type, application_date, status, job_url, salary_range, recruiter_name, recruiter_email, notes)
SELECT u.id, 'Umbrella Systems', 'Part-time QA Automation Engineer',
       'Write automated UI tests.', 'Chennai, India', 'PART_TIME',
       DATE_SUB(CURDATE(), INTERVAL 9 DAY), 'APPLIED', 'https://example.com/jobs/umbrella-qa',
       'INR 25k / month', NULL, NULL, NULL
FROM users u WHERE u.email = 'demo@example.com';

INSERT INTO job_applications
    (user_id, company_name, job_title, job_description, location, job_type, application_date, status, job_url, salary_range, recruiter_name, recruiter_email, notes)
SELECT u.id, 'Stark Cloud', 'Java Developer',
       'Microservices on AWS.', 'Hyderabad, India', 'FULL_TIME',
       DATE_SUB(CURDATE(), INTERVAL 6 DAY), 'APPLIED', 'https://example.com/jobs/stark-java',
       'INR 8-12 LPA', 'Kiran Patel', 'kiran@stark.example', NULL
FROM users u WHERE u.email = 'demo@example.com';

INSERT INTO job_applications
    (user_id, company_name, job_title, job_description, location, job_type, application_date, status, job_url, salary_range, recruiter_name, recruiter_email, notes)
SELECT u.id, 'Wayne Analytics', 'Data Platform Intern',
       'Help build data pipelines.', 'Remote', 'INTERNSHIP',
       DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'APPLIED', 'https://example.com/jobs/wayne-intern',
       NULL, NULL, NULL, NULL
FROM users u WHERE u.email = 'demo@example.com';

INSERT INTO job_applications
    (user_id, company_name, job_title, job_description, location, job_type, application_date, status, job_url, salary_range, recruiter_name, recruiter_email, notes)
SELECT u.id, 'Hooli', 'Backend Developer',
       'Withdrew after accepting another offer.', 'Mumbai, India', 'FULL_TIME',
       DATE_SUB(CURDATE(), INTERVAL 60 DAY), 'WITHDRAWN', 'https://example.com/jobs/hooli-backend',
       'INR 9-11 LPA', NULL, NULL, NULL
FROM users u WHERE u.email = 'demo@example.com';

-- ---------------------------------------------------------------------------
-- Interviews
-- ---------------------------------------------------------------------------
INSERT INTO interviews (application_id, round_name, interview_type, scheduled_at, location_or_link, notes)
SELECT a.id, 'Technical Round 1', 'ONLINE', DATE_ADD(NOW(), INTERVAL 3 DAY),
       'https://meet.example.com/globex-tech-1', 'Expect Java collections and Spring Boot questions.'
FROM job_applications a JOIN users u ON u.id = a.user_id
WHERE u.email = 'demo@example.com' AND a.company_name = 'Globex Technologies';

INSERT INTO interviews (application_id, round_name, interview_type, scheduled_at, location_or_link, notes)
SELECT a.id, 'HR Round', 'PHONE', DATE_ADD(NOW(), INTERVAL 6 DAY),
       'Recruiter will call', NULL
FROM job_applications a JOIN users u ON u.id = a.user_id
WHERE u.email = 'demo@example.com' AND a.company_name = 'Globex Technologies';

INSERT INTO interviews (application_id, round_name, interview_type, scheduled_at, location_or_link, notes)
SELECT a.id, 'Technical Round', 'IN_PERSON', DATE_SUB(NOW(), INTERVAL 40 DAY),
       'Acme office, Hitech City', 'Went well on Java, struggled with system design.'
FROM job_applications a JOIN users u ON u.id = a.user_id
WHERE u.email = 'demo@example.com' AND a.company_name = 'Acme Software';

INSERT INTO interviews (application_id, round_name, interview_type, scheduled_at, location_or_link, notes)
SELECT a.id, 'Team Match', 'ONLINE', DATE_SUB(NOW(), INTERVAL 25 DAY),
       'https://meet.example.com/northwind-team', 'Positive conversation with the manager.'
FROM job_applications a JOIN users u ON u.id = a.user_id
WHERE u.email = 'demo@example.com' AND a.company_name = 'Northwind Labs';

-- ---------------------------------------------------------------------------
-- Application notes
-- ---------------------------------------------------------------------------
INSERT INTO application_notes (application_id, category, content)
SELECT a.id, 'PREPARATION', 'Revise Spring Security filter chain, JPA lazy loading and REST status codes.'
FROM job_applications a JOIN users u ON u.id = a.user_id
WHERE u.email = 'demo@example.com' AND a.company_name = 'Globex Technologies';

INSERT INTO application_notes (application_id, category, content)
SELECT a.id, 'RECRUITER_CONVERSATION', 'Anita confirmed two technical rounds followed by HR.'
FROM job_applications a JOIN users u ON u.id = a.user_id
WHERE u.email = 'demo@example.com' AND a.company_name = 'Globex Technologies';

INSERT INTO application_notes (application_id, category, content)
SELECT a.id, 'FOLLOW_UP', 'Send a thank-you email to Rahul and confirm the start date.'
FROM job_applications a JOIN users u ON u.id = a.user_id
WHERE u.email = 'demo@example.com' AND a.company_name = 'Northwind Labs';

INSERT INTO application_notes (application_id, category, content)
SELECT a.id, 'INTERVIEW_FEEDBACK', 'Feedback: strong coding skills, needs more practice with system design.'
FROM job_applications a JOIN users u ON u.id = a.user_id
WHERE u.email = 'demo@example.com' AND a.company_name = 'Acme Software';

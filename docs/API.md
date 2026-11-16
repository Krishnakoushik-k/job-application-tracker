# API Reference

Base URL (local): `http://localhost:8080/api`. Interactive documentation is served by the running backend at
`http://localhost:8080/swagger-ui.html` (raw spec at `/v3/api-docs`). Set `SWAGGER_ENABLED=false` in production.

All endpoints except `/api/auth/**` require the header `Authorization: Bearer <token>`.
Bodies are JSON (`Content-Type: application/json`). Page numbers are 0-based.

## Authentication

### POST `/api/auth/register` → `201 Created`

```json
{ "fullName": "Asha Reddy", "email": "asha@example.com", "password": "Secret123" }
```

Password rules: 8-72 characters, at least one letter and one digit.

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": { "id": 1, "fullName": "Asha Reddy", "email": "asha@example.com" }
}
```

Errors: `400` validation, `409` email already registered.

### POST `/api/auth/login` → `200 OK`

```json
{ "email": "asha@example.com", "password": "Secret123" }
```

Returns the same body as register. Errors: `400` validation, `401` invalid email or password (one generic message for both cases).

## Profile

| Method | Path | Body | Success |
|---|---|---|---|
| GET | `/api/users/me` | none | `200` user |
| PUT | `/api/users/me` | `{ "fullName": "...", "email": "..." }` | `200` user (`409` if email is taken) |
| PUT | `/api/users/me/password` | `{ "currentPassword": "...", "newPassword": "..." }` | `204` (`400` if current password is wrong) |

## Job applications

| Method | Path | Success |
|---|---|---|
| GET | `/api/applications` | `200` page of summaries |
| GET | `/api/applications/export` | `200` CSV download |
| GET | `/api/applications/{id}` | `200` full application |
| POST | `/api/applications` | `201` full application |
| PUT | `/api/applications/{id}` | `200` full application |
| DELETE | `/api/applications/{id}` | `204` (also deletes its interviews and notes) |

### List query parameters (all optional)

| Parameter | Meaning |
|---|---|
| `search` | Case-insensitive match on company name or job title |
| `status` | `APPLIED`, `ASSESSMENT`, `INTERVIEW`, `OFFER`, `REJECTED`, `WITHDRAWN` |
| `jobType` | `FULL_TIME`, `PART_TIME`, `INTERNSHIP` |
| `dateFrom`, `dateTo` | Inclusive application date range, `yyyy-MM-dd` |
| `sort` | `newest` (default) or `oldest` |
| `page`, `size` | Default `0` and `10`; `size` is capped at 100 |

Example: `GET /api/applications?search=java&status=INTERVIEW&sort=oldest&page=0&size=5`

### Create / update body

```json
{
  "companyName": "Globex Technologies",
  "jobTitle": "Full Stack Developer",
  "jobDescription": "Build internal tools with Spring Boot and React.",
  "location": "Bengaluru, India",
  "jobType": "FULL_TIME",
  "applicationDate": "2026-11-10",
  "status": "APPLIED",
  "jobUrl": "https://example.com/jobs/123",
  "salaryRange": "INR 10-14 LPA",
  "recruiterName": "Priya Nair",
  "recruiterEmail": "priya@globex.example",
  "notes": "Referred by a friend."
}
```

Required: `companyName`, `jobTitle`, `jobType`, `applicationDate`, `status`. Everything else is optional.

## Interviews

| Method | Path | Success |
|---|---|---|
| GET | `/api/interviews` | `200` all my interviews |
| GET | `/api/interviews/upcoming?limit=10` | `200` upcoming, soonest first |
| GET | `/api/applications/{applicationId}/interviews` | `200` |
| POST | `/api/applications/{applicationId}/interviews` | `201` |
| PUT | `/api/interviews/{id}` | `200` |
| DELETE | `/api/interviews/{id}` | `204` |

```json
{
  "roundName": "Technical Round 1",
  "interviewType": "ONLINE",
  "scheduledAt": "2026-11-20T14:30:00",
  "locationOrLink": "https://meet.example.com/abc",
  "notes": "Focus on data structures."
}
```

`interviewType`: `ONLINE`, `PHONE`, `IN_PERSON`.

## Notes

| Method | Path | Success |
|---|---|---|
| GET | `/api/applications/{applicationId}/notes` | `200` |
| POST | `/api/applications/{applicationId}/notes` | `201` |
| PUT | `/api/notes/{id}` | `200` |
| DELETE | `/api/notes/{id}` | `204` |

```json
{ "category": "PREPARATION", "content": "Revise Spring Security and JPA lazy loading." }
```

`category`: `RECRUITER_CONVERSATION`, `INTERVIEW_FEEDBACK`, `FOLLOW_UP`, `PREPARATION`, `GENERAL`.

## Dashboard

### GET `/api/dashboard` → `200 OK`

```json
{
  "totalApplications": 12,
  "appliedCount": 5,
  "assessmentCount": 1,
  "interviewCount": 3,
  "offerCount": 1,
  "rejectedCount": 2,
  "withdrawnCount": 0,
  "upcomingInterviewCount": 2,
  "successRate": 8.3,
  "statusDistribution": [ { "status": "APPLIED", "count": 5 } ],
  "monthlyTrend": [ { "month": "2026-11", "count": 7 } ],
  "upcomingInterviews": [],
  "recentApplications": []
}
```

`successRate` = offers / total applications x 100, rounded to one decimal, `0` when there are no applications.

## Error format

Every error uses the same body and never includes stack traces:

```json
{
  "timestamp": "2026-11-10T09:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/applications",
  "fieldErrors": { "companyName": "Company name is required" }
}
```

| Status | When |
|---|---|
| 400 | Validation failure, malformed JSON, bad parameter value |
| 401 | Missing, invalid or expired token; wrong login credentials |
| 403 | Authenticated but not allowed |
| 404 | Resource does not exist **or belongs to another user** |
| 409 | Email already registered |
| 500 | Unexpected error (generic message only) |

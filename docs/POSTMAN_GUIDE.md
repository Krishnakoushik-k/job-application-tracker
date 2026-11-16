# Postman testing guide

1. Start the backend (see the README). Base URL: `http://localhost:8080/api`.
2. In Postman create an environment with two variables: `baseUrl` = `http://localhost:8080/api` and `token` (leave empty).
3. **Register**: `POST {{baseUrl}}/auth/register`, body (raw JSON):
   `{ "fullName": "Test User", "email": "test@example.com", "password": "Secret123" }`.
   Expect `201` and a `token` in the response.
4. **Save the token automatically.** In the request's *Scripts → Post-response* tab add:
   `pm.environment.set("token", pm.response.json().token);`
5. **Authorize other requests.** On the collection, open *Authorization*, choose *Bearer Token* and enter `{{token}}`.
6. Run through these checks:

| Request | Expected |
|---|---|
| `POST /auth/login` with a wrong password | `401`, message "Invalid email or password" |
| `POST /auth/register` with `"password": "short"` | `400` with `fieldErrors.password` |
| `GET /applications` with no token | `401` |
| `POST /applications` with the body from `docs/API.md` | `201` |
| `GET /applications?search=globex&status=APPLIED` | `200`, filtered page |
| `PUT /applications/{id}` with a changed `status` | `200` |
| `POST /applications/{id}/interviews` | `201` |
| `POST /applications/{id}/notes` | `201` |
| `GET /dashboard` | `200` with counts matching your data |
| `GET /applications/export` | `200`, use *Send and Download* to save the CSV |
| `DELETE /applications/{id}` | `204`, then `GET` the same id returns `404` |

7. **Verify isolation.** Register a second user, copy their token into `token`, then request an application id created by the
   first user. Expect `404`, never the data.

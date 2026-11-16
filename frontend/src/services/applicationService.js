import api from './api';

/** Removes empty filters so the query string only contains real criteria. */
function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined),
  );
}

export const searchApplications = (params) =>
  api.get('/applications', { params: cleanParams(params) }).then((res) => res.data);

export const getApplication = (id) => api.get(`/applications/${id}`).then((res) => res.data);

export const createApplication = (payload) => api.post('/applications', payload).then((res) => res.data);

export const updateApplication = (id, payload) =>
  api.put(`/applications/${id}`, payload).then((res) => res.data);

export const deleteApplication = (id) => api.delete(`/applications/${id}`);

/** Downloads the CSV through Axios (so the Authorization header is sent) and saves it as a file. */
export async function exportApplicationsCsv() {
  const response = await api.get('/applications/export', { responseType: 'blob' });
  const url = window.URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'job-applications.csv';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

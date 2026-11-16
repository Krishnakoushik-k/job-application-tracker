import api from './api';

export const listInterviews = () => api.get('/interviews').then((res) => res.data);

export const listUpcomingInterviews = (limit = 10) =>
  api.get('/interviews/upcoming', { params: { limit } }).then((res) => res.data);

export const listApplicationInterviews = (applicationId) =>
  api.get(`/applications/${applicationId}/interviews`).then((res) => res.data);

export const createInterview = (applicationId, payload) =>
  api.post(`/applications/${applicationId}/interviews`, payload).then((res) => res.data);

export const updateInterview = (id, payload) => api.put(`/interviews/${id}`, payload).then((res) => res.data);

export const deleteInterview = (id) => api.delete(`/interviews/${id}`);

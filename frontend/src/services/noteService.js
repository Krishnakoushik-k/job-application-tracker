import api from './api';

export const listNotes = (applicationId) =>
  api.get(`/applications/${applicationId}/notes`).then((res) => res.data);

export const createNote = (applicationId, payload) =>
  api.post(`/applications/${applicationId}/notes`, payload).then((res) => res.data);

export const updateNote = (id, payload) => api.put(`/notes/${id}`, payload).then((res) => res.data);

export const deleteNote = (id) => api.delete(`/notes/${id}`);

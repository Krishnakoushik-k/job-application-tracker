import api from './api';

export const getProfile = () => api.get('/users/me').then((res) => res.data);

export const updateProfile = (payload) => api.put('/users/me', payload).then((res) => res.data);

export const changePassword = (currentPassword, newPassword) =>
  api.put('/users/me/password', { currentPassword, newPassword });

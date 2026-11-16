import api from './api';

export const getDashboard = () => api.get('/dashboard').then((res) => res.data);

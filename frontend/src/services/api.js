import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
});

let authClient = null;

export const setAuthClient = (client) => {
  authClient = client;
};

api.interceptors.request.use(async (config) => {
  if (authClient) {
    try {
      const tokenManager = authClient.tokenManager;
      const accessToken = await tokenManager.get('accessToken');
      if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken.accessToken}`;
      }
    } catch (err) {
      console.error('Failed to attach auth token', err);
    }
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && authClient) {
      authClient.signInWithRedirect();
    }
    return Promise.reject(error);
  }
);

export const getMe = () => api.get('/auth/me');

export const getTickets = () => api.get('/tickets');

export const getTicketStats = () => api.get('/tickets/stats');

/** @param {Record<string,string|number|undefined>} params — q, status, priority, page, size, sort */
export const searchTickets = (params) => api.get('/tickets/search', { params });

export const getTicketPool = (params = {}) =>
  api.get('/tickets/pool', {
    params: { page: 0, size: 20, sort: 'createdAt,asc', ...params },
  });

export const getTicket = (id) => api.get(`/tickets/${id}`);

/** @param {Record<string,string|number|undefined>} params — page, size, sort */
export const getTicketActivity = (ticketId, params = {}) =>
  api.get(`/tickets/${ticketId}/activity`, {
    params: { page: 0, size: 40, sort: 'createdAt,asc', ...params },
  });

/** Visible SLA-overdue tickets (OPEN / IN_PROGRESS, past first-response deadline). */
export const getSlaBreachedTickets = (params = {}) =>
  api.get('/tickets/sla/breached', {
    params: { page: 0, size: 20, sort: 'slaRespondBy,asc', ...params },
  });

export const getTicketMessages = (ticketId) => api.get(`/tickets/${ticketId}/messages`);

export const postTicketMessage = (ticketId, body) =>
  api.post(`/tickets/${ticketId}/messages`, { body });

export const getTicketAttachments = (ticketId) => api.get(`/tickets/${ticketId}/attachments`);

export const requestTicketAttachmentUploadUrl = (ticketId, payload) =>
  api.post(`/tickets/${ticketId}/attachments/upload-url`, payload);

export const confirmTicketAttachment = (ticketId, payload) =>
  api.post(`/tickets/${ticketId}/attachments/confirm`, payload);

export const deleteTicketAttachment = (ticketId, attachmentId) =>
  api.delete(`/tickets/${ticketId}/attachments/${attachmentId}`);

export const submitTicketSatisfaction = (ticketId, payload) =>
  api.post(`/tickets/${ticketId}/satisfaction`, payload);

export const claimTicket = (id) => api.post(`/tickets/${id}/claim`);

export const exportTicketsCsv = () =>
  api.get('/tickets/export', { responseType: 'blob', headers: { Accept: 'text/csv' } });

export const createTicket = (data) => api.post('/tickets', data);
export const updateTicket = (id, data) => api.put(`/tickets/${id}`, data);

export const getSessions = () => api.get('/sessions');
export const startSession = (data) => api.post('/sessions', data);
export const endSession = (id, notes) => api.post(`/sessions/${id}/end`, { notes });
export const getUploadUrl = (sessionId, fileName, contentType) =>
  api.post('/recordings/upload-url', { sessionId, fileName, contentType });
export const confirmUpload = (data) => api.post('/recordings/confirm', data);
export const getRecording = (id) => api.get(`/recordings/${id}`);
export const getRecordingsBySession = (sessionId) => api.get(`/recordings/session/${sessionId}`);
export const getDashboard = () => api.get('/admin/dashboard');
export const getUsers = () => api.get('/users');
export const updateUserRole = (id, role) => api.put(`/users/${id}/role`, { role });

export const getAISummary = () => api.get('/ai/insights/summary');
export const getRecentAnalyses = () => api.get('/ai/analysis/recent');
export const getRecentInsights = () => api.get('/ai/insights/recent');
export const analyzeTranscript = (data) => api.post('/ai/analyze-transcript', data);
export const categorizeTicket = (ticketId) => api.post(`/ai/categorize-ticket/${ticketId}`);
export const generateCustomerInsights = (userId) => api.post(`/ai/generate-insights/user/${userId}`);

export default api;

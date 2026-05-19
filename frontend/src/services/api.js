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

/** Compact status counts for tickets visible to the current user. */
export const getMyTicketSummary = () => api.get('/tickets/my/summary');

/** Recently updated visible tickets (default: updatedAt desc, size 10). */
export const getRecentTickets = (params = {}) =>
  api.get('/tickets/recent', { params: { page: 0, size: 10, sort: 'updatedAt,desc', ...params } });

/** Distinct tags on tickets visible to the current user (role-scoped). */
export const getTicketTags = () => api.get('/tickets/tags');

/** @param {Record<string,string|number|undefined>} params — q, status, priority, page, size, sort */
export const searchTickets = (params) => api.get('/tickets/search', { params });

export const getTicketPool = (params = {}) =>
  api.get('/tickets/pool', {
    params: { page: 0, size: 20, sort: 'createdAt,asc', ...params },
  });

export const getTicket = (id) => api.get(`/tickets/${id}`);

/** Support sessions linked to a ticket (subject to ticket visibility). */
export const getTicketSessions = (ticketId) => api.get(`/tickets/${ticketId}/sessions`);

/** Recording metadata for all sessions on this ticket (no presigned URLs; use getRecording per id). */
export const getTicketRecordings = (ticketId) => api.get(`/tickets/${ticketId}/recordings`);

/** Merged activity + message timeline (chronological). */
export const getTicketTimeline = (ticketId) => api.get(`/tickets/${ticketId}/timeline`);

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

export const postTicketMessage = (ticketId, payload) => {
  if (typeof payload === 'string') {
    return api.post(`/tickets/${ticketId}/messages`, { body: payload });
  }
  return api.post(`/tickets/${ticketId}/messages`, {
    body: payload.body,
    ...(payload.internalNote ? { internalNote: true } : {}),
  });
};

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

/** Release assignment; ticket goes back to OPEN / pool (executive, admin, or manager). */
export const releaseTicket = (id) => api.post(`/tickets/${id}/release`);

/** Reopen RESOLVED/CLOSED ticket (must be allowed to view it). */
export const reopenTicket = (id) => api.post(`/tickets/${id}/reopen`);

export const exportTicketsCsv = () =>
  api.get('/tickets/export', { responseType: 'blob', headers: { Accept: 'text/csv' } });

export const createTicket = (data) => api.post('/tickets', data);
export const updateTicket = (id, data) => api.put(`/tickets/${id}`, data);

export const applyTicketAiClassification = (ticketId) =>
  api.post(`/tickets/${ticketId}/classification/apply-ai`);

/** Saved replies / macros — support roles read; managers & admins mutate. */
export const getSavedReplies = () => api.get('/saved-replies');

export const createSavedReply = (payload) => api.post('/saved-replies', payload);

export const updateSavedReply = (id, payload) => api.put(`/saved-replies/${id}`, payload);

export const deleteSavedReply = (id) => api.delete(`/saved-replies/${id}`);

/** @param {Record<string,string|number|undefined>} params — q (required, min 2 chars effective), limit */
export const searchSavedReplies = (params) =>
  api.get('/saved-replies/search', { params: { limit: 20, ...params } });

export const getSessions = () => api.get('/sessions');
export const startSession = (data) => api.post('/sessions', data);
export const endSession = (id, notes) => api.post(`/sessions/${id}/end`, { notes });
export const getUploadUrl = (sessionId, fileName, contentType) =>
  api.post('/recordings/upload-url', { sessionId, fileName, contentType });
export const confirmUpload = (data) => api.post('/recordings/confirm', data);
export const getRecording = (id) => api.get(`/recordings/${id}`);
export const getRecordingsBySession = (sessionId) => api.get(`/recordings/session/${sessionId}`);
export const getDashboard = () => api.get('/admin/dashboard');

/** @param {Record<string,string|number|undefined>} params — page, size, sort */
export const getAdminRecentTicketActivity = (params = {}) =>
  api.get('/admin/activity/recent', { params: { page: 0, size: 20, sort: 'createdAt,desc', ...params } });
export const getExecutiveWorkloadReport = () => api.get('/admin/reports/executive-workload');
export const getUsers = () => api.get('/users');

/** Active support executives for assignment pickers (admin / manager). */
export const getAssignableExecutives = () => api.get('/users/assignable-executives');
export const updateUserRole = (id, role) => api.put(`/users/${id}/role`, { role });

export const getAISummary = () => api.get('/ai/insights/summary');
export const getRecentAnalyses = () => api.get('/ai/analysis/recent');
export const getRecentInsights = () => api.get('/ai/insights/recent');
export const analyzeTranscript = (data) => api.post('/ai/analyze-transcript', data);
export const categorizeTicket = (ticketId) => api.post(`/ai/categorize-ticket/${ticketId}`);
export const generateCustomerInsights = (userId) => api.post(`/ai/generate-insights/user/${userId}`);

export default api;

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
export const getTickets = (params) =>
  api.get('/tickets', params != null ? { params } : {});
export const getTicket = (id) => api.get(`/tickets/${id}`);
export const exportTicketsCsv = () => api.get('/tickets/export', { responseType: 'blob' });
export const createTicket = (data) => api.post('/tickets', data);
export const updateTicket = (id, data) => api.put(`/tickets/${id}`, data);
export const getTicketComments = (ticketId) => api.get(`/tickets/${ticketId}/comments`);
export const createTicketComment = (ticketId, data) => api.post(`/tickets/${ticketId}/comments`, data);
export const updateTicketComment = (ticketId, commentId, data) =>
  api.put(`/tickets/${ticketId}/comments/${commentId}`, data);
export const deleteTicketComment = (ticketId, commentId) =>
  api.delete(`/tickets/${ticketId}/comments/${commentId}`);
export const getSessions = () => api.get('/sessions');
export const startSession = (data) => api.post('/sessions', data);
export const endSession = (id, notes) => api.put(`/sessions/${id}/end`, { notes });
export const createSessionShareLink = (sessionId) =>
  api.post(`/sessions/${sessionId}/share-link`);
export const resolveSessionShareLink = (token) =>
  api.get(`/sessions/shared/${encodeURIComponent(token)}`);
export const revokeSessionShareLink = (linkId) =>
  api.delete(`/sessions/share-links/${linkId}`);
export const getUploadUrl = (sessionId, fileName, contentType) =>
  api.post('/recordings/upload-url', { sessionId, fileName, contentType });
export const confirmUpload = (data) => api.post('/recordings/confirm', data);
export const getRecording = (id) => api.get(`/recordings/${id}`);
export const getRecordingDownloadUrl = (id) => api.post(`/recordings/${id}/download-url`);
export const getRecordingsBySession = (sessionId) => api.get(`/recordings/session/${sessionId}`);
export const getDashboard = () => api.get('/dashboard');
export const getUsers = () => api.get('/users');
export const updateUserRole = (id, role) => api.put(`/users/${id}/role`, { role });
export const getAuditLogs = (params) => api.get('/admin/audit-logs', { params });
export const getAdminRecordings = (params) => api.get('/admin/recordings', params != null ? { params } : {});
export const getStorageDefaults = () => api.get('/admin/storage/defaults');
export const listOrganizations = () => api.get('/admin/storage/organizations');
export const createOrganization = (name) => api.post('/admin/storage/organizations', { name });
export const upsertRetentionPolicy = (data) => api.put('/admin/storage/retention-policies', data);
export const assignUserOrganization = (userId, organizationId) =>
  api.put(`/admin/storage/users/${userId}/organization`, { organizationId });
export const setRecordingLegalHold = (recordingId, legalHold) =>
  api.patch(`/admin/storage/recordings/${recordingId}/legal-hold`, { legalHold });
export const softDeleteRecordingStorage = (recordingId) =>
  api.post(`/admin/storage/recordings/${recordingId}/soft-delete`);

export const getAISummary = () => api.get('/ai/insights/summary');
export const getRecentAnalyses = () => api.get('/ai/analysis/recent');
export const getRecentInsights = () => api.get('/ai/insights/recent');
export const analyzeTranscript = (data) => api.post('/ai/analyze-transcript', data);
export const categorizeTicket = (ticketId) => api.post(`/ai/categorize-ticket/${ticketId}`);
export const generateCustomerInsights = (userId) => api.post(`/ai/generate-insights/user/${userId}`);
export const getAiLlmDataHandling = () => api.get('/ai/llm-data-handling');

export const getAllKBArticles = () => api.get('/kb');
export const searchKBArticles = (q) => api.get('/kb/search', { params: { q } });
export const getKBArticleById = (id) => api.get(`/kb/${id}`);

export const getPortalKBCategories = () => api.get('/portal/kb/categories');
export const listPortalKBArticles = (params) =>
  api.get('/portal/kb/articles', params != null ? { params } : {});
export const searchPortalKBArticles = (q) =>
  api.get('/portal/kb/articles/search', { params: { q } });
export const getPortalKBArticle = (id) => api.get(`/portal/kb/articles/${id}`);
export const getPortalKBRelatedArticles = (id, limit) =>
  api.get(`/portal/kb/articles/${id}/related`, limit != null ? { params: { limit } } : {});

export const getCannedResponses = () => api.get('/canned-responses');
export const getCannedResponse = (id) => api.get(`/canned-responses/${id}`);
export const createCannedResponse = (data) => api.post('/canned-responses', data);
export const updateCannedResponse = (id, data) => api.put(`/canned-responses/${id}`, data);
export const deleteCannedResponse = (id) => api.delete(`/canned-responses/${id}`);

export default api;

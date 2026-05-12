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

export const getMe = () => api.get('/users/me');
export const getTickets = () => api.get('/tickets');
export const getTicket = (id) => api.get(`/tickets/${id}`);
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

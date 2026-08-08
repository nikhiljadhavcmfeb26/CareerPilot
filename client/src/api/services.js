import api from './axios';

/**
 * Resume endpoints return a raw PDF stream, not the usual { success, message,
 * data } envelope, so they need responseType: 'blob'. This helper turns the
 * response into a temporary object URL and opens it in a new tab, reading the
 * filename from Content-Disposition when the server sends one (the gateway
 * exposes that header - see api-gateway.yml).
 */
export const openPdfResponse = (response, fallbackName = 'resume.pdf') => {
  const blob = new Blob([response.data], { type: 'application/pdf' });
  const url = window.URL.createObjectURL(blob);

  const disposition = response.headers?.['content-disposition'] || '';
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
  const fileName = match ? decodeURIComponent(match[1]) : fallbackName;

  const link = document.createElement('a');
  link.href = url;
  link.target = '_blank';
  link.rel = 'noopener';
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  // Give the browser a moment to start reading the blob before revoking it.
  setTimeout(() => window.URL.revokeObjectURL(url), 10000);
};

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  // Sends the refresh token so the session can still be revoked server-side
  // when the access token has already expired.
  logout: (refreshToken) => api.post('/auth/logout', { refreshToken }),
  refreshToken: (refreshToken) => api.post('/auth/refresh-token', { refreshToken }),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword: (data) => api.post('/auth/reset-password', data)
};

export const userApi = {
  getMe: () => api.get('/users/me'),
  updateMe: (data) => api.put('/users/me', data),
  getAll: () => api.get('/users'),
  deactivate: (id) => api.put(`/users/${id}/deactivate`),
  activate: (id) => api.put(`/users/${id}/activate`),
  changePassword: (data) => api.put('/users/me/password', data)
};

export const companyApi = {
  register: (data) => api.post('/companies', data),
  update: (data) => api.put('/companies', data),
  getMy: () => api.get('/companies/my'),
  getPending: () => api.get('/companies/pending'),
  approve: (id) => api.put(`/companies/${id}/approve`),
  // Admin-only. getAllAdmin lists approved employers too, so an approval can
  // be taken back; getPending alone could only ever grant one.
  getAllAdmin: () => api.get('/companies/admin/all'),
  revokeApproval: (id) => api.put(`/companies/${id}/revoke-approval`)
};

export const jobApi = {
  search: (params) => api.get('/jobs', { params }),
  getById: (id) => api.get(`/jobs/${id}`),
  create: (data) => api.post('/jobs', data),
  update: (id, data) => api.put(`/jobs/${id}`, data),
  delete: (id) => api.delete(`/jobs/${id}`),
  getMy: () => api.get('/jobs/my'),
  publish: (id) => api.put(`/jobs/${id}/publish`),
  close: (id) => api.put(`/jobs/${id}/close`),
  getAllAdmin: () => api.get('/jobs/admin/all'),
  adminDelete: (id) => api.delete(`/jobs/admin/${id}`)
};

export const applicationApi = {
  apply: (data) => api.post('/applications', data),

  withdraw: (id) =>
    api.put(`/applications/${id}/withdraw`),

  getMy: () =>
    api.get('/applications/my'),

  getByJob: (jobId) =>
    api.get(`/applications/job/${jobId}`),

  updateStatus: (id, status) =>
    api.put(`/applications/${id}/status`, { status }),

  // NEW

  checkStatus: (jobId) =>
    api.get(`/applications/check/${jobId}`),

  // Employer-only. Authorization is enforced server-side: application-service
  // verifies the caller owns the job before user-service is ever contacted.
  downloadResume: (applicationId) =>
    api.get(`/applications/${applicationId}/resume`, { responseType: 'blob' }),

  // Admin-only recruitment monitoring. Lives on application-service, not on
  // the admin service, because that is where the data is.
  getAllAdmin: (params) => api.get('/applications/admin/all', { params })
};

export const bookmarkApi = {
  getSaved: () => api.get('/bookmarks'),
  save: (jobId) => api.post('/bookmarks', { jobId }),
  remove: (jobId) => api.delete(`/bookmarks/${jobId}`)
};

export const resumeApi = {
  getMy: () => api.get('/resumes'),
  upload: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/resumes/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
  },
  delete: (id) => api.delete(`/resumes/${id}`),
  setDefault: (id) => api.put(`/resumes/${id}/default`),
  download: (id) => api.get(`/resumes/${id}/download`, { responseType: 'blob' })
};

export const profileApi = {
  get: () => api.get('/profiles'),
  update: (data) => api.put('/profiles', data)
};

/**
 * Premium subscription. Every one of these already existed on
 * auth-service's SubscriptionController - none of it is new backend surface
 * except /plans, which just exposes the configured price.
 */
export const subscriptionApi = {
  getPlan: () => api.get('/auth/subscriptions/plans'),
  createOrder: () => api.post('/auth/subscriptions/create-order', { planName: 'PREMIUM' }),
  verifyPayment: (data) => api.post('/auth/subscriptions/verify-payment', data),
  getMy: () => api.get('/auth/subscriptions/my')
};

/**
 * AI features. Every endpoint here already existed on ai-service's
 * AiController - this is purely the missing client wiring.
 *
 * All four are POST (they perform work and are logged in AiRequestLog), and
 * two of them take no body at all: ai-service resolves the caller's default
 * resume from the JWT rather than accepting a resumeId from the client.
 *
 * These calls are SLOW by nature - Gemini round-trips take seconds, and
 * candidate screening does one per applicant - so no client-side timeout is
 * set and every caller shows an explicit loading state.
 */
export const aiApi = {
  resumeFeedback: () => api.post('/ai/resume-feedback'),
  coverLetter: (jobId) => api.post('/ai/cover-letter', { jobId }),
  candidateScreening: (jobId) => api.post('/ai/candidate-screening', { jobId }),
  jobRecommendations: () => api.post('/ai/job-recommendations')
};

/**
 * ADMIN MODULE. Everything under /api/admin is Admin-only and enforced in
 * three places (gateway JWT check, auth-service SecurityConfig prefix rule,
 * and the service layer's self-lockout guards) - the frontend role check in
 * ProtectedRoute is a UX affordance, never the security boundary.
 */
export const adminApi = {
  stats: () => api.get('/admin/stats'),

  // params: { role, active, search } - all optional
  getUsers: (params) => api.get('/admin/users', { params }),
  getUser: (id) => api.get(`/admin/users/${id}`),
  activateUser: (id) => api.put(`/admin/users/${id}/activate`),
  deactivateUser: (id) => api.put(`/admin/users/${id}/deactivate`),
  blockUser: (id, reason) => api.put(`/admin/users/${id}/block`, { reason }),
  unblockUser: (id) => api.put(`/admin/users/${id}/unblock`),

  enableUserAi: (id) => api.put(`/admin/users/${id}/ai/enable`),
  disableUserAi: (id) => api.put(`/admin/users/${id}/ai/disable`),

  // params: { status } - CREATED | PAID | FAILED
  getSubscriptions: (params) => api.get('/admin/subscriptions', { params }),
  revokeSubscription: (id) => api.put(`/admin/subscriptions/${id}/revoke`),
  extendSubscription: (id, days) => api.put(`/admin/subscriptions/${id}/extend`, { days }),

  getAiSettings: () => api.get('/admin/ai-settings'),
  updateAiSettings: (settings) => api.put('/admin/ai-settings', settings)
};

export const dashboardApi = {
  admin: () => api.get('/dashboard/admin'),
  employer: () => api.get('/dashboard/employer'),
  jobSeeker: () => api.get('/dashboard/jobseeker')
};

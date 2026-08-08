import { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import { applicationApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

const STATUSES = ['Pending', 'Reviewed', 'Shortlisted', 'Rejected', 'Accepted', 'Withdrawn'];

const STATUS_COLORS = {
  Pending: 'secondary',
  Reviewed: 'info',
  Shortlisted: 'primary',
  Accepted: 'success',
  Rejected: 'danger',
  Withdrawn: 'dark'
};

/**
 * ADMIN MODULE - recruitment monitoring. Read-only on purpose: an admin
 * watches the pipeline but does not make hiring decisions on an employer's
 * behalf, so there is no status-change control here (application-service only
 * accepts those from the employer who owns the job).
 */
const AdminApplications = () => {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState('');

  const fetchApplications = useCallback(() => {
    setLoading(true);
    applicationApi.getAllAdmin({ status: status || undefined })
      .then(({ data }) => {
        if (data.success) setApplications(data.data);
      })
      .catch((err) => toast.error(err.response?.data?.message || 'Could not load applications'))
      .finally(() => setLoading(false));
  }, [status]);

  useEffect(() => { fetchApplications(); }, [fetchApplications]);

  return (
    <div className="container py-4">
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
        <h2 className="fw-bold mb-0">Application Monitoring</h2>
        <span className="text-muted small">{applications.length} application(s)</span>
      </div>

      <div className="row g-2 mb-3">
        <div className="col-md-3">
          <select className="form-select" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All statuses</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>
      </div>

      {loading ? <LoadingSpinner /> : (
        <div className="table-responsive">
          <table className="table table-hover align-middle bg-white shadow-sm">
            <thead className="table-primary">
              <tr>
                <th>#</th><th>Applicant</th><th>Email</th><th>Job</th>
                <th>Company</th><th>Status</th><th>Applied</th>
              </tr>
            </thead>
            <tbody>
              {applications.length === 0 && (
                <tr><td colSpan="7" className="text-center text-muted py-4">No applications found.</td></tr>
              )}
              {applications.map((a) => (
                <tr key={a.id}>
                  <td className="text-muted small">{a.id}</td>
                  <td>{a.applicantName || 'Unknown'}</td>
                  <td className="small">{a.applicantEmail || '-'}</td>
                  <td>{a.jobTitle || `Job #${a.jobId}`}</td>
                  <td>{a.companyName || '-'}</td>
                  <td><span className={`badge bg-${STATUS_COLORS[a.status] || 'secondary'}`}>{a.status}</span></td>
                  <td>{a.appliedAt ? new Date(a.appliedAt).toLocaleDateString() : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default AdminApplications;

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../../api/services';
import { toast } from 'react-toastify';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../../components/LoadingSpinner';

const EmployerDashboard = () => {
  const { isPremium } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardApi.employer().then(({ data: res }) => {
      if (res.success) setData(res.data);
    }).catch((err) => {
      toast.error(err.response?.data?.message || 'Could not load dashboard');
    }).finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingSpinner fullPage />;

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center gap-3 mb-4">
        <div className="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center fw-bold" style={{ width: '48px', height: '48px', fontSize: '1.25rem' }}>
          <i className="bi bi-speedometer2"></i>
        </div>
        <div>
          <h2 className="fw-bold mb-0">Employer Dashboard</h2>
          <p className="text-muted mb-0 small">Welcome back! Manage your job postings and review applicants.</p>
        </div>
      </div>

      {!isPremium && (
        <div className="alert alert-warning border-0 shadow-sm d-flex align-items-center justify-content-between gap-3 mb-4">
          <div className="d-flex align-items-center gap-3">
            <i className="bi bi-star-fill text-warning fs-4"></i>
            <div>
              <strong>Unlock AI features with Premium.</strong>
              <div className="small">AI candidate screening and automatic feedback for rejected candidates.</div>
            </div>
          </div>
          <Link to="/premium" className="btn btn-sm btn-warning text-dark fw-semibold text-nowrap">Upgrade</Link>
        </div>
      )}

      {!data?.isApproved && (
        <div className="alert alert-warning border-0 shadow-sm d-flex align-items-center gap-3 mb-4">
          <i className="bi bi-exclamation-triangle-fill text-warning fs-4"></i>
          <div>
            Your company is pending admin approval. You cannot publish jobs until approved.
          </div>
        </div>
      )}

      <div className="row g-4 mb-5">
        {[
          { label: 'Total Jobs', value: data?.totalJobs, color: 'primary', icon: 'bi-briefcase-fill' },
          { label: 'Published Jobs', value: data?.publishedJobs, color: 'success', icon: 'bi-cloud-check-fill' },
          { label: 'Total Applications', value: data?.totalApplications, color: 'info', icon: 'bi-people-fill' },
          { label: 'Pending Review', value: data?.pendingApplications, color: 'warning', icon: 'bi-hourglass-split' }
        ].map((s) => (
          <div key={s.label} className="col-md-3 col-sm-6">
            <div className={`card border-0 shadow-sm p-4 h-100 dashboard-card dashboard-card-${s.color}`}>
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div className={`p-3 bg-${s.color} bg-opacity-10 text-${s.color} rounded-3 d-flex align-items-center justify-content-center`} style={{ width: '48px', height: '48px' }}>
                  <i className={`bi ${s.icon} fs-4`}></i>
                </div>
                <h3 className={`fw-bold mb-0 text-dark`}>{s.value ?? 0}</h3>
              </div>
              <p className="text-muted mb-0 small fw-semibold text-uppercase tracking-wider">{s.label}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="d-flex flex-wrap gap-3">
        <Link to="/employer/jobs/create" className="btn btn-primary px-4 py-2.5 d-inline-flex align-items-center gap-2 shadow-sm">
          <i className="bi bi-plus-circle"></i> Post New Job
        </Link>
        <Link to="/employer/jobs" className="btn btn-outline-primary px-4 py-2.5 d-inline-flex align-items-center gap-2">
          <i className="bi bi-list-task"></i> Manage Jobs
        </Link>
        <Link to="/employer/company" className="btn btn-outline-secondary px-4 py-2.5 d-inline-flex align-items-center gap-2">
          <i className="bi bi-building"></i> Company Profile
        </Link>
      </div>
    </div>
  );
};

export default EmployerDashboard;

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../api/services';
import { toast } from 'react-toastify';
import LoadingSpinner from '../../components/LoadingSpinner';

/**
 * Reads /api/admin/stats rather than the older /api/dashboard/admin. Both
 * still work, but only the admin endpoint carries the counters the admin
 * module added (blocked accounts, premium subscribers, application pipeline).
 */
const TILES = [
  { key: 'totalUsers', label: 'Total Users', color: 'primary', icon: 'bi-people-fill' },
  { key: 'totalJobSeekers', label: 'Job Seekers', color: 'info', icon: 'bi-person-badge-fill' },
  { key: 'totalEmployers', label: 'Employers', color: 'success', icon: 'bi-building-fill' },
  { key: 'blockedUsers', label: 'Blocked Users', color: 'danger', icon: 'bi-slash-circle-fill' },
  { key: 'totalJobs', label: 'Total Jobs', color: 'secondary', icon: 'bi-briefcase-fill' },
  { key: 'publishedJobs', label: 'Published Jobs', color: 'success', icon: 'bi-cloud-check-fill' },
  { key: 'totalApplications', label: 'Applications', color: 'warning', icon: 'bi-file-earmark-text-fill' },
  { key: 'premiumSubscribers', label: 'Premium Subscribers', color: 'warning', icon: 'bi-star-fill' },
  { key: 'pendingEmployerApprovals', label: 'Pending Approvals', color: 'danger', icon: 'bi-shield-exclamation' }
];

const AdminDashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi.stats().then(({ data: res }) => {
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
          <h2 className="fw-bold mb-0">Admin Dashboard</h2>
          <p className="text-muted mb-0 small">Portal operations, users, jobs, subscriptions and AI access.</p>
        </div>
      </div>

      <div className="row g-4 mb-5">
        {TILES.map((tile) => (
          <div key={tile.key} className="col-md-3 col-sm-6">
            <div className={`card border-0 shadow-sm p-4 h-100 dashboard-card dashboard-card-${tile.color}`}>
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div className={`p-3 bg-${tile.color} bg-opacity-10 text-${tile.color} rounded-3 d-flex align-items-center justify-content-center`} style={{ width: '48px', height: '48px' }}>
                  <i className={`bi ${tile.icon} fs-4`}></i>
                </div>
                <h3 className="fw-bold mb-0 text-dark">{data?.[tile.key] ?? 0}</h3>
              </div>
              <p className="text-muted mb-0 small fw-semibold text-uppercase">{tile.label}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="d-flex flex-wrap gap-3">
        <Link to="/admin/users" className="btn btn-primary px-4 py-2 d-inline-flex align-items-center gap-2 shadow-sm">
          <i className="bi bi-people"></i> Manage Users
        </Link>
        <Link to="/admin/employers" className="btn btn-outline-primary px-4 py-2 d-inline-flex align-items-center gap-2">
          <i className="bi bi-patch-check"></i> Employers
        </Link>
        <Link to="/admin/jobs" className="btn btn-outline-secondary px-4 py-2 d-inline-flex align-items-center gap-2">
          <i className="bi bi-briefcase"></i> Manage Jobs
        </Link>
        <Link to="/admin/applications" className="btn btn-outline-secondary px-4 py-2 d-inline-flex align-items-center gap-2">
          <i className="bi bi-file-earmark-text"></i> Applications
        </Link>
        <Link to="/admin/subscriptions" className="btn btn-outline-warning px-4 py-2 d-inline-flex align-items-center gap-2">
          <i className="bi bi-star"></i> Subscriptions
        </Link>
        <Link to="/admin/ai" className="btn btn-outline-dark px-4 py-2 d-inline-flex align-items-center gap-2">
          <i className="bi bi-robot"></i> AI Features
        </Link>
      </div>
    </div>
  );
};

export default AdminDashboard;

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../../api/services';
import { toast } from 'react-toastify';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../../components/LoadingSpinner';

const JobSeekerDashboard = () => {
  const { isPremium } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardApi.jobSeeker().then(({ data: res }) => {
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
          <h2 className="fw-bold mb-0">Job Seeker Dashboard</h2>
          <p className="text-muted mb-0 small">Welcome back! Here's an overview of your job search progress.</p>
        </div>
      </div>

      {!isPremium && (
        <div className="alert alert-warning border-0 shadow-sm d-flex align-items-center justify-content-between gap-3 mb-4">
          <div className="d-flex align-items-center gap-3">
            <i className="bi bi-star-fill text-warning fs-4"></i>
            <div>
              <strong>Unlock AI features with Premium.</strong>
              <div className="small">AI resume analysis, cover letter generation and personalised job recommendations.</div>
            </div>
          </div>
          <Link to="/premium" className="btn btn-sm btn-warning text-dark fw-semibold text-nowrap">Upgrade</Link>
        </div>
      )}

      {!data?.hasResume && (
        <div className="alert alert-info border-0 shadow-sm d-flex align-items-center gap-3 mb-4">
          <i className="bi bi-info-circle-fill text-info fs-4"></i>
          <div>
            Upload your resume to apply for jobs faster. <Link to="/jobseeker/resume" className="fw-semibold text-decoration-none">Upload Resume</Link>
          </div>
        </div>
      )}

      <div className="row g-4 mb-5">
        {[
          { label: 'Applied Jobs', value: data?.appliedJobs, link: '/jobseeker/applied', color: 'primary', icon: 'bi-send-fill' },
          { label: 'Saved Jobs', value: data?.savedJobs, link: '/jobseeker/saved', color: 'success', icon: 'bi-bookmark-star-fill' },
          { label: 'Pending Applications', value: data?.pendingApplications, link: '/jobseeker/applied', color: 'warning', icon: 'bi-clock-history' },
          { label: 'Shortlisted', value: data?.shortlistedApplications, link: '/jobseeker/applied', color: 'info', icon: 'bi-trophy-fill' }
        ].map((s) => (
          <div key={s.label} className="col-md-3 col-sm-6">
            <Link to={s.link} className="text-decoration-none">
              <div className={`card border-0 shadow-sm p-4 h-100 dashboard-card dashboard-card-${s.color}`}>
                <div className="d-flex align-items-center justify-content-between mb-3">
                  <div className={`p-3 bg-${s.color} bg-opacity-10 text-${s.color} rounded-3 d-flex align-items-center justify-content-center`} style={{ width: '48px', height: '48px' }}>
                    <i className={`bi ${s.icon} fs-4`}></i>
                  </div>
                  <h3 className={`fw-bold mb-0 text-dark`}>{s.value ?? 0}</h3>
                </div>
                <p className="text-muted mb-0 small fw-semibold text-uppercase tracking-wider">{s.label}</p>
              </div>
            </Link>
          </div>
        ))}
      </div>

      <div className="d-flex flex-wrap gap-3">
        <Link to="/jobs" className="btn btn-primary px-4 py-2.5 d-inline-flex align-items-center gap-2 shadow-sm">
          <i className="bi bi-search"></i> Browse Jobs
        </Link>
        <Link to="/jobseeker/profile" className="btn btn-outline-primary px-4 py-2.5 d-inline-flex align-items-center gap-2">
          <i className="bi bi-person-gear"></i> Edit Profile
        </Link>
        <Link to="/jobseeker/resume" className="btn btn-outline-secondary px-4 py-2.5 d-inline-flex align-items-center gap-2">
          <i className="bi bi-file-earmark-arrow-up"></i> Manage Resume
        </Link>
        <Link to="/jobseeker/ai/recommendations" className="btn btn-outline-warning px-4 py-2.5 d-inline-flex align-items-center gap-2">
          <i className="bi bi-stars"></i> AI Recommendations
        </Link>
        <Link to="/jobseeker/ai/resume" className="btn btn-outline-warning px-4 py-2.5 d-inline-flex align-items-center gap-2">
          <i className="bi bi-file-earmark-bar-graph"></i> AI Resume Analysis
        </Link>
      </div>
    </div>
  );
};

export default JobSeekerDashboard;

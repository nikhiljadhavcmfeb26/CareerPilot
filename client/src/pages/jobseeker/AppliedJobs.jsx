import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { applicationApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

const AppliedJobs = () => {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchApplications = () => {
    applicationApi.getMy().then(({ data }) => {
      if (data.success) setApplications(data.data);
    }).catch((err) => {
      toast.error(err.response?.data?.message || 'Could not load your applications');
    }).finally(() => setLoading(false));
  };

  useEffect(() => { fetchApplications(); }, []);

  const handleWithdraw = async (id) => {
    try {
      await applicationApi.withdraw(id);
      toast.success('Application withdrawn');
      fetchApplications();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to withdraw');
    }
  };

  if (loading) return <LoadingSpinner fullPage />;

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-4">Applied Jobs</h2>
      <div className="table-responsive">
        <table className="table table-hover bg-white shadow-sm">
          <thead className="table-primary">
            <tr><th>Job</th><th>Company</th><th>Applied</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {applications.length === 0 ? (
              <tr><td colSpan="5" className="text-center text-muted">No applications yet. <Link to="/jobs">Browse jobs</Link></td></tr>
            ) : applications.map((a) => (
              <tr key={a.id}>
                <td><Link to={`/jobs/${a.jobId}`}>{a.jobTitle}</Link></td>
                <td>{a.companyName}</td>
                <td>{new Date(a.appliedAt).toLocaleDateString()}</td>
                <td><span className={`badge bg-${a.status === 'Shortlisted' ? 'success' : a.status === 'Rejected' ? 'danger' : 'info'}`}>{a.status}</span></td>
                <td>
                  {a.status === 'Pending' && (
                    <button className="btn btn-sm btn-outline-warning" onClick={() => handleWithdraw(a.id)}>Withdraw</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AppliedJobs;

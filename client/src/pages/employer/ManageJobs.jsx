import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { jobApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

const ManageJobs = () => {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchJobs = () => {
    jobApi.getMy().then(({ data }) => {
      if (data.success) setJobs(data.data);
    }).catch((err) => {
      toast.error(err.response?.data?.message || 'Could not load your jobs');
    }).finally(() => setLoading(false));
  };

  useEffect(() => { fetchJobs(); }, []);

  const handleAction = async (action, id) => {
    try {
      await jobApi[action](id);
      toast.success(`Job ${action}d successfully`);
      fetchJobs();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  if (loading) return <LoadingSpinner fullPage />;

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2 className="fw-bold mb-0">Manage Jobs</h2>
        <Link to="/employer/jobs/create" className="btn btn-primary">+ New Job</Link>
      </div>
      <div className="table-responsive">
        <table className="table table-hover bg-white shadow-sm rounded">
          <thead className="table-primary">
            <tr><th>Title</th><th>Location</th><th>Status</th><th>Applications</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {jobs.map((job) => (
              <tr key={job.id}>
                <td>{job.title}</td>
                <td>{job.location}</td>
                <td><span className={`badge bg-${job.status === 'Published' ? 'success' : job.status === 'Closed' ? 'secondary' : 'warning'}`}>{job.status}</span></td>
                <td>
                  <Link to={`/employer/applicants/${job.id}`}>{job.applicationCount} view</Link>
                </td>
                <td>
                  {job.status === 'Draft' && <button className="btn btn-sm btn-success me-1" onClick={() => handleAction('publish', job.id)}>Publish</button>}
                  {job.status === 'Published' && <button className="btn btn-sm btn-warning me-1" onClick={() => handleAction('close', job.id)}>Close</button>}
                  <button className="btn btn-sm btn-danger" onClick={() => handleAction('delete', job.id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ManageJobs;

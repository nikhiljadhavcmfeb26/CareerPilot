import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { jobApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

const AdminJobs = () => {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchJobs = () => {
    jobApi.getAllAdmin().then(({ data }) => {
      if (data.success) setJobs(data.data);
    }).catch((err) => {
      toast.error(err.response?.data?.message || 'Could not load jobs');
    }).finally(() => setLoading(false));
  };

  useEffect(() => { fetchJobs(); }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this job?')) return;
    try {
      await jobApi.adminDelete(id);
      toast.success('Job deleted');
      fetchJobs();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to delete');
    }
  };

  if (loading) return <LoadingSpinner fullPage />;

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-4">Manage Jobs</h2>
      <div className="table-responsive">
        <table className="table table-hover bg-white shadow-sm">
          <thead className="table-primary">
            <tr><th>Title</th><th>Company</th><th>Location</th><th>Status</th><th>Applications</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {jobs.map((job) => (
              <tr key={job.id}>
                <td>{job.title}</td>
                <td>{job.companyName}</td>
                <td>{job.location}</td>
                <td><span className="badge bg-info">{job.status}</span></td>
                <td>{job.applicationCount}</td>
                <td>
                  <button className="btn btn-sm btn-danger" onClick={() => handleDelete(job.id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AdminJobs;

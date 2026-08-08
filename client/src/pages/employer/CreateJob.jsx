import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { jobApi } from '../../api/services';

const CreateJob = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    title: '', description: '', requirements: '', location: '',
    jobType: 0, salaryMin: '', salaryMax: '', experienceLevel: 0
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = { ...form, salaryMin: form.salaryMin ? Number(form.salaryMin) : null, salaryMax: form.salaryMax ? Number(form.salaryMax) : null };
      const { data } = await jobApi.create(payload);
      if (data.success) {
        toast.success('Job created! Publish it from Manage Jobs.');
        navigate('/employer/jobs');
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create job');
    }
  };

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-4">Create Job</h2>
      <div className="card border-0 shadow-sm p-4 col-lg-8">
        <form onSubmit={handleSubmit}>
          <div className="mb-3">
            <label className="form-label">Title</label>
            <input className="form-control" required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          </div>
          <div className="mb-3">
            <label className="form-label">Description</label>
            <textarea className="form-control" rows="4" required value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div className="mb-3">
            <label className="form-label">Requirements</label>
            <textarea className="form-control" rows="3" required value={form.requirements} onChange={(e) => setForm({ ...form, requirements: e.target.value })} />
          </div>
          <div className="row">
            <div className="col-md-6 mb-3">
              <label className="form-label">Location</label>
              <input className="form-control" required value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} />
            </div>
            <div className="col-md-6 mb-3">
              <label className="form-label">Job Type</label>
              <select className="form-select" value={form.jobType} onChange={(e) => setForm({ ...form, jobType: Number(e.target.value) })}>
                <option value={0}>Full Time</option><option value={1}>Part Time</option>
                <option value={2}>Contract</option><option value={3}>Remote</option><option value={4}>Internship</option>
              </select>
            </div>
          </div>
          <div className="row">
            <div className="col-md-4 mb-3">
              <label className="form-label">Min Salary (₹)</label>
              <input type="number" className="form-control" value={form.salaryMin} onChange={(e) => setForm({ ...form, salaryMin: e.target.value })} />
            </div>
            <div className="col-md-4 mb-3">
              <label className="form-label">Max Salary (₹)</label>
              <input type="number" className="form-control" value={form.salaryMax} onChange={(e) => setForm({ ...form, salaryMax: e.target.value })} />
            </div>
            <div className="col-md-4 mb-3">
              <label className="form-label">Experience</label>
              <select className="form-select" value={form.experienceLevel} onChange={(e) => setForm({ ...form, experienceLevel: Number(e.target.value) })}>
                <option value={0}>Entry</option><option value={1}>Mid</option>
                <option value={2}>Senior</option><option value={3}>Lead</option>
                <option value={4}>Executive</option>
              </select>
            </div>
          </div>
          <button type="submit" className="btn btn-primary">Create Job</button>
        </form>
      </div>
    </div>
  );
};

export default CreateJob;

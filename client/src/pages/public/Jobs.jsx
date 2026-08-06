import { useState, useEffect } from 'react';
import { jobApi } from '../../api/services';
import { toast } from 'react-toastify';
import JobCard from '../../components/JobCard';
import Pagination from '../../components/Pagination';
import LoadingSpinner from '../../components/LoadingSpinner';

const Jobs = () => {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ keyword: '', location: '', jobType: '', experienceLevel: '', page: 1 });
  const [pagination, setPagination] = useState({ totalPages: 1 });

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const params = { page: filters.page, pageSize: 9 };
      if (filters.keyword) params.keyword = filters.keyword;
      if (filters.location) params.location = filters.location;
      if (filters.jobType) params.jobType = filters.jobType;
      if (filters.experienceLevel) params.experienceLevel = filters.experienceLevel;
      const { data } = await jobApi.search(params);
      if (data.success) {
        setJobs(data.data.items);
        setPagination({ totalPages: data.data.totalPages });
      }
    } catch (err) {
      setJobs([]);
      toast.error(err.response?.data?.message || 'Could not load jobs');
    } finally { setLoading(false); }
  };

  useEffect(() => { fetchJobs(); }, [filters.page]);

  const handleSearch = (e) => {
    e.preventDefault();
    setFilters({ ...filters, page: 1 });
    fetchJobs();
  };

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-4">Browse Jobs</h2>
      <div className="card border-0 shadow-sm p-4 mb-4">
        <form onSubmit={handleSearch}>
          <div className="row g-3">
            <div className="col-md-4">
              <input className="form-control" placeholder="Job title or keyword"
                value={filters.keyword} onChange={(e) => setFilters({ ...filters, keyword: e.target.value })} />
            </div>
            <div className="col-md-3">
              <input className="form-control" placeholder="Location"
                value={filters.location} onChange={(e) => setFilters({ ...filters, location: e.target.value })} />
            </div>
            <div className="col-md-2">
              <select className="form-select" value={filters.jobType}
                onChange={(e) => setFilters({ ...filters, jobType: e.target.value })}>
                <option value="">Job Type</option>
                <option value="0">Full Time</option>
                <option value="1">Part Time</option>
                <option value="2">Contract</option>
                <option value="3">Remote</option>
                <option value="4">Internship</option>
              </select>
            </div>
            <div className="col-md-2">
              <select className="form-select" value={filters.experienceLevel}
                onChange={(e) => setFilters({ ...filters, experienceLevel: e.target.value })}>
                <option value="">Experience</option>
                <option value="0">Entry</option>
                <option value="1">Mid</option>
                <option value="2">Senior</option>
                <option value="3">Lead</option>
                <option value="4">Executive</option>
              </select>
            </div>
            <div className="col-md-1">
              <button type="submit" className="btn btn-primary w-100">Search</button>
            </div>
          </div>
        </form>
      </div>

      {loading ? <LoadingSpinner /> : (
        <>
          <div className="row g-4">
            {jobs.length === 0 ? (
              <div className="col-12 text-center text-muted py-5">No jobs found. Try different filters.</div>
            ) : jobs.map((job) => (
              <div key={job.id} className="col-md-6 col-lg-4"><JobCard job={job} /></div>
            ))}
          </div>
          <Pagination currentPage={filters.page} totalPages={pagination.totalPages}
            onPageChange={(page) => setFilters({ ...filters, page })} />
        </>
      )}
    </div>
  );
};

export default Jobs;

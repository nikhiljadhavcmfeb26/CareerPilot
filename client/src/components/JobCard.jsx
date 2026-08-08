import { Link } from 'react-router-dom';

const JobCard = ({ job }) => (
  <div className="card job-card h-100 shadow-sm border-0">
    <div className="card-body">
      <div className="d-flex justify-content-between align-items-start">
        <div>
          <h5 className="card-title mb-1">
            <Link to={`/jobs/${job.id}`} className="text-decoration-none text-dark">{job.title}</Link>
          </h5>
          <p className="text-primary mb-1 fw-semibold">{job.companyName}</p>
        </div>
        <span className="badge bg-light text-dark">{job.jobType}</span>
      </div>
      <p className="text-muted small mb-2">
        <i className="bi bi-geo-alt"></i> {job.location}
      </p>
      {job.salaryMin && (
        <p className="text-success small mb-2">
          ₹{(job.salaryMin / 100000).toFixed(1)}L - ₹{(job.salaryMax / 100000).toFixed(1)}L
        </p>
      )}
      <p className="card-text text-muted small text-truncate-3">{job.description?.substring(0, 120)}...</p>
      <div className="d-flex justify-content-between align-items-center mt-3">
        <span className="badge bg-secondary">{job.experienceLevel}</span>
        <Link to={`/jobs/${job.id}`} className="btn btn-outline-primary btn-sm">View Details</Link>
      </div>
    </div>
  </div>
);

export default JobCard;

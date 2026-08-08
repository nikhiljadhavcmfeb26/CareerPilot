import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { applicationApi, openPdfResponse } from '../../api/services';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../../components/LoadingSpinner';

const Applicants = () => {
  const { jobId } = useParams();
  const { isPremium } = useAuth();
  const [applicants, setApplicants] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchApplicants = () => {
    applicationApi.getByJob(jobId).then(({ data }) => {
      if (data.success) setApplicants(data.data);
    }).catch((err) => {
      toast.error(err.response?.data?.message || 'Could not load applicants');
    }).finally(() => setLoading(false));
  };

  useEffect(() => { fetchApplicants(); }, [jobId]);

  /**
   * The backend endpoint for this has existed since the migration but had no
   * button wired to it. application-service checks that this employer owns the
   * job before user-service is ever asked for the file.
   */
  const viewResume = async (application) => {
    try {
      const response = await applicationApi.downloadResume(application.id);
      openPdfResponse(response, application.resumeFileName || 'resume.pdf');
    } catch (err) {
      // A blob-typed error body has to be read back as text before the
      // server's JSON message is visible.
      let message = 'Could not open the resume';
      try {
        const text = await err.response?.data?.text?.();
        if (text) message = JSON.parse(text).message || message;
      } catch { /* keep the fallback */ }
      toast.error(message);
    }
  };

  const updateStatus = async (id, status) => {
    try {
      await applicationApi.updateStatus(id, status);
      toast.success('Status updated');
      fetchApplicants();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update');
    }
  };

  if (loading) return <LoadingSpinner fullPage />;

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4">
        <h2 className="fw-bold mb-0">Applicants</h2>
        <Link to={`/employer/ai/screening/${jobId}`} className="btn btn-outline-primary">
          <i className="bi bi-stars me-2"></i>AI Screening
          {!isPremium && <span className="badge bg-warning text-dark ms-2">Premium</span>}
        </Link>
      </div>

      {/*
        AI rejection feedback is triggered automatically by application-service
        when a status is set to Rejected - there is no button for it and no
        endpoint for the browser to call. Surfacing it here is the integration:
        the employer needs to know that this action emails the candidate.
      */}
      <div className="alert alert-light border d-flex align-items-start gap-3 small">
        <i className="bi bi-info-circle text-primary fs-5"></i>
        <div>
          Setting a candidate to <strong>Rejected</strong> automatically emails them
          constructive feedback.
          {isPremium
            ? ' Because your subscription is active, that feedback is personalised by AI against their resume.'
            : ' Upgrade to Premium to have that feedback personalised by AI; otherwise a professional default message is sent.'}
          {!isPremium && <> <Link to="/premium" className="fw-semibold">Upgrade</Link></>}
        </div>
      </div>
      <div className="table-responsive">
        <table className="table table-hover bg-white shadow-sm">
          <thead className="table-primary">
            <tr><th>Name</th><th>Email</th><th>Applied</th><th>Resume</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {applicants.length === 0 ? (
              <tr><td colSpan="6" className="text-center text-muted">No applicants yet</td></tr>
            ) : applicants.map((a) => (
              <tr key={a.id}>
                <td>{a.applicantName}</td>
                <td>{a.applicantEmail}</td>
                <td>{new Date(a.appliedAt).toLocaleDateString()}</td>
                <td>
                  {a.resumeId ? (
                    <button className="btn btn-sm btn-outline-primary" onClick={() => viewResume(a)}>
                      View Resume
                    </button>
                  ) : (
                    <span className="text-muted small">Not attached</span>
                  )}
                </td>
                <td><span className="badge bg-info">{a.status}</span></td>
                <td>
                  <select className="form-select form-select-sm" style={{ width: '140px' }} value={a.status}
                    onChange={(e) => updateStatus(a.id, e.target.value)}
                    disabled={a.status === 'Withdrawn'}>
                    <option value="Pending">Pending</option>
                    <option value="Reviewed">Reviewed</option>
                    <option value="Shortlisted">Shortlisted</option>
                    <option value="Rejected">Rejected</option>
                    <option value="Accepted">Accepted</option>
                    {a.status === 'Withdrawn' && <option value="Withdrawn">Withdrawn</option>}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Applicants;

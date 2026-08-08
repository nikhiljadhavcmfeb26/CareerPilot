import { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import { companyApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

/**
 * ADMIN MODULE - employer management.
 *
 * Previously this page could only ever see the pending queue, which made
 * approval a one-way door: once granted there was no way to take it back
 * without editing the database. It now lists every registered employer and
 * supports revoking an approval, which stops that company publishing new jobs
 * (JobServiceImpl.publishJob checks the flag) without touching jobs already
 * live - remove those individually from Manage Jobs.
 */
const AdminEmployers = () => {
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [showPendingOnly, setShowPendingOnly] = useState(false);

  const fetchCompanies = useCallback(() => {
    setLoading(true);
    const request = showPendingOnly ? companyApi.getPending() : companyApi.getAllAdmin();
    request
      .then(({ data }) => {
        if (data.success) setCompanies(data.data);
      })
      .catch((err) => toast.error(err.response?.data?.message || 'Could not load employers'))
      .finally(() => setLoading(false));
  }, [showPendingOnly]);

  useEffect(() => { fetchCompanies(); }, [fetchCompanies]);

  const run = async (id, action, successMessage) => {
    setBusyId(id);
    try {
      await action();
      toast.success(successMessage);
      fetchCompanies();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    } finally {
      setBusyId(null);
    }
  };

  const approve = (c) => run(c.id, () => companyApi.approve(c.id), 'Employer approved');

  const revoke = (c) => {
    if (!window.confirm(`Revoke approval for ${c.name}? They will not be able to publish new jobs.`)) return undefined;
    return run(c.id, () => companyApi.revokeApproval(c.id), 'Approval revoked');
  };

  return (
    <div className="container py-4">
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
        <h2 className="fw-bold mb-0">Manage Employers</h2>
        <div className="form-check form-switch">
          <input
            className="form-check-input"
            type="checkbox"
            id="pendingOnly"
            checked={showPendingOnly}
            onChange={() => setShowPendingOnly((v) => !v)}
          />
          <label className="form-check-label small" htmlFor="pendingOnly">Pending approval only</label>
        </div>
      </div>

      {loading ? <LoadingSpinner /> : companies.length === 0 ? (
        <p className="text-muted">{showPendingOnly ? 'No pending employer approvals.' : 'No employers registered yet.'}</p>
      ) : (
        <div className="row g-4">
          {companies.map((c) => (
            <div key={c.id} className="col-md-6">
              <div className="card border-0 shadow-sm p-4 h-100">
                <div className="d-flex justify-content-between align-items-start mb-2">
                  <h5 className="fw-bold mb-0">{c.name}</h5>
                  <span className={`badge bg-${c.isApproved ? 'success' : 'warning text-dark'}`}>
                    {c.isApproved ? 'Approved' : 'Pending'}
                  </span>
                </div>
                <p className="text-muted small mb-2">
                  {[c.industry, c.location].filter(Boolean).join(' · ') || 'No industry/location on file'}
                </p>
                <p className="flex-grow-1 small">
                  {c.description ? `${c.description.substring(0, 180)}${c.description.length > 180 ? '...' : ''}` : 'No description provided.'}
                </p>
                <div className="d-flex gap-2">
                  {c.isApproved ? (
                    <button className="btn btn-outline-danger btn-sm" disabled={busyId === c.id} onClick={() => revoke(c)}>
                      Revoke approval
                    </button>
                  ) : (
                    <button className="btn btn-success btn-sm" disabled={busyId === c.id} onClick={() => approve(c)}>
                      Approve
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default AdminEmployers;

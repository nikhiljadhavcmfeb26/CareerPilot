import { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import { adminApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

const STATUS_COLORS = { PAID: 'success', CREATED: 'secondary', FAILED: 'danger' };

/**
 * ADMIN MODULE - subscription management.
 *
 * "Revoke" expires the subscription rather than deleting the row: the payment
 * really happened and the record of it is not the admin's to erase. "Extend"
 * adds days from whichever is later - today or the current expiry - so a
 * customer with time remaining never silently loses it.
 */
const AdminSubscriptions = () => {
  const [subscriptions, setSubscriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [status, setStatus] = useState('');

  const fetchSubscriptions = useCallback(() => {
    setLoading(true);
    adminApi.getSubscriptions({ status: status || undefined })
      .then(({ data }) => {
        if (data.success) setSubscriptions(data.data);
      })
      .catch((err) => toast.error(err.response?.data?.message || 'Could not load subscriptions'))
      .finally(() => setLoading(false));
  }, [status]);

  useEffect(() => { fetchSubscriptions(); }, [fetchSubscriptions]);

  const run = async (id, action, successMessage) => {
    setBusyId(id);
    try {
      await action();
      toast.success(successMessage);
      fetchSubscriptions();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    } finally {
      setBusyId(null);
    }
  };

  const revoke = (s) => {
    if (!window.confirm(`Revoke Premium for ${s.userEmail}? Their AI features stop immediately.`)) return undefined;
    return run(s.id, () => adminApi.revokeSubscription(s.id), 'Subscription revoked');
  };

  const extend = (s) => {
    const input = window.prompt('Extend by how many days?', '30');
    if (input === null) return undefined;
    const days = Number.parseInt(input, 10);
    if (!Number.isInteger(days) || days < 1 || days > 730) {
      toast.error('Enter a whole number of days between 1 and 730.');
      return undefined;
    }
    return run(s.id, () => adminApi.extendSubscription(s.id, days), `Extended by ${days} days`);
  };

  return (
    <div className="container py-4">
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
        <h2 className="fw-bold mb-0">Subscription Management</h2>
        <span className="text-muted small">{subscriptions.length} record(s)</span>
      </div>

      <div className="row g-2 mb-3">
        <div className="col-md-3">
          <select className="form-select" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All payment statuses</option>
            <option value="PAID">Paid</option>
            <option value="CREATED">Created (unpaid)</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>
      </div>

      {loading ? <LoadingSpinner /> : (
        <div className="table-responsive">
          <table className="table table-hover align-middle bg-white shadow-sm">
            <thead className="table-primary">
              <tr>
                <th>Subscriber</th><th>Role</th><th>Plan</th><th>Payment</th>
                <th>Amount</th><th>Expires</th><th>Active</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {subscriptions.length === 0 && (
                <tr><td colSpan="8" className="text-center text-muted py-4">No subscriptions found.</td></tr>
              )}
              {subscriptions.map((s) => (
                <tr key={s.id}>
                  <td>
                    <div className="fw-semibold">{s.userName}</div>
                    <div className="text-muted small">{s.userEmail}</div>
                  </td>
                  <td><span className="badge bg-secondary">{s.role}</span></td>
                  <td>{s.planName}</td>
                  <td><span className={`badge bg-${STATUS_COLORS[s.paymentStatus] || 'secondary'}`}>{s.paymentStatus}</span></td>
                  <td>{s.amount != null ? `₹${s.amount}` : '-'}</td>
                  <td>{s.expiryDate ? new Date(s.expiryDate).toLocaleDateString() : '-'}</td>
                  <td>
                    <span className={`badge bg-${s.active ? 'success' : 'light text-muted'}`}>
                      {s.active ? 'Yes' : 'No'}
                    </span>
                  </td>
                  <td>
                    <div className="d-flex flex-wrap gap-1">
                      <button
                        className="btn btn-sm btn-outline-primary"
                        disabled={busyId === s.id || s.paymentStatus !== 'PAID'}
                        onClick={() => extend(s)}
                      >
                        Extend
                      </button>
                      <button
                        className="btn btn-sm btn-outline-danger"
                        disabled={busyId === s.id || !s.active}
                        onClick={() => revoke(s)}
                      >
                        Revoke
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default AdminSubscriptions;

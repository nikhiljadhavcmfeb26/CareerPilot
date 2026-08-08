import { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import { adminApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

/**
 * ADMIN MODULE - user management.
 *
 * "Deactivate" and "Block" are separate actions because they mean different
 * things server-side: deactivation is a reversible administrative suspension,
 * blocking is a security action that records a reason and kills the live
 * session. The AI column is a third, independent axis - an admin can revoke
 * AI from an account without touching either flag or the subscription.
 */
const AdminUsers = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [role, setRole] = useState('');
  const [search, setSearch] = useState('');

  const fetchUsers = useCallback(() => {
    setLoading(true);
    adminApi.getUsers({ role: role || undefined, search: search || undefined })
      .then(({ data }) => {
        if (data.success) setUsers(data.data);
      })
      .catch((err) => toast.error(err.response?.data?.message || 'Could not load users'))
      .finally(() => setLoading(false));
  }, [role, search]);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  // Every mutation funnels through here so one place handles the busy state,
  // the toast, and the refresh - the previous version duplicated all three.
  const run = async (id, action, successMessage) => {
    setBusyId(id);
    try {
      await action();
      toast.success(successMessage);
      fetchUsers();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    } finally {
      setBusyId(null);
    }
  };

  const toggleActive = (u) => run(
    u.id,
    () => (u.isActive ? adminApi.deactivateUser(u.id) : adminApi.activateUser(u.id)),
    u.isActive ? 'User deactivated' : 'User activated'
  );

  const toggleBlock = (u) => {
    if (u.isBlocked) {
      return run(u.id, () => adminApi.unblockUser(u.id), 'User unblocked');
    }
    const reason = window.prompt('Reason for blocking this account?', 'Suspicious activity');
    if (reason === null) return undefined;
    return run(u.id, () => adminApi.blockUser(u.id, reason), 'User blocked');
  };

  const toggleAi = (u) => run(
    u.id,
    () => (u.aiEnabled ? adminApi.disableUserAi(u.id) : adminApi.enableUserAi(u.id)),
    u.aiEnabled ? 'AI access revoked' : 'AI access restored'
  );

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-4">Manage Users</h2>

      <div className="row g-2 mb-3">
        <div className="col-md-3">
          <select className="form-select" value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="">All roles</option>
            <option value="JobSeeker">Job Seekers</option>
            <option value="Employer">Employers</option>
            <option value="Admin">Admins</option>
          </select>
        </div>
        <div className="col-md-5">
          <input
            className="form-control"
            placeholder="Search by name or email"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {loading ? <LoadingSpinner /> : (
        <div className="table-responsive">
          <table className="table table-hover align-middle bg-white shadow-sm">
            <thead className="table-primary">
              <tr>
                <th>Name</th><th>Email</th><th>Role</th><th>Status</th>
                <th>Premium</th><th>AI</th><th>Joined</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 && (
                <tr><td colSpan="8" className="text-center text-muted py-4">No users match these filters.</td></tr>
              )}
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.firstName} {u.lastName}</td>
                  <td>{u.email}</td>
                  <td><span className="badge bg-secondary">{u.role}</span></td>
                  <td>
                    {u.isBlocked ? (
                      <span className="badge bg-danger" title={u.blockedReason || ''}>Blocked</span>
                    ) : (
                      <span className={`badge bg-${u.isActive ? 'success' : 'warning text-dark'}`}>
                        {u.isActive ? 'Active' : 'Inactive'}
                      </span>
                    )}
                  </td>
                  <td>
                    {u.role === 'Admin'
                      ? <span className="text-muted small">n/a</span>
                      : <span className={`badge bg-${u.premium ? 'warning text-dark' : 'light text-muted'}`}>
                          {u.premium ? 'Premium' : 'Free'}
                        </span>}
                  </td>
                  <td>
                    <span className={`badge bg-${u.aiEnabled ? 'info' : 'secondary'}`}>
                      {u.aiEnabled ? 'On' : 'Off'}
                    </span>
                  </td>
                  <td>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-'}</td>
                  <td>
                    <div className="d-flex flex-wrap gap-1">
                      <button
                        className={`btn btn-sm ${u.isActive ? 'btn-outline-warning' : 'btn-outline-success'}`}
                        disabled={busyId === u.id}
                        onClick={() => toggleActive(u)}
                      >
                        {u.isActive ? 'Deactivate' : 'Activate'}
                      </button>
                      <button
                        className={`btn btn-sm ${u.isBlocked ? 'btn-outline-success' : 'btn-outline-danger'}`}
                        disabled={busyId === u.id}
                        onClick={() => toggleBlock(u)}
                      >
                        {u.isBlocked ? 'Unblock' : 'Block'}
                      </button>
                      <button
                        className="btn btn-sm btn-outline-dark"
                        disabled={busyId === u.id}
                        onClick={() => toggleAi(u)}
                      >
                        {u.aiEnabled ? 'Disable AI' : 'Enable AI'}
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

export default AdminUsers;

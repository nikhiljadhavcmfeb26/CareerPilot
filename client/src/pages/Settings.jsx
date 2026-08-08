import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../api/services';
import { toast } from 'react-toastify';

/**
 * Account settings, shared by every role (JobSeeker, Employer, Admin) - the
 * three /settings routes in App.jsx all render this one component.
 *
 * Scope is deliberately narrow: the registered name and email (read-only, they
 * are edited from the role's own profile page) plus a real password change.
 * The old Preferences tab and the 2FA switch were removed because neither was
 * ever persisted - both were local state that reset on refresh - so they
 * promised settings the platform did not actually honour.
 */
const Settings = () => {
  const { user } = useAuth();
  const [loading, setLoading] = useState(false);

  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      toast.error('New passwords do not match.');
      return;
    }
    if (passwordForm.newPassword.length < 6) {
      toast.error('New password must be at least 6 characters.');
      return;
    }

    setLoading(true);
    try {
      const { data } = await userApi.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword
      });
      if (data.success) {
        toast.success(data.message || 'Password changed successfully');
        setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not change your password');
    } finally {
      setLoading(false);
    }
  };

  const fullName = user?.firstName ? `${user.firstName} ${user.lastName}` : user?.role;

  return (
    <div className="container py-4">
      <div className="row">
        <div className="col-12">
          <h2 className="fw-bold mb-1">Account Settings</h2>
          <p className="text-muted mb-4">Manage your account details and password.</p>
        </div>
      </div>

      <div className="row g-4">
        <div className="col-md-4">
          <div className="card border-0 shadow-sm p-3">
            <div className="d-flex align-items-center gap-3 p-2 mb-3 border-bottom">
              <div className="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center fw-bold" style={{ width: '50px', height: '50px', fontSize: '1.25rem' }}>
                {user?.firstName?.[0] || user?.role?.[0] || 'U'}
              </div>
              <div>
                <h5 className="mb-0 fw-bold">{fullName}</h5>
                <span className="badge bg-primary bg-opacity-10 text-primary small px-2 py-1">{user?.role}</span>
              </div>
            </div>
            <div className="nav flex-column nav-pills">
              <span className="nav-link active text-start py-2.5 px-3 d-flex align-items-center gap-2">
                <i className="bi bi-shield-lock"></i> Security
              </span>
            </div>
          </div>
        </div>

        <div className="col-md-8">
          <div className="card border-0 shadow-sm p-4">
            <h4 className="fw-bold mb-4 text-dark">Security</h4>

            <div className="row g-3 mb-4">
              <div className="col-sm-6">
                <label className="form-label fw-semibold">Name</label>
                <input type="text" className="form-control" value={fullName || ''} readOnly disabled />
              </div>
              <div className="col-sm-6">
                <label className="form-label fw-semibold">Email</label>
                <input type="email" className="form-control" value={user?.email || ''} readOnly disabled />
              </div>
            </div>

            <hr className="my-4 text-muted opacity-25" />

            <h6 className="fw-bold mb-3">Change Password</h6>
            <form onSubmit={handlePasswordSubmit}>
              <div className="mb-3">
                <label className="form-label fw-semibold">Current Password</label>
                <input
                  type="password"
                  className="form-control"
                  required
                  value={passwordForm.currentPassword}
                  onChange={e => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                  disabled={loading}
                />
              </div>
              <div className="mb-3">
                <label className="form-label fw-semibold">New Password</label>
                <input
                  type="password"
                  className="form-control"
                  required
                  value={passwordForm.newPassword}
                  onChange={e => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                  disabled={loading}
                />
              </div>
              <div className="mb-4">
                <label className="form-label fw-semibold">Confirm New Password</label>
                <input
                  type="password"
                  className="form-control"
                  required
                  value={passwordForm.confirmPassword}
                  onChange={e => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                  disabled={loading}
                />
              </div>

              <button type="submit" className="btn btn-primary px-4" disabled={loading}>
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                    Updating Password...
                  </>
                ) : (
                  'Update Password'
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Settings;

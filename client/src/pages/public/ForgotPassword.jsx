import { useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { authApi } from '../../api/services';

const ForgotPassword = () => {
  const [step, setStep] = useState(1); // 1 = Request OTP, 2 = Verify OTP & Reset, 3 = Success
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRequestOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await authApi.forgotPassword(email);
      if (data.success) {
        toast.success(data.message || 'OTP sent successfully! Please check your email.');
        setStep(2);
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to send OTP.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      toast.error('Passwords do not match.');
      return;
    }
    if (newPassword.length < 6) {
      toast.error('Password must be at least 6 characters.');
      return;
    }
    setLoading(true);
    try {
      const { data } = await authApi.resetPassword({
        email,
        otp,
        newPassword
      });
      if (data.success) {
        toast.success(data.message || 'Password reset successful! You can now log in.');
        setStep(3);
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to reset password. Please check your OTP.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-md-5">
          <div className="card border-0 shadow p-4">
            <div className="text-center mb-4">
              <div className="d-inline-flex align-items-center justify-content-center bg-primary bg-opacity-10 text-primary rounded-circle mb-3" style={{ width: '60px', height: '60px' }}>
                <i className="bi bi-key-fill fs-3"></i>
              </div>
              <h2 className="fw-bold">
                {step === 1 && 'Forgot Password?'}
                {step === 2 && 'Reset Password'}
                {step === 3 && 'Success!'}
              </h2>
              <p className="text-muted mb-0">
                {step === 1 && "Enter your email and we'll send you an OTP to reset your password."}
                {step === 2 && `An OTP has been sent to ${email}. Enter details below.`}
                {step === 3 && 'Your password has been successfully reset.'}
              </p>
            </div>

            {step === 1 && (
              <form onSubmit={handleRequestOtp}>
                <div className="mb-4">
                  <label className="form-label fw-semibold">Email Address</label>
                  <div className="input-group">
                    <span className="input-group-text bg-light border-end-0">
                      <i className="bi bi-envelope text-muted"></i>
                    </span>
                    <input
                      type="email"
                      className="form-control border-start-0 ps-0"
                      placeholder="name@example.com"
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      disabled={loading}
                    />
                  </div>
                </div>
                
                <button type="submit" className="btn btn-primary w-100 py-2 fw-semibold" disabled={loading}>
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Sending OTP...
                    </>
                  ) : (
                    'Send OTP'
                  )}
                </button>
                
                <div className="text-center mt-4">
                  <Link to="/login" className="text-decoration-none text-primary fw-semibold">
                    <i className="bi bi-arrow-left me-1"></i> Back to Login
                  </Link>
                </div>
              </form>
            )}

            {step === 2 && (
              <form onSubmit={handleResetPassword}>
                <div className="mb-3">
                  <label className="form-label fw-semibold">6-Digit OTP</label>
                  <div className="input-group">
                    <span className="input-group-text bg-light border-end-0">
                      <i className="bi bi-shield-check text-muted"></i>
                    </span>
                    <input
                      type="text"
                      className="form-control border-start-0 ps-0"
                      placeholder="Enter OTP"
                      required
                      maxLength="6"
                      value={otp}
                      onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                      disabled={loading}
                    />
                  </div>
                </div>

                <div className="mb-3">
                  <label className="form-label fw-semibold">New Password</label>
                  <div className="input-group">
                    <span className="input-group-text bg-light border-end-0">
                      <i className="bi bi-lock text-muted"></i>
                    </span>
                    <input
                      type="password"
                      className="form-control border-start-0 ps-0"
                      placeholder="At least 6 characters"
                      required
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      disabled={loading}
                    />
                  </div>
                </div>

                <div className="mb-4">
                  <label className="form-label fw-semibold">Confirm Password</label>
                  <div className="input-group">
                    <span className="input-group-text bg-light border-end-0">
                      <i className="bi bi-lock-fill text-muted"></i>
                    </span>
                    <input
                      type="password"
                      className="form-control border-start-0 ps-0"
                      placeholder="Re-enter password"
                      required
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      disabled={loading}
                    />
                  </div>
                </div>
                
                <button type="submit" className="btn btn-primary w-100 py-2 fw-semibold" disabled={loading}>
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Resetting Password...
                    </>
                  ) : (
                    'Reset Password'
                  )}
                </button>

                <div className="text-center mt-3">
                  <button type="button" className="btn btn-link text-decoration-none text-primary fw-semibold p-0" onClick={() => setStep(1)} disabled={loading}>
                    <i className="bi bi-arrow-left me-1"></i> Resend OTP / Change Email
                  </button>
                </div>
              </form>
            )}

            {step === 3 && (
              <div className="text-center py-3">
                <div className="alert alert-success border-0 bg-success bg-opacity-10 text-success p-3 rounded-3 mb-4">
                  <i className="bi bi-check-circle-fill me-2 fs-5 align-middle"></i>
                  Password has been reset successfully!
                </div>
                <Link to="/login" className="btn btn-primary w-100 py-2 fw-semibold">
                  Go to Login <i className="bi bi-arrow-right ms-2"></i>
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;

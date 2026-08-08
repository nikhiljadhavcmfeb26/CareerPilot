import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../../context/AuthContext';

const Login = () => {
  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const data = await login(form);
      if (data.success) {
        toast.success('Login successful!');
        const role = data.data.user.role;
        if (role === 'Admin') navigate('/admin/dashboard');
        else if (role === 'Employer') navigate('/employer/dashboard');
        else navigate('/jobseeker/dashboard');
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-md-5">
          <div className="card border-0 shadow p-4">
            <h2 className="fw-bold text-center mb-4">Login</h2>
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label className="form-label">Email</label>
                <input type="email" className="form-control" required value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </div>
              <div className="mb-3">
                <label className="form-label d-flex justify-content-between align-items-center">
                  Password
                  {/* <Link to="/forgot-password" style={{ fontSize: '0.875rem' }} className="text-decoration-none text-primary fw-semibold">Forgot Password?</Link> */}
                </label>
                <input type="password" className="form-control" required value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })} />
              </div>

              <button type="submit" className="btn btn-primary w-100" disabled={loading}>
                {loading ? 'Logging in...' : 'Login'}
              </button>
            </form>
            <p className="text-center mt-3 mb-0">
              Don't have an account? <Link to="/register">Register</Link><br></br>
              <Link to="/forgot-password" style={{ fontSize: '0.875rem' }} className="text-decoration-none text-primary fw-semibold">Forgot Password?</Link>
            </p>
            <hr />
            
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;

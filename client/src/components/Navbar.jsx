import { useState } from 'react';
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import PremiumBadge from './PremiumBadge';

const Navbar = () => {
  const { user, logout, isAuthenticated, isPremium } = useAuth();
  const navigate = useNavigate();
  const [isNavCollapsed, setIsNavCollapsed] = useState(true);

  const handleLogout = async () => {
    setIsNavCollapsed(true);
    await logout();
    navigate('/');
  };

  // Determine links based on role
  const getNavLinks = () => {
    if (!isAuthenticated) {
      return [
        { path: '/', label: 'Home', icon: 'bi-house-door' },
        { path: '/jobs', label: 'Jobs', icon: 'bi-briefcase' },
        { path: '/about', label: 'About Us', icon: 'bi-info-circle' },
        { path: '/contact', label: 'Contact', icon: 'bi-envelope' },
      ];
    }

    switch (user?.role) {
      case 'JobSeeker':
        return [
          { path: '/jobs', label: 'Jobs', icon: 'bi-briefcase' },
          { path: '/jobseeker/applied', label: 'Applied Jobs', icon: 'bi-check2-circle' },
          { path: '/jobseeker/saved', label: 'Saved Jobs', icon: 'bi-bookmark' },
          { path: '/jobseeker/dashboard', label: 'Dashboard', icon: 'bi-speedometer2' },
          { path: '/contact', label: 'Contact', icon: 'bi-envelope' },
        ];
      case 'Employer':
        return [
          { path: '/employer/dashboard', label: 'Dashboard', icon: 'bi-speedometer2' },
          { path: '/employer/jobs', label: 'Manage Jobs', icon: 'bi-list-task' },
          { path: '/employer/company', label: 'Company', icon: 'bi-building' },
        ];
      case 'Admin':
        return [
          { path: '/admin/dashboard', label: 'Dashboard', icon: 'bi-speedometer2' },
          { path: '/admin/users', label: 'Users', icon: 'bi-people' },
          { path: '/admin/employers', label: 'Employers', icon: 'bi-building-gear' },
          { path: '/admin/jobs', label: 'Jobs', icon: 'bi-briefcase' },
          { path: '/admin/applications', label: 'Applications', icon: 'bi-file-earmark-text' },
          { path: '/admin/subscriptions', label: 'Subscriptions', icon: 'bi-star' },
          { path: '/admin/ai', label: 'AI', icon: 'bi-robot' },
        ];
      default:
        return [];
    }
  };

  const navLinks = getNavLinks();

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-primary sticky-top shadow-sm py-2.5">
      <div className="container">
        {/* Brand */}
        <Link 
          className="navbar-brand fw-bold fs-4 d-flex align-items-center gap-2" 
          to={
            !isAuthenticated 
              ? '/' 
              : user?.role === 'Admin' 
              ? '/admin/dashboard' 
              : user?.role === 'Employer' 
              ? '/employer/dashboard' 
              : '/jobseeker/dashboard'
          } 
          onClick={() => setIsNavCollapsed(true)}
        >
          <i className="bi bi-compass-fill text-warning fs-3"></i>
          <span>
            <span className="text-warning">Career</span>Pilot
          </span>
        </Link>

        {/* Mobile Hamburger Button */}
        <button 
          className="navbar-toggler border-0 focus-none" 
          type="button" 
          onClick={() => setIsNavCollapsed(!isNavCollapsed)}
          aria-controls="navbarNav" 
          aria-expanded={!isNavCollapsed} 
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        {/* Navbar Collapse */}
        <div className={`collapse navbar-collapse ${isNavCollapsed ? '' : 'show'}`} id="navbarNav">
          {/* Main Links */}
          <ul className="navbar-nav me-auto mb-2 mb-lg-0 mt-2 mt-lg-0">
            {navLinks.map((link) => (
              <li className="nav-item" key={link.path}>
                <NavLink 
                  className={({ isActive }) => 
                    `nav-link px-3 py-2 d-flex align-items-center gap-2 ${isActive ? 'active fw-bold text-warning' : ''}`
                  } 
                  to={link.path}
                  onClick={() => setIsNavCollapsed(true)}
                  end={link.path === '/'}
                >
                  <i className={`bi ${link.icon} d-lg-none d-xl-inline`}></i>
                  {link.label}
                </NavLink>
              </li>
            ))}
          </ul>

          {/* Right Side Buttons/Profile */}
          <div className="navbar-nav align-items-lg-center">
            {isAuthenticated ? (
              <div className="nav-item dropdown mt-2 mt-lg-0">
                <a 
                  className="nav-link dropdown-toggle d-flex align-items-center gap-2 bg-white bg-opacity-10 rounded-pill px-3 py-1.5 border border-white border-opacity-10 text-white" 
                  href="#" 
                  role="button" 
                  data-bs-toggle="dropdown"
                  aria-expanded="false"
                >
                  <div className="bg-warning text-dark fw-bold rounded-circle d-flex align-items-center justify-content-center text-uppercase" style={{ width: '28px', height: '28px', fontSize: '0.8rem' }}>
                    {user?.firstName?.[0] || user?.role?.[0] || 'U'}
                  </div>
                  <span className="fw-semibold">
                    {user?.firstName ? `${user.firstName} ${user.lastName}` : user?.role}
                  </span>
                  <PremiumBadge className="ms-1" />
                </a>
                <ul className="dropdown-menu dropdown-menu-end shadow border-0 mt-2 p-2 rounded-3 animate slideIn">
                  {user?.role === 'JobSeeker' && (
                    <>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/jobseeker/profile" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-person text-primary fs-5"></i> Profile
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/jobseeker/resume" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-file-earmark-pdf text-primary fs-5"></i> Resume
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/jobseeker/settings" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-gear text-primary fs-5"></i> Settings
                        </Link>
                      </li>
                    </>
                  )}
                  {user?.role === 'Employer' && (
                    <>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/employer/company" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-building text-primary fs-5"></i> Company Profile
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/employer/settings" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-gear text-primary fs-5"></i> Settings
                        </Link>
                      </li>
                    </>
                  )}
                  {user?.role === 'Admin' && (
                    <>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/admin/dashboard" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-speedometer2 text-primary fs-5"></i> Dashboard
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/admin/settings" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-gear text-primary fs-5"></i> Settings
                        </Link>
                      </li>
                    </>
                  )}
                  {user?.role === 'Employer' && (
                    <>
                      <li><hr className="dropdown-divider text-muted opacity-25" /></li>
                      <li>
                        <h6 className="dropdown-header text-uppercase small fw-bold d-flex align-items-center gap-2">
                          <i className="bi bi-stars text-warning"></i> AI Tools
                        </h6>
                      </li>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/employer/ai/screening" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-person-check text-primary fs-5"></i> AI Candidate Screening
                        </Link>
                      </li>
                    </>
                  )}
                  {user?.role === 'JobSeeker' && (
                    <>
                      <li><hr className="dropdown-divider text-muted opacity-25" /></li>
                      <li>
                        <h6 className="dropdown-header text-uppercase small fw-bold d-flex align-items-center gap-2">
                          <i className="bi bi-stars text-warning"></i> AI Tools
                        </h6>
                      </li>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/jobseeker/ai/resume" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-file-earmark-bar-graph text-primary fs-5"></i> Resume Analysis
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/jobseeker/ai/cover-letter" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-pencil-square text-primary fs-5"></i> Cover Letter
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/jobseeker/ai/recommendations" onClick={() => setIsNavCollapsed(true)}>
                          <i className="bi bi-stars text-primary fs-5"></i> Job Recommendations
                        </Link>
                      </li>
                    </>
                  )}

                  {user?.role !== 'Admin' && (
                    <li>
                      <Link className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2" to="/premium" onClick={() => setIsNavCollapsed(true)}>
                        <i className="bi bi-star-fill text-warning fs-5"></i>
                        {isPremium ? 'Premium (active)' : 'Upgrade to Premium'}
                      </Link>
                    </li>
                  )}
                  <li><hr className="dropdown-divider text-muted opacity-25" /></li>
                  <li>
                    <button className="dropdown-item rounded-2 py-2 d-flex align-items-center gap-2 text-danger" onClick={handleLogout}>
                      <i className="bi bi-box-arrow-right fs-5"></i> Logout
                    </button>
                  </li>
                </ul>
              </div>
            ) : (
              <div className="d-flex flex-column flex-lg-row align-items-stretch align-items-lg-center gap-2 w-100 w-lg-auto mt-2 mt-lg-0">
                <Link className="btn btn-outline-light px-3 py-1.5 fw-semibold" to="/login" onClick={() => setIsNavCollapsed(true)}>
                  Login
                </Link>
                <Link className="btn btn-warning text-dark px-4 py-1.5 fw-semibold shadow-sm" to="/register" onClick={() => setIsNavCollapsed(true)}>
                  Register
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;

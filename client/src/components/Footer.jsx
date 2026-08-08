import { Link } from "react-router-dom";

const Footer = () => (
  <footer className="bg-dark text-light py-5 mt-auto">
    <div className="container">
      <div className="row">

        {/* Company */}
        <div className="col-md-4 mb-4">
          <h5 className="fw-bold">
            <span className="text-warning">Career</span>Pilot
          </h5>
          <p className="text-white-50">
            Your trusted job portal connecting talented professionals with top employers.
          </p>
        </div>

        {/* Quick Links */}
        <div className="col-md-4 mb-4">
          <h6 className="fw-bold text-white">Quick Links</h6>

          <ul className="list-unstyled">
            <li className="mb-2">
              <Link to="/jobs" className="text-white-50 text-decoration-none">
                Browse Jobs
              </Link>
            </li>

            <li className="mb-2">
              <Link to="/about" className="text-white-50 text-decoration-none">
                About Us
              </Link>
            </li>

            <li className="mb-2">
              <Link to="/contact" className="text-white-50 text-decoration-none">
                Contact
              </Link>
            </li>

            <li>
              <Link to="/register" className="text-white-50 text-decoration-none">
                Register
              </Link>
            </li>
          </ul>
        </div>

        {/* Contact */}
        <div className="col-md-4 mb-4">
          <h6 className="fw-bold text-white">Contact</h6>

          <p className="text-white-50 mb-1">
            Email: support@careerpilot.com
          </p>

          <p className="text-white-50 mb-1">
            Phone: +91 98765 43210
          </p>

          <p className="text-white-50">
            Bangalore, India
          </p>
        </div>

      </div>

      <hr className="border-secondary" />

      <p className="text-center text-white-50 mb-0">
        &copy; {new Date().getFullYear()} CareerPilot. All rights reserved.
      </p>
    </div>
  </footer>
);

export default Footer;
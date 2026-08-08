import { Link } from 'react-router-dom';

const Home = () => (
  <>
    <section className="hero-section text-white py-5">
      <div className="container py-5">
        <div className="row align-items-center">
          <div className="col-lg-7">
            <h1 className="display-4 fw-bold mb-3">Find Your Dream Job Today</h1>
            <p className="lead mb-4">CareerPilot connects talented professionals with top employers across India. Browse thousands of opportunities and take the next step in your career.</p>
            <div className="d-flex gap-3">
              <Link to="/jobs" className="btn btn-warning btn-lg px-4">Browse Jobs</Link>
              <Link to="/register" className="btn btn-outline-light btn-lg px-4">Get Started</Link>
            </div>
          </div>
          <div className="col-lg-5 d-none d-lg-block text-center">
            <div className="hero-stats p-4 rounded-4 bg-white bg-opacity-10">
              <div className="row text-center">
                <div className="col-4"><h2 className="fw-bold">500+</h2><small>Jobs</small></div>
                <div className="col-4"><h2 className="fw-bold">200+</h2><small>Companies</small></div>
                <div className="col-4"><h2 className="fw-bold">10K+</h2><small>Users</small></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section className="py-5 bg-light">
      <div className="container">
        <h2 className="text-center fw-bold mb-5">Why Choose CareerPilot?</h2>
        <div className="row g-4">
          {[
            { title: 'Smart Job Search', desc: 'Advanced filters to find the perfect match for your skills and location.' },
            { title: 'Verified Employers', desc: 'All employers are verified by our admin team for your safety.' },
            { title: 'Easy Applications', desc: 'Apply with one click using your saved resume and profile.' },
            { title: 'Career Dashboard', desc: 'Track applications, saved jobs, and get insights on your job search.' }
          ].map((item) => (
            <div key={item.title} className="col-md-6 col-lg-3">
              <div className="card h-100 border-0 shadow-sm text-center p-4">
                <h5 className="fw-bold text-primary">{item.title}</h5>
                <p className="text-muted small mb-0">{item.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>

    <section className="py-5">
      <div className="container text-center">
        <h2 className="fw-bold mb-3">Ready to Start?</h2>
        <p className="text-muted mb-4">Join thousands of professionals who found their dream jobs through CareerPilot.</p>
        <Link to="/register" className="btn btn-primary btn-lg px-5">Create Free Account</Link>
      </div>
    </section>
  </>
);

export default Home;

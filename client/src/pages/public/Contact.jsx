/**
 * Static contact page. The old version carried a "Send Message" form that was
 * never wired to a backend endpoint - it only ever fired a toast - so it has
 * been removed along with its state rather than left as a dead control.
 * Everything the page needs is now literal content, which is why there are no
 * hooks and no imports here.
 */
const Contact = () => (
  <div className="container py-5">
    <h1 className="fw-bold mb-4 text-center">Contact Us</h1>
    <div className="row justify-content-center">
      <div className="col-lg-6 col-md-8">
        <div className="card border-0 shadow-sm p-4 text-center">
          <h5 className="fw-bold">Get in Touch</h5>
          <p className="text-muted">
            Have questions or facing any issues? Feel free to reach out to us via email or phone.
          </p>
          <p className="mb-2">
            <i className="bi bi-envelope text-primary me-2"></i>
            <strong>Email:</strong> support@careerpilot.com
          </p>
          <p className="mb-2">
            <i className="bi bi-telephone text-primary me-2"></i>
            <strong>Phone:</strong> +91 98765 43210
          </p>
          <p className="mb-0">
            <i className="bi bi-geo-alt text-primary me-2"></i>
            <strong>Address:</strong> Pune, Maharashtra
          </p>
        </div>
      </div>
    </div>
  </div>
);

export default Contact;

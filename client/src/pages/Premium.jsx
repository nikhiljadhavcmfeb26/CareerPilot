import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { subscriptionApi } from '../api/services';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from '../components/LoadingSpinner';

const CHECKOUT_SRC = 'https://checkout.razorpay.com/v1/checkout.js';

/**
 * Loads Razorpay Checkout on demand rather than from index.html, so the
 * script is only fetched by users who actually open this page. Resolves
 * immediately if it is already on the page.
 */
const loadCheckoutScript = () =>
  new Promise((resolve, reject) => {
    if (window.Razorpay) {
      resolve();
      return;
    }
    const existing = document.querySelector(`script[src="${CHECKOUT_SRC}"]`);
    if (existing) {
      existing.addEventListener('load', () => resolve());
      existing.addEventListener('error', () => reject(new Error('checkout script failed')));
      return;
    }
    const script = document.createElement('script');
    script.src = CHECKOUT_SRC;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('checkout script failed'));
    document.body.appendChild(script);
  });

const FEATURES = [
  { icon: 'bi-file-earmark-text', label: 'AI resume analysis and improvement suggestions' },
  { icon: 'bi-pencil-square', label: 'AI cover letter generation, tailored per job' },
  { icon: 'bi-stars', label: 'Personalised AI job recommendations' },
  { icon: 'bi-people', label: 'AI candidate screening (employers)' },
  { icon: 'bi-chat-left-text', label: 'AI feedback for rejected candidates (employers)' }
];

const Premium = () => {
  const { user, subscription, refreshSubscription, isPremium } = useAuth();
  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    subscriptionApi.getPlan()
      .then(({ data }) => {
        if (data.success) setPlan(data.data);
      })
      .catch((err) => {
        toast.error(err.response?.data?.message || 'Could not load plan details');
      })
      .finally(() => setLoading(false));
  }, []);

  const formatDate = (value) => (value ? new Date(value).toLocaleDateString() : '—');

  const handleUpgrade = async () => {
    setProcessing(true);
    try {
      await loadCheckoutScript();

      // Step 1 - backend creates the Razorpay order and records a CREATED row.
      const { data: orderResponse } = await subscriptionApi.createOrder();
      if (!orderResponse.success) {
        toast.error(orderResponse.message || 'Could not start the payment');
        setProcessing(false);
        return;
      }

      const order = orderResponse.data;

      // Step 2 - Razorpay Checkout collects the payment in a hosted modal.
      const checkout = new window.Razorpay({
        key: order.razorpayKeyId,
        amount: order.amount,
        currency: order.currency,
        name: 'CareerPilot',
        description: 'Premium subscription',
        order_id: order.razorpayOrderId,
        prefill: {
          name: user ? `${user.firstName} ${user.lastName}` : '',
          email: user?.email || '',
          contact: user?.phone || ''
        },
        theme: { color: '#0d6efd' },
        modal: {
          ondismiss: () => {
            setProcessing(false);
            toast.info('Payment cancelled');
          }
        },
        // Step 3 - the signature goes back to the backend, which is the only
        // place that decides whether the payment was genuine. Nothing on this
        // page grants Premium; it only reports what the server confirmed.
        handler: async (response) => {
          try {
            const { data: verifyResponse } = await subscriptionApi.verifyPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature
            });

            if (verifyResponse.success) {
              await refreshSubscription();
              toast.success(verifyResponse.message || 'Premium activated');
            } else {
              toast.error(verifyResponse.message || 'Payment verification failed');
            }
          } catch (err) {
            toast.error(err.response?.data?.message || 'Payment verification failed');
          } finally {
            setProcessing(false);
          }
        }
      });

      checkout.on('payment.failed', (response) => {
        toast.error(response?.error?.description || 'Payment failed');
        setProcessing(false);
      });

      checkout.open();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not start the payment. Please try again.');
      setProcessing(false);
    }
  };

  if (loading) return <LoadingSpinner fullPage />;

  return (
    <div className="container py-5">
      <div className="text-center mb-5">
        <h2 className="fw-bold mb-2">CareerPilot Premium</h2>
        <p className="text-muted mb-0">Unlock every AI-powered feature on the platform.</p>
      </div>

      {isPremium && (
        <div className="alert alert-success border-0 shadow-sm d-flex align-items-center gap-3 mb-4">
          <i className="bi bi-star-fill text-warning fs-4"></i>
          <div>
            <strong>Your Premium subscription is active.</strong>
            <div className="small">Valid until {formatDate(subscription?.expiryDate)}.</div>
          </div>
        </div>
      )}

      <div className="row g-4 justify-content-center">
        <div className="col-lg-5">
          <div className="card border-0 shadow-sm h-100 p-4">
            <h5 className="fw-bold">Free</h5>
            <p className="text-muted small">Everything you need for a normal job search.</p>
            <h3 className="fw-bold mb-4">₹0</h3>
            <ul className="list-unstyled small mb-0">
              <li className="mb-2"><i className="bi bi-check2 text-success me-2"></i>Browse and search all jobs</li>
              <li className="mb-2"><i className="bi bi-check2 text-success me-2"></i>Apply with your resume</li>
              <li className="mb-2"><i className="bi bi-check2 text-success me-2"></i>Save jobs and track applications</li>
              <li className="mb-2"><i className="bi bi-check2 text-success me-2"></i>Post and manage jobs (employers)</li>
            </ul>
          </div>
        </div>

        <div className="col-lg-5">
          <div className="card border-warning border-2 shadow h-100 p-4 position-relative">
            <span className="badge bg-warning text-dark position-absolute top-0 start-50 translate-middle px-3 py-2">
              Recommended
            </span>

            <h5 className="fw-bold d-flex align-items-center gap-2">
              <i className="bi bi-star-fill text-warning"></i> Premium
            </h5>
            <p className="text-muted small">Everything in Free, plus the AI toolkit.</p>

            <h3 className="fw-bold mb-1">
              ₹{plan?.amount ?? '—'}
              <small className="text-muted fs-6 fw-normal"> / {plan?.durationDays ?? 30} days</small>
            </h3>
            <p className="text-muted small mb-4">One-time payment, no auto-renewal.</p>

            <ul className="list-unstyled small mb-4">
              {FEATURES.map((f) => (
                <li className="mb-2" key={f.label}>
                  <i className={`bi ${f.icon} text-warning me-2`}></i>{f.label}
                </li>
              ))}
            </ul>

            <button
              className="btn btn-warning text-dark fw-semibold w-100 py-2 mt-auto"
              onClick={handleUpgrade}
              disabled={processing || isPremium}
            >
              {isPremium ? 'Already Premium' : processing ? 'Processing…' : 'Upgrade to Premium'}
            </button>
          </div>
        </div>
      </div>

      {subscription && (
        <div className="row justify-content-center mt-5">
          <div className="col-lg-10">
            <div className="card border-0 shadow-sm p-4">
              <h6 className="fw-bold mb-3">Subscription status</h6>
              <div className="row small">
                <div className="col-md-3 mb-2">
                  <div className="text-muted">Plan</div>
                  <div className="fw-semibold">{subscription.planName || 'FREE'}</div>
                </div>
                <div className="col-md-3 mb-2">
                  <div className="text-muted">Payment status</div>
                  <div className="fw-semibold">{subscription.paymentStatus || '—'}</div>
                </div>
                <div className="col-md-3 mb-2">
                  <div className="text-muted">Started</div>
                  <div className="fw-semibold">{formatDate(subscription.startDate)}</div>
                </div>
                <div className="col-md-3 mb-2">
                  <div className="text-muted">Expires</div>
                  <div className="fw-semibold">{formatDate(subscription.expiryDate)}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Premium;

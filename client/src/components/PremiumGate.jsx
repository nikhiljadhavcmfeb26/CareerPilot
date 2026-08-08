import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from './LoadingSpinner';

/**
 * Wraps any Premium-only feature. Non-premium users see an upgrade prompt
 * instead of the feature.
 *
 * This is a convenience layer, NOT the security boundary: ai-service checks
 * the subscription itself on every request (AiServiceImpl.requirePremium),
 * so hiding the UI is about not offering something that would fail, not about
 * enforcement. The AI pages built in Phase 6 will be wrapped in this.
 */
const PremiumGate = ({ children, feature = 'This feature' }) => {
  const { loading, isPremium } = useAuth();

  if (loading) return <LoadingSpinner fullPage />;
  if (isPremium) return children;

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-lg-7">
          <div className="card border-0 shadow-sm text-center p-5">
            <div className="mb-3">
              <i className="bi bi-star-fill text-warning" style={{ fontSize: '2.5rem' }}></i>
            </div>
            <h3 className="fw-bold mb-2">{feature} is a Premium feature</h3>
            <p className="text-muted mb-4">
              Upgrade to Premium to unlock AI resume analysis, cover letter generation,
              candidate screening and personalised job recommendations.
            </p>
            <div>
              <Link to="/premium" className="btn btn-warning text-dark fw-semibold px-4 py-2">
                <i className="bi bi-arrow-up-circle me-2"></i>Upgrade to Premium
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PremiumGate;

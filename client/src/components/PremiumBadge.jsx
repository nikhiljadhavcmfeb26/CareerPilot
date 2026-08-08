import { useAuth } from '../context/AuthContext';

/**
 * Small "Premium" pill, shown wherever the user's plan is relevant.
 * Reads AuthContext's isPremium, which mirrors the backend's own gate
 * (paid AND not expired) - it never decides premium status on its own.
 */
const PremiumBadge = ({ className = '' }) => {
  const { isPremium } = useAuth();

  if (!isPremium) return null;

  return (
    <span
      className={`badge bg-warning text-dark fw-semibold d-inline-flex align-items-center gap-1 ${className}`}
      title="Premium subscription active"
    >
      <i className="bi bi-star-fill"></i> Premium
    </span>
  );
};

export default PremiumBadge;

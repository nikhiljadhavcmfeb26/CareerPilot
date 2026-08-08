import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { adminApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

/**
 * ADMIN MODULE - AI feature management.
 *
 * These switches are enforced server-side in auth-service
 * (AdminServiceImpl.checkAiAccess), which ai-service consults before every
 * Gemini call. Turning something off here takes effect on the very next
 * request - there is no cached copy anywhere.
 */
const FEATURES = [
  { key: 'resumeFeedbackEnabled', label: 'Resume analysis', hint: 'Job seeker - AI resume feedback and scoring' },
  { key: 'coverLetterEnabled', label: 'Cover letter generation', hint: 'Job seeker - tailored cover letters' },
  { key: 'jobRecommendationsEnabled', label: 'Job recommendations', hint: 'Job seeker - resume-to-job matching' },
  { key: 'candidateScreeningEnabled', label: 'Candidate screening', hint: 'Employer - AI applicant ranking' },
  { key: 'rejectionFeedbackEnabled', label: 'Rejection feedback', hint: 'Automatic constructive feedback email on rejection' }
];

const AdminAiSettings = () => {
  const [settings, setSettings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    adminApi.getAiSettings().then(({ data }) => {
      if (data.success) setSettings(data.data);
    }).catch((err) => {
      toast.error(err.response?.data?.message || 'Could not load AI settings');
    }).finally(() => setLoading(false));
  }, []);

  const toggle = (key) => setSettings((prev) => ({ ...prev, [key]: !prev[key] }));

  const save = async () => {
    setSaving(true);
    try {
      const { data } = await adminApi.updateAiSettings(settings);
      if (data.success) {
        setSettings(data.data);
        toast.success('AI settings updated');
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not save AI settings');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingSpinner fullPage />;
  if (!settings) return <div className="container py-4"><p className="text-muted">AI settings unavailable.</p></div>;

  const masterOff = !settings.aiEnabled;

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-1">AI Feature Management</h2>
      <p className="text-muted small mb-4">
        Controls apply platform-wide and take effect on the next request. To revoke AI from a single
        account instead, use the AI column on <strong>Manage Users</strong>.
      </p>

      <div className="card border-0 shadow-sm p-4 mb-4">
        <div className="form-check form-switch mb-2">
          <input
            className="form-check-input"
            type="checkbox"
            id="aiEnabled"
            checked={settings.aiEnabled}
            onChange={() => toggle('aiEnabled')}
          />
          <label className="form-check-label fw-bold" htmlFor="aiEnabled">
            AI features enabled (master switch)
          </label>
        </div>
        <p className="text-muted small mb-0">
          When off, every AI endpoint is refused for every user regardless of the switches below.
        </p>
      </div>

      <div className="card border-0 shadow-sm p-4 mb-4">
        <h5 className="fw-bold mb-3">Individual features</h5>
        {FEATURES.map((f) => (
          <div className="form-check form-switch mb-3" key={f.key}>
            <input
              className="form-check-input"
              type="checkbox"
              id={f.key}
              disabled={masterOff}
              checked={settings[f.key]}
              onChange={() => toggle(f.key)}
            />
            <label className="form-check-label" htmlFor={f.key}>
              <span className="fw-semibold">{f.label}</span>
              <span className="text-muted small d-block">{f.hint}</span>
            </label>
          </div>
        ))}
        {masterOff && <p className="text-warning small mb-0">Master switch is off - individual features are inactive.</p>}
      </div>

      <div className="card border-0 shadow-sm p-4 mb-4">
        <div className="form-check form-switch mb-2">
          <input
            className="form-check-input"
            type="checkbox"
            id="requirePremium"
            checked={settings.requirePremium}
            onChange={() => toggle('requirePremium')}
          />
          <label className="form-check-label fw-bold" htmlFor="requirePremium">
            Require an active Premium subscription
          </label>
        </div>
        <p className="text-muted small mb-0">
          Turning this off opens AI features to every logged-in user of the right role without changing
          anyone&apos;s subscription records. Intended for demos and incident response.
        </p>
      </div>

      <button className="btn btn-primary px-4" onClick={save} disabled={saving}>
        {saving ? 'Saving...' : 'Save AI settings'}
      </button>
    </div>
  );
};

export default AdminAiSettings;

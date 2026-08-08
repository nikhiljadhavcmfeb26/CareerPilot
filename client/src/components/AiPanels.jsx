/**
 * Small presentational pieces shared by every AI page, so the four pages stay
 * consistent and none of them re-implements a list, a score dial or an empty
 * state. No business logic lives here.
 */

/** Circular-ish score readout, coloured by band. */
export const ScoreBadge = ({ score, label = 'Score' }) => {
  const tone = score >= 75 ? 'success' : score >= 50 ? 'warning' : 'danger';
  return (
    <div className={`d-inline-flex flex-column align-items-center justify-content-center rounded-circle bg-${tone} bg-opacity-10 border border-${tone} border-2`}
      style={{ width: '96px', height: '96px' }}>
      <span className={`fw-bold text-${tone}`} style={{ fontSize: '1.75rem', lineHeight: 1 }}>{score ?? 0}</span>
      <small className="text-muted">{label}</small>
    </div>
  );
};

/** A titled list of AI-generated points. Renders nothing when the list is empty. */
export const AiList = ({ title, items, icon, tone = 'primary' }) => {
  if (!items || items.length === 0) return null;
  return (
    <div className="mb-4">
      <h6 className={`fw-bold d-flex align-items-center gap-2 text-${tone}`}>
        <i className={`bi ${icon}`}></i> {title}
      </h6>
      <ul className="list-group list-group-flush">
        {items.map((item, i) => (
          <li className="list-group-item px-0 py-2 border-0 d-flex gap-2" key={`${title}-${i}`}>
            <i className={`bi bi-dot text-${tone} fs-5 flex-shrink-0`}></i>
            <span className="small">{item}</span>
          </li>
        ))}
      </ul>
    </div>
  );
};

/** Shared "nothing here yet" / "run this first" panel. */
export const AiEmptyState = ({ icon = 'bi-stars', title, message, children }) => (
  <div className="card border-0 shadow-sm text-center p-5">
    <div className="mb-3">
      <i className={`bi ${icon} text-primary`} style={{ fontSize: '2.25rem' }}></i>
    </div>
    <h5 className="fw-bold mb-2">{title}</h5>
    <p className="text-muted mb-3">{message}</p>
    {children}
  </div>
);

/**
 * Gemini calls take several seconds, and candidate screening runs one per
 * applicant - a bare spinner reads as a hang, so this says what is happening
 * and roughly how long it takes.
 */
export const AiThinking = ({ message = 'Analysing with AI…', hint }) => (
  <div className="card border-0 shadow-sm text-center p-5">
    <div className="spinner-border text-primary mx-auto mb-3" role="status">
      <span className="visually-hidden">Loading</span>
    </div>
    <h6 className="fw-bold mb-1">{message}</h6>
    <p className="text-muted small mb-0">{hint || 'This usually takes a few seconds.'}</p>
  </div>
);

/** Page heading used by all AI pages. */
export const AiPageHeader = ({ icon, title, subtitle }) => (
  <div className="d-flex align-items-center gap-3 mb-4">
    <div className="bg-primary bg-opacity-10 text-primary rounded-3 d-flex align-items-center justify-content-center"
      style={{ width: '48px', height: '48px' }}>
      <i className={`bi ${icon} fs-4`}></i>
    </div>
    <div>
      <h2 className="fw-bold mb-0 d-flex align-items-center gap-2">
        {title}
        <span className="badge bg-warning text-dark align-middle" style={{ fontSize: '0.6rem' }}>PREMIUM</span>
      </h2>
      <p className="text-muted mb-0 small">{subtitle}</p>
    </div>
  </div>
);

/**
 * ai-service wraps every Gemini failure in a friendly BadRequestException
 * message ("Could not generate ... right now"), so surfacing the server's own
 * message is almost always more useful than a generic one.
 */
export const aiErrorMessage = (err, fallback) =>
  err?.response?.data?.message || fallback;

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { aiApi } from '../../../api/services';
import { AiEmptyState, AiThinking, AiPageHeader, aiErrorMessage } from '../../../components/AiPanels';

/**
 * ai-service compares the candidate's default resume against the 50 most
 * recently published jobs and returns matches >= 50, already sorted by
 * matchScore descending - so no client-side sorting or filtering is needed.
 */
const AiRecommendations = () => {
  const [recommendations, setRecommendations] = useState(null);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await aiApi.jobRecommendations();
      if (data.success) setRecommendations(data.data || []);
    } catch (err) {
      toast.error(aiErrorMessage(err, 'Could not generate recommendations right now'));
    } finally {
      setLoading(false);
    }
  };

  const tone = (score) => (score >= 75 ? 'success' : score >= 60 ? 'primary' : 'warning');

  return (
    <div className="container py-4">
      <AiPageHeader
        icon="bi-stars"
        title="AI Job Recommendations"
        subtitle="Open jobs matched against your resume, ranked by fit."
      />

      {loading ? (
        <AiThinking message="Matching you to open jobs…" hint="Gemini is comparing your resume against the latest published jobs." />
      ) : recommendations === null ? (
        <AiEmptyState
          icon="bi-stars"
          title="Find jobs that fit you"
          message="We'll compare your default resume against the newest published jobs and rank the best matches."
        >
          <button className="btn btn-primary px-4" onClick={load}>
            <i className="bi bi-stars me-2"></i>Get recommendations
          </button>
        </AiEmptyState>
      ) : recommendations.length === 0 ? (
        <AiEmptyState
          icon="bi-search"
          title="No strong matches right now"
          message="None of the currently published jobs scored highly against your resume. Try again after new jobs are posted, or strengthen your resume first."
        >
          <div className="d-flex gap-2 justify-content-center flex-wrap">
            <Link to="/jobseeker/ai/resume" className="btn btn-outline-primary px-4">Improve my resume</Link>
            <Link to="/jobs" className="btn btn-outline-secondary px-4">Browse all jobs</Link>
          </div>
        </AiEmptyState>
      ) : (
        <>
          <div className="row g-3">
            {recommendations.map((r) => (
              <div className="col-lg-6" key={r.jobId}>
                <div className="card border-0 shadow-sm h-100 p-4">
                  <div className="d-flex justify-content-between align-items-start gap-3 mb-2">
                    <div>
                      <h6 className="fw-bold mb-1">{r.jobTitle}</h6>
                      {r.companyName && <p className="text-muted small mb-0">{r.companyName}</p>}
                    </div>
                    <span className={`badge bg-${tone(r.matchScore)} flex-shrink-0`}>{r.matchScore}% match</span>
                  </div>
                  <div className="progress mb-3" style={{ height: '6px' }}>
                    <div className={`progress-bar bg-${tone(r.matchScore)}`} style={{ width: `${r.matchScore}%` }}></div>
                  </div>
                  {r.reasoning && <p className="small text-muted flex-grow-1">{r.reasoning}</p>}
                  <Link to={`/jobs/${r.jobId}`} className="btn btn-sm btn-outline-primary mt-auto align-self-start">
                    View job
                  </Link>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-4">
            <button className="btn btn-outline-primary" onClick={load}>
              <i className="bi bi-arrow-clockwise me-2"></i>Refresh recommendations
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default AiRecommendations;

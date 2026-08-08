import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { aiApi, jobApi } from '../../../api/services';
import { AiList, AiEmptyState, AiThinking, AiPageHeader, aiErrorMessage } from '../../../components/AiPanels';
import LoadingSpinner from '../../../components/LoadingSpinner';

/**
 * ai-service screens every applicant on the job one at a time (one Gemini call
 * per resume) and returns the list already sorted by matchScore descending, so
 * no client-side sorting is needed.
 *
 * Two behaviours worth knowing, both decided server-side:
 *  - applicants with no resume attached, and withdrawn applications, are
 *    skipped entirely;
 *  - one candidate failing to screen doesn't fail the batch, so a short list
 *    doesn't mean an error.
 *
 * Reachable two ways: from a job's applicant list (jobId in the URL) and from
 * the employer profile menu (no jobId), which first asks which job to screen.
 */
const RECOMMENDATION_STYLE = {
  HighlyRecommended: { tone: 'success', label: 'Highly recommended' },
  Recommended: { tone: 'primary', label: 'Recommended' },
  Consider: { tone: 'warning', label: 'Consider' },
  NotRecommended: { tone: 'secondary', label: 'Not recommended' }
};

const AiScreening = () => {
  const { jobId } = useParams();
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  // null = still loading; only fetched when the page was opened without a job.
  const [myJobs, setMyJobs] = useState(null);

  useEffect(() => {
    if (jobId) return undefined;

    let cancelled = false;
    jobApi.getMy()
      .then(({ data }) => {
        if (!cancelled) setMyJobs(data.success ? data.data || [] : []);
      })
      .catch((err) => {
        if (cancelled) return;
        toast.error(err.response?.data?.message || 'Could not load your jobs');
        setMyJobs([]);
      });

    return () => { cancelled = true; };
  }, [jobId]);

  const screen = async () => {
    setLoading(true);
    try {
      const { data } = await aiApi.candidateScreening(Number(jobId));
      if (data.success) setResults(data.data || []);
    } catch (err) {
      toast.error(aiErrorMessage(err, 'Could not screen candidates right now'));
    } finally {
      setLoading(false);
    }
  };

  if (!jobId) {
    return (
      <div className="container py-4">
        <AiPageHeader
          icon="bi-person-check"
          title="AI Candidate Screening"
          subtitle="Choose one of your jobs to score every applicant's resume against it."
        />

        {myJobs === null ? (
          <LoadingSpinner />
        ) : myJobs.length === 0 ? (
          <AiEmptyState
            icon="bi-briefcase"
            title="No jobs to screen yet"
            message="Screening runs against a job's applicants, so post a job first."
          >
            <Link to="/employer/jobs/create" className="btn btn-primary px-4">Post a job</Link>
          </AiEmptyState>
        ) : (
          <div className="list-group shadow-sm">
            {myJobs.map((job) => (
              <Link
                key={job.id}
                to={`/employer/ai/screening/${job.id}`}
                className="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-3"
              >
                <span>
                  <span className="fw-semibold d-block">{job.title}</span>
                  <small className="text-muted">{job.location}</small>
                </span>
                <span className="badge bg-primary rounded-pill">
                  {job.applicationCount} applicant{job.applicationCount === 1 ? '' : 's'}
                </span>
              </Link>
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="container py-4">
      <AiPageHeader
        icon="bi-person-check"
        title="AI Candidate Screening"
        subtitle="Every applicant's resume scored against this job's requirements."
      />

      <div className="mb-4">
        <Link to={`/employer/applicants/${jobId}`} className="btn btn-sm btn-outline-secondary">
          <i className="bi bi-arrow-left me-1"></i>Back to applicants
        </Link>
      </div>

      {loading ? (
        <AiThinking
          message="Screening candidates…"
          hint="One AI review per applicant, so this can take up to a minute on a busy job. Please keep this page open."
        />
      ) : results === null ? (
        <AiEmptyState
          icon="bi-person-check"
          title="Screen this job's applicants"
          message="Each applicant's resume is compared against the job description and requirements, then ranked by fit."
        >
          <button className="btn btn-primary px-4" onClick={screen}>
            <i className="bi bi-stars me-2"></i>Run screening
          </button>
        </AiEmptyState>
      ) : results.length === 0 ? (
        <AiEmptyState
          icon="bi-inbox"
          title="Nothing to screen"
          message="No applicant on this job has a resume attached, or the applications have been withdrawn. Screening only runs on applications with a resume."
        >
          <Link to={`/employer/applicants/${jobId}`} className="btn btn-outline-primary px-4">View applicants</Link>
        </AiEmptyState>
      ) : (
        <>
          <p className="text-muted small">
            {results.length} candidate{results.length === 1 ? '' : 's'} screened, best match first.
          </p>

          <div className="row g-3">
            {results.map((r) => {
              const style = RECOMMENDATION_STYLE[r.recommendation] || RECOMMENDATION_STYLE.Consider;
              return (
                <div className="col-lg-6" key={r.applicationId}>
                  <div className="card border-0 shadow-sm h-100 p-4">
                    <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                      <div>
                        <h6 className="fw-bold mb-1">{r.applicantName || 'Candidate'}</h6>
                        <span className={`badge bg-${style.tone}`}>{style.label}</span>
                      </div>
                      <div className="text-end flex-shrink-0">
                        <div className="fw-bold fs-4">{r.matchScore}%</div>
                        <small className="text-muted">overall match</small>
                      </div>
                    </div>

                    <div className="mb-3">
                      <div className="d-flex justify-content-between small text-muted mb-1">
                        <span>Skill match</span>
                        <span>{r.skillMatchPercentage}%</span>
                      </div>
                      <div className="progress" style={{ height: '6px' }}>
                        <div className={`progress-bar bg-${style.tone}`} style={{ width: `${r.skillMatchPercentage}%` }}></div>
                      </div>
                    </div>

                    <AiList title="Strengths" items={r.strengths} icon="bi-hand-thumbs-up" tone="success" />
                    <AiList title="Concerns" items={r.weaknesses} icon="bi-exclamation-triangle" tone="danger" />
                    <AiList title="Missing skills" items={r.missingSkills} icon="bi-plus-circle" tone="warning" />
                  </div>
                </div>
              );
            })}
          </div>

          <div className="mt-4">
            <button className="btn btn-outline-primary" onClick={screen}>
              <i className="bi bi-arrow-clockwise me-2"></i>Re-run screening
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default AiScreening;

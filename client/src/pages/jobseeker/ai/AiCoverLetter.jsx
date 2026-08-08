import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { aiApi, jobApi } from '../../../api/services';
import { AiEmptyState, AiThinking, AiPageHeader, aiErrorMessage } from '../../../components/AiPanels';

/**
 * ai-service's /api/ai/cover-letter takes only a jobId - it pairs the target
 * job with the candidate's DEFAULT resume server-side, so there is nothing
 * else to collect here.
 *
 * The job list is loaded through the existing jobApi.search rather than a new
 * endpoint. Arriving with ?jobId=<n> (e.g. from a job page) preselects it.
 */
const AiCoverLetter = () => {
  const [searchParams] = useSearchParams();
  const [jobs, setJobs] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [jobId, setJobId] = useState(searchParams.get('jobId') || '');
  const [coverLetter, setCoverLetter] = useState('');
  const [loadingJobs, setLoadingJobs] = useState(true);
  const [generating, setGenerating] = useState(false);

  const loadJobs = async (search = '') => {
    setLoadingJobs(true);
    try {
      const params = { page: 1, pageSize: 50 };
      if (search) params.keyword = search;
      const { data } = await jobApi.search(params);
      if (data.success) setJobs(data.data.items || []);
    } catch (err) {
      toast.error(aiErrorMessage(err, 'Could not load jobs'));
      setJobs([]);
    } finally {
      setLoadingJobs(false);
    }
  };

  useEffect(() => { loadJobs(); }, []);

  const generate = async () => {
    if (!jobId) {
      toast.error('Pick a job first');
      return;
    }
    setGenerating(true);
    try {
      const { data } = await aiApi.coverLetter(Number(jobId));
      if (data.success) setCoverLetter(data.data.coverLetter || '');
    } catch (err) {
      toast.error(aiErrorMessage(err, 'Could not generate a cover letter right now'));
    } finally {
      setGenerating(false);
    }
  };

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(coverLetter);
      toast.success('Cover letter copied');
    } catch {
      toast.error('Could not copy - select the text and copy manually');
    }
  };

  return (
    <div className="container py-4">
      <AiPageHeader
        icon="bi-pencil-square"
        title="Cover Letter Generator"
        subtitle="A tailored cover letter written from your resume and the job you pick."
      />

      <div className="card border-0 shadow-sm p-4 mb-4">
        <div className="row g-3 align-items-end">
          <div className="col-md-5">
            <label className="form-label fw-semibold small">Search jobs</label>
            <div className="input-group">
              <input
                className="form-control"
                placeholder="Title, skill, company…"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') loadJobs(keyword); }}
              />
              <button className="btn btn-outline-secondary" onClick={() => loadJobs(keyword)}>
                <i className="bi bi-search"></i>
              </button>
            </div>
          </div>

          <div className="col-md-5">
            <label className="form-label fw-semibold small">Job</label>
            <select className="form-select" value={jobId} onChange={(e) => setJobId(e.target.value)} disabled={loadingJobs}>
              <option value="">{loadingJobs ? 'Loading jobs…' : 'Select a job'}</option>
              {jobs.map((j) => (
                <option key={j.id} value={j.id}>
                  {j.title}{j.companyName ? ` — ${j.companyName}` : ''}
                </option>
              ))}
            </select>
          </div>

          <div className="col-md-2 d-grid">
            <button className="btn btn-primary" onClick={generate} disabled={generating || !jobId}>
              <i className="bi bi-stars me-2"></i>Generate
            </button>
          </div>
        </div>

        {!loadingJobs && jobs.length === 0 && (
          <p className="text-muted small mb-0 mt-3">
            No published jobs matched. <Link to="/jobs">Browse all jobs</Link>.
          </p>
        )}
      </div>

      {generating ? (
        <AiThinking message="Writing your cover letter…" hint="Gemini is reading your resume and the job description." />
      ) : coverLetter ? (
        <div className="card border-0 shadow-sm p-4">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h6 className="fw-bold mb-0">Your cover letter</h6>
            <button className="btn btn-sm btn-outline-primary" onClick={copy}>
              <i className="bi bi-clipboard me-1"></i>Copy
            </button>
          </div>
          <textarea
            className="form-control"
            rows="16"
            value={coverLetter}
            onChange={(e) => setCoverLetter(e.target.value)}
          />
          <p className="text-muted small mt-2 mb-0">
            Edit it here before pasting it into your application.
          </p>
        </div>
      ) : (
        <AiEmptyState
          icon="bi-pencil-square"
          title="No cover letter yet"
          message="Pick a job above and hit Generate. We'll use your default resume, so make sure one is uploaded."
        />
      )}
    </div>
  );
};

export default AiCoverLetter;

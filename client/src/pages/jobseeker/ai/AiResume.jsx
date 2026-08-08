import { useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { aiApi } from '../../../api/services';
import {
  ScoreBadge, AiList, AiEmptyState, AiThinking, AiPageHeader, aiErrorMessage
} from '../../../components/AiPanels';

/**
 * Resume Analysis AND Resume Improvement.
 *
 * ai-service exposes ONE endpoint - POST /api/ai/resume-feedback - which
 * returns score + strengths + weaknesses (the analysis) and missingSkills +
 * improvementSuggestions (the improvement plan) in a single response. Splitting
 * these into two routes would mean two Gemini calls for data the backend
 * already hands back together: slower, and billed twice. They are presented
 * here as two clearly separated tabs over one call instead.
 */
const AiResume = () => {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [tab, setTab] = useState('analysis');

  const analyse = async () => {
    setLoading(true);
    try {
      const { data } = await aiApi.resumeFeedback();
      if (data.success) {
        setResult(data.data);
        setTab('analysis');
      }
    } catch (err) {
      toast.error(aiErrorMessage(err, 'Could not analyse your resume right now'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-4">
      <AiPageHeader
        icon="bi-file-earmark-bar-graph"
        title="Resume Analysis & Improvement"
        subtitle="An AI review of your default resume, with concrete ways to strengthen it."
      />

      {loading ? (
        <AiThinking message="Reading your resume…" hint="Gemini is reviewing the PDF. This usually takes 5-15 seconds." />
      ) : !result ? (
        <AiEmptyState
          icon="bi-file-earmark-bar-graph"
          title="Analyse your resume"
          message="We'll review your default resume and score it, then suggest specific improvements. Make sure you have uploaded a resume first."
        >
          <div className="d-flex gap-2 justify-content-center flex-wrap">
            <button className="btn btn-primary px-4" onClick={analyse}>
              <i className="bi bi-stars me-2"></i>Analyse my resume
            </button>
            <Link to="/jobseeker/resume" className="btn btn-outline-secondary px-4">Manage resumes</Link>
          </div>
        </AiEmptyState>
      ) : (
        <>
          <ul className="nav nav-tabs mb-4">
            <li className="nav-item">
              <button className={`nav-link ${tab === 'analysis' ? 'active fw-semibold' : ''}`} onClick={() => setTab('analysis')}>
                <i className="bi bi-graph-up me-2"></i>Analysis
              </button>
            </li>
            <li className="nav-item">
              <button className={`nav-link ${tab === 'improvement' ? 'active fw-semibold' : ''}`} onClick={() => setTab('improvement')}>
                <i className="bi bi-tools me-2"></i>Improvement
              </button>
            </li>
          </ul>

          {tab === 'analysis' ? (
            <div className="card border-0 shadow-sm p-4">
              <div className="d-flex align-items-center gap-4 mb-4 flex-wrap">
                <ScoreBadge score={result.score} label="/ 100" />
                <div>
                  <h5 className="fw-bold mb-1">Overall resume score</h5>
                  <p className="text-muted small mb-0">
                    Based on clarity, impact and overall quality.
                  </p>
                </div>
              </div>
              <hr />
              <AiList title="Strengths" items={result.strengths} icon="bi-hand-thumbs-up" tone="success" />
              <AiList title="Weaknesses" items={result.weaknesses} icon="bi-exclamation-triangle" tone="danger" />
              {(!result.strengths?.length && !result.weaknesses?.length) && (
                <p className="text-muted small mb-0">The AI did not return any analysis points for this resume.</p>
              )}
            </div>
          ) : (
            <div className="card border-0 shadow-sm p-4">
              <AiList title="Suggested improvements" items={result.improvementSuggestions} icon="bi-lightbulb" tone="primary" />
              <AiList title="Skills worth adding" items={result.missingSkills} icon="bi-plus-circle" tone="warning" />
              {(!result.improvementSuggestions?.length && !result.missingSkills?.length) && (
                <p className="text-muted small mb-0">The AI did not return any improvement suggestions for this resume.</p>
              )}
            </div>
          )}

          <div className="mt-4">
            <button className="btn btn-outline-primary" onClick={analyse}>
              <i className="bi bi-arrow-clockwise me-2"></i>Re-run analysis
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default AiResume;

package com.careerpilot.aiservice.service;

import com.careerpilot.aiservice.dto.CandidateScreeningResult;
import com.careerpilot.aiservice.dto.CoverLetterResponse;
import com.careerpilot.aiservice.dto.JobRecommendationResult;
import com.careerpilot.aiservice.dto.ResumeFeedbackResponse;
import com.careerpilot.aiservice.dto.internal.RejectionFeedbackRequest;

import java.util.List;

public interface AiService {

    /** AI FEATURE 1 - Resume Feedback. Candidate only, premium-gated. Operates on the caller's default resume. */
    ResumeFeedbackResponse getResumeFeedback(int userId);

    /** AI FEATURE 2 - Cover Letter Generator. Candidate only, premium-gated. */
    CoverLetterResponse generateCoverLetter(int userId, int jobId);

    /** AI FEATURE 3 - Candidate Screening. Employer only, premium-gated. Results sorted by matchScore descending. */
    List<CandidateScreeningResult> screenCandidates(int userId, int jobId);

    /** New - "Job recommendation support". Candidate only, premium-gated. Results sorted by matchScore descending. */
    List<JobRecommendationResult> getJobRecommendations(int userId);

    /** AI FEATURE 4 - AI Rejection Feedback. Internal only, triggered by application-service when an employer rejects a candidate. Not premium-gated at the call site - this service decides that internally based on the employer's subscription. */
    void generateRejectionFeedback(RejectionFeedbackRequest request);
}

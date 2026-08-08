package com.careerpilot.aiservice.controller;

import com.careerpilot.aiservice.dto.CandidateScreeningRequest;
import com.careerpilot.aiservice.dto.CandidateScreeningResult;
import com.careerpilot.aiservice.dto.CoverLetterRequest;
import com.careerpilot.aiservice.dto.CoverLetterResponse;
import com.careerpilot.aiservice.dto.JobRecommendationResult;
import com.careerpilot.aiservice.dto.ResumeFeedbackResponse;
import com.careerpilot.aiservice.service.AiService;
import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /** AI FEATURE 1 - Resume Feedback. */
    @PostMapping("/resume-feedback")
    public ApiResponse<ResumeFeedbackResponse> resumeFeedback() {
        return ApiResponse.ok(aiService.getResumeFeedback(SecurityUtils.currentUserId()));
    }

    /** AI FEATURE 2 - Cover Letter Generator. */
    @PostMapping("/cover-letter")
    public ApiResponse<CoverLetterResponse> coverLetter(@Valid @RequestBody CoverLetterRequest request) {
        return ApiResponse.ok(aiService.generateCoverLetter(SecurityUtils.currentUserId(), request.getJobId()));
    }

    /** AI FEATURE 3 - Candidate Screening. Results are pre-sorted by matchScore descending. */
    @PostMapping("/candidate-screening")
    public ApiResponse<List<CandidateScreeningResult>> candidateScreening(@Valid @RequestBody CandidateScreeningRequest request) {
        return ApiResponse.ok(aiService.screenCandidates(SecurityUtils.currentUserId(), request.getJobId()));
    }

    /** New - "Job recommendation support". Results are pre-sorted by matchScore descending. */
    @PostMapping("/job-recommendations")
    public ApiResponse<List<JobRecommendationResult>> jobRecommendations() {
        return ApiResponse.ok(aiService.getJobRecommendations(SecurityUtils.currentUserId()));
    }
}

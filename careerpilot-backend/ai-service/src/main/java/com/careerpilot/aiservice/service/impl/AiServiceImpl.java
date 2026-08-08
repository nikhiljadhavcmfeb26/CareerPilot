package com.careerpilot.aiservice.service.impl;

import com.careerpilot.aiservice.client.ApplicationServiceClient;
import com.careerpilot.aiservice.client.AuthServiceClient;
import com.careerpilot.aiservice.client.GeminiClient;
import com.careerpilot.aiservice.client.GeminiClient.GeminiException;
import com.careerpilot.aiservice.client.JobServiceClient;
import com.careerpilot.aiservice.client.NotificationServiceClient;
import com.careerpilot.aiservice.client.UserServiceClient;
import com.careerpilot.aiservice.dto.CandidateScreeningResult;
import com.careerpilot.aiservice.dto.CoverLetterResponse;
import com.careerpilot.aiservice.dto.JobRecommendationResult;
import com.careerpilot.aiservice.dto.ResumeFeedbackResponse;
import com.careerpilot.aiservice.dto.internal.RejectionFeedbackRequest;
import com.careerpilot.aiservice.entity.AiRequestLog;
import com.careerpilot.aiservice.enums.AiRequestType;
import com.careerpilot.aiservice.enums.ScreeningRecommendation;
import com.careerpilot.aiservice.repository.AiRequestLogRepository;
import com.careerpilot.aiservice.service.AiService;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final GeminiClient geminiClient;
    private final AuthServiceClient authServiceClient;
    private final UserServiceClient userServiceClient;
    private final JobServiceClient jobServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final AiRequestLogRepository requestLogRepository;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(GeminiClient geminiClient, AuthServiceClient authServiceClient,
                          UserServiceClient userServiceClient, JobServiceClient jobServiceClient,
                          ApplicationServiceClient applicationServiceClient,
                          NotificationServiceClient notificationServiceClient,
                          AiRequestLogRepository requestLogRepository, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.authServiceClient = authServiceClient;
        this.userServiceClient = userServiceClient;
        this.jobServiceClient = jobServiceClient;
        this.applicationServiceClient = applicationServiceClient;
        this.notificationServiceClient = notificationServiceClient;
        this.requestLogRepository = requestLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResumeFeedbackResponse getResumeFeedback(int userId) {
        requireAiAccess(userId, AiRequestType.RESUME_FEEDBACK);
        int profileId = getJobSeekerProfileId(userId);
        String base64Pdf = fetchDefaultResumeBase64(profileId);

        try {
            String prompt = """
                    You are an expert resume reviewer and career coach. Analyze the attached resume (PDF) \
                    and respond with ONLY a JSON object (no markdown, no code fences) matching exactly this shape:
                    {"score": <integer 0-100>, "strengths": [<string>, ...], "weaknesses": [<string>, ...], \
                    "missingSkills": [<string>, ...], "improvementSuggestions": [<string>, ...]}
                    Base the score on overall resume quality, clarity, and impact. List 3-5 specific, actionable \
                    items in each array.""";

            JsonNode result = geminiClient.generateJson(prompt, base64Pdf);

            ResumeFeedbackResponse response = new ResumeFeedbackResponse();
            response.setScore(result.path("score").asInt(0));
            response.setStrengths(toStringList(result.path("strengths")));
            response.setWeaknesses(toStringList(result.path("weaknesses")));
            response.setMissingSkills(toStringList(result.path("missingSkills")));
            response.setImprovementSuggestions(toStringList(result.path("improvementSuggestions")));

            logRequest(userId, AiRequestType.RESUME_FEEDBACK, true, null);
            return response;
        } catch (Exception ex) {
            logRequest(userId, AiRequestType.RESUME_FEEDBACK, false, ex.getMessage());
            log.error("Failed to generate resume feedback for user {}", userId, ex);
            throw new BadRequestException(userFacingAiError(ex, "Could not generate resume feedback right now. Please try again."));
        }
    }

    @Override
    public CoverLetterResponse generateCoverLetter(int userId, int jobId) {
        requireAiAccess(userId, AiRequestType.COVER_LETTER);
        int profileId = getJobSeekerProfileId(userId);
        String base64Pdf = fetchDefaultResumeBase64(profileId);
        JobServiceClient.JobLookup job = fetchJob(jobId);

        try {
            String prompt = """
                    You are an expert career coach. Using the attached resume (PDF) and the job details below, \
                    write a professional, personalized cover letter (3-4 paragraphs). Respond with ONLY a JSON \
                    object (no markdown, no code fences) matching exactly this shape:
                    {"coverLetter": "<the full cover letter text, paragraphs separated by two newlines>"}

                    Job Title: %s
                    Job Description: %s
                    Job Requirements: %s""".formatted(job.title(), job.description(), job.requirements());

            JsonNode result = geminiClient.generateJson(prompt, base64Pdf);
            String coverLetter = result.path("coverLetter").asText("");

            logRequest(userId, AiRequestType.COVER_LETTER, true, null);
            return new CoverLetterResponse(coverLetter);
        } catch (Exception ex) {
            logRequest(userId, AiRequestType.COVER_LETTER, false, ex.getMessage());
            log.error("Failed to generate cover letter for user {} / job {}", userId, jobId, ex);
            throw new BadRequestException(userFacingAiError(ex, "Could not generate a cover letter right now. Please try again."));
        }
    }

    @Override
    public List<CandidateScreeningResult> screenCandidates(int userId, int jobId) {
        Integer companyId = getEmployerCompanyIdOrThrow(userId);
        requireAiAccess(userId, AiRequestType.CANDIDATE_SCREENING);

        JobServiceClient.JobLookup job = fetchJob(jobId);
        if (!job.companyId().equals(companyId)) {
            throw new BadRequestException("You can only screen candidates for your own jobs.");
        }

        List<ApplicationServiceClient.ApplicationLookup> applicants;
        try {
            applicants = applicationServiceClient.getByJob(jobId);
        } catch (Exception ex) {
            log.error("application-service unavailable while fetching applicants for job {}", jobId, ex);
            throw new BadRequestException("Could not load applicants right now. Please try again.");
        }

        String screeningPrompt = """
                You are an expert technical recruiter. Compare the attached candidate resume (PDF) against the \
                job requirements below, and respond with ONLY a JSON object (no markdown, no code fences) \
                matching exactly this shape:
                {"matchScore": <integer 0-100>, "skillMatchPercentage": <integer 0-100>, \
                "strengths": [<string>, ...], "weaknesses": [<string>, ...], "missingSkills": [<string>, ...], \
                "recommendation": "<one of: HighlyRecommended, Recommended, Consider, NotRecommended>"}

                Job Title: %s
                Job Description: %s
                Job Requirements: %s""".formatted(job.title(), job.description(), job.requirements());

        List<CandidateScreeningResult> results = new ArrayList<>();
        for (ApplicationServiceClient.ApplicationLookup applicant : applicants) {
            if ("Withdrawn".equals(applicant.status()) || applicant.resumeId() == null) {
                continue;
            }
            try {
                UserServiceClient.ResumeDownload resume = userServiceClient.downloadResume(applicant.resumeId());
                JsonNode resultNode = geminiClient.generateJson(screeningPrompt, resume.base64Content());

                CandidateScreeningResult result = new CandidateScreeningResult();
                result.setApplicationId(applicant.applicationId());
                result.setMatchScore(resultNode.path("matchScore").asInt(0));
                result.setSkillMatchPercentage(resultNode.path("skillMatchPercentage").asInt(0));
                result.setStrengths(toStringList(resultNode.path("strengths")));
                result.setWeaknesses(toStringList(resultNode.path("weaknesses")));
                result.setMissingSkills(toStringList(resultNode.path("missingSkills")));
                result.setRecommendation(parseRecommendation(resultNode.path("recommendation").asText(null)));

                try {
                    AuthServiceClient.UserBasicInfo applicantInfo = authServiceClient.getUserBasicInfo(applicant.userId());
                    result.setApplicantName(applicantInfo != null ? applicantInfo.firstName() + " " + applicantInfo.lastName() : "Unknown candidate");
                } catch (Exception ex) {
                    result.setApplicantName("Unknown candidate");
                }

                results.add(result);
            } catch (Exception ex) {
                // One candidate failing to screen (bad resume file, transient Gemini
                // error) shouldn't take down the whole batch - skip and continue.
                log.warn("Failed to screen application {} for job {}", applicant.applicationId(), jobId, ex);
            }
        }

        results.sort(Comparator.comparingInt(CandidateScreeningResult::getMatchScore).reversed());
        logRequest(userId, AiRequestType.CANDIDATE_SCREENING, true, null);
        return results;
    }

    @Override
    public List<JobRecommendationResult> getJobRecommendations(int userId) {
        requireAiAccess(userId, AiRequestType.JOB_RECOMMENDATIONS);
        int profileId = getJobSeekerProfileId(userId);
        String base64Pdf = fetchDefaultResumeBase64(profileId);

        List<JobServiceClient.JobLookup> jobs;
        try {
            jobs = jobServiceClient.getPublishedJobs();
        } catch (Exception ex) {
            log.error("job-service unavailable while fetching published jobs for recommendations", ex);
            throw new BadRequestException("Could not load open jobs right now. Please try again.");
        }

        if (jobs.isEmpty()) {
            logRequest(userId, AiRequestType.JOB_RECOMMENDATIONS, true, null);
            return List.of();
        }

        try {
            String jobsJson = objectMapper.writeValueAsString(jobs.stream()
                    .map(j -> new SimpleJobForPrompt(j.id(), j.title(), j.description(), j.requirements()))
                    .toList());

            String prompt = """
                    You are an expert career advisor. Given the attached candidate resume (PDF) and the \
                    following list of currently open jobs (as JSON), identify the best-matching jobs for this \
                    candidate. Respond with ONLY a JSON object (no markdown, no code fences) matching exactly \
                    this shape:
                    {"recommendations": [{"jobId": <integer>, "matchScore": <integer 0-100>, \
                    "reasoning": "<1-2 sentence explanation>"}, ...]}
                    Return at most 10 recommendations, sorted by matchScore descending, only including jobs \
                    with matchScore >= 50.

                    Jobs:
                    %s""".formatted(jobsJson);

            JsonNode result = geminiClient.generateJson(prompt, base64Pdf);

            List<JobRecommendationResult> recommendations = new ArrayList<>();
            for (JsonNode rec : result.path("recommendations")) {
                int jobId = rec.path("jobId").asInt(-1);
                JobServiceClient.JobLookup matchedJob = jobs.stream().filter(j -> j.id() == jobId).findFirst().orElse(null);
                if (matchedJob == null) {
                    continue;
                }

                JobRecommendationResult recommendation = new JobRecommendationResult();
                recommendation.setJobId(matchedJob.id());
                recommendation.setJobTitle(matchedJob.title());
                recommendation.setMatchScore(rec.path("matchScore").asInt(0));
                recommendation.setReasoning(rec.path("reasoning").asText(""));

                try {
                    UserServiceClient.CompanySummary company = userServiceClient.getCompanySummary(matchedJob.companyId());
                    recommendation.setCompanyName(company != null ? company.name() : null);
                } catch (Exception ex) {
                    log.debug("user-service unavailable while hydrating company name for recommended job {}", matchedJob.id());
                }

                recommendations.add(recommendation);
            }

            recommendations.sort(Comparator.comparingInt(JobRecommendationResult::getMatchScore).reversed());
            logRequest(userId, AiRequestType.JOB_RECOMMENDATIONS, true, null);
            return recommendations;
        } catch (Exception ex) {
            logRequest(userId, AiRequestType.JOB_RECOMMENDATIONS, false, ex.getMessage());
            log.error("Failed to generate job recommendations for user {}", userId, ex);
            throw new BadRequestException(userFacingAiError(ex, "Could not generate job recommendations right now. Please try again."));
        }
    }

    @Override
    public void generateRejectionFeedback(RejectionFeedbackRequest request) {
        String jobTitle = "the position";
        String jobDescription = "";
        String jobRequirements = "";
        try {
            JobServiceClient.JobLookup job = jobServiceClient.getJob(request.jobId());
            jobTitle = job.title();
            jobDescription = job.description();
            jobRequirements = job.requirements();
        } catch (Exception ex) {
            log.warn("job-service unavailable while building rejection feedback for job {}, using generic wording", request.jobId());
        }

        String feedbackText = null;
        boolean aiAllowed = false;
        try {
            AuthServiceClient.AiAccessResponse access = authServiceClient.checkAiAccess(
                    request.employerUserId(), AiRequestType.REJECTION_FEEDBACK.name());
            aiAllowed = access != null && access.allowed();
        } catch (Exception ex) {
            log.warn("auth-service unavailable while checking AI access for rejection feedback, employer {}", request.employerUserId());
        }

        if (aiAllowed) {
            feedbackText = tryGenerateRejectionFeedback(request, jobTitle, jobDescription, jobRequirements);
        }

        try {
            notificationServiceClient.sendAiFeedbackEmail(new NotificationServiceClient.AiFeedbackEmailRequest(
                    request.candidateEmail(), request.candidateFirstName(), jobTitle, feedbackText));
            logRequest(request.employerUserId(), AiRequestType.REJECTION_FEEDBACK, true, null);
        } catch (Exception ex) {
            logRequest(request.employerUserId(), AiRequestType.REJECTION_FEEDBACK, false, ex.getMessage());
            log.error("Failed to send rejection feedback email for job {}", request.jobId(), ex);
        }
    }

    /**
     * Best-effort - any failure here (Gemini down, no resume on file, bad
     * response) just means notification-service falls back to its own
     * professional default message, per your spec: "If the Gemini API fails,
     * send a professional default feedback email." This method itself never
     * throws.
     */
    private String tryGenerateRejectionFeedback(RejectionFeedbackRequest request, String jobTitle, String jobDescription, String jobRequirements) {
        try {
            String base64Pdf = null;
            try {
                UserServiceClient.ResumeDownload resume = userServiceClient.downloadDefaultResume(request.jobSeekerProfileId());
                base64Pdf = resume.base64Content();
            } catch (Exception ex) {
                log.debug("No resume on file for job seeker profile {}, generating feedback from job details alone", request.jobSeekerProfileId());
            }

            String prompt = """
                    You are a professional, empathetic recruiter. A candidate was not selected for the role \
                    below. Write brief, constructive feedback (2-3 sentences) explaining general areas they \
                    could improve, based on comparing their resume (if attached) against the job requirements. \
                    Be encouraging and professional - this feedback will be emailed directly to the candidate. \
                    Respond with ONLY a JSON object (no markdown, no code fences) matching exactly this shape:
                    {"feedback": "<the feedback text>"}

                    Job Title: %s
                    Job Description: %s
                    Job Requirements: %s""".formatted(jobTitle, jobDescription, jobRequirements);

            JsonNode result = geminiClient.generateJson(prompt, base64Pdf);
            String feedback = result.path("feedback").asText(null);
            return (feedback == null || feedback.isBlank()) ? null : feedback;
        } catch (Exception ex) {
            log.warn("Gemini failed while generating rejection feedback for job {}, falling back to default email", request.jobId(), ex);
            return null;
        }
    }

    // ---- shared helpers ----

    /**
     * ADMIN MODULE. Replaces the old requirePremium(): one call to
     * auth-service now covers the Premium subscription AND the administrator's
     * AI controls (global kill switch, per-feature switches, per-account
     * revocation, blocked/deactivated accounts). The denial reason comes back
     * from auth-service so the user is told which of those actually applies,
     * instead of always being told to buy Premium.
     */
    private void requireAiAccess(int userId, AiRequestType feature) {
        AuthServiceClient.AiAccessResponse access;
        try {
            access = authServiceClient.checkAiAccess(userId, feature.name());
        } catch (Exception ex) {
            log.error("auth-service unavailable while checking AI access for user {}", userId, ex);
            throw new BadRequestException("Could not verify your subscription right now. Please try again.");
        }
        if (access == null) {
            throw new BadRequestException("Could not verify your subscription right now. Please try again.");
        }
        if (!access.allowed()) {
            throw new BadRequestException(access.reason() != null
                    ? access.reason()
                    : "This feature requires an active Premium subscription.");
        }
    }

    private int getJobSeekerProfileId(int userId) {
        try {
            UserServiceClient.JobSeekerProfileLookup lookup = userServiceClient.getJobSeekerProfileForUser(userId);
            if (lookup == null || lookup.jobSeekerProfileId() == null) {
                throw new BadRequestException("Only job seekers can use this feature.");
            }
            return lookup.jobSeekerProfileId();
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Only job seekers can use this feature.");
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("user-service unavailable while resolving job seeker profile for user {}", userId, ex);
            throw new BadRequestException("Could not verify your account right now. Please try again.");
        }
    }

    private Integer getEmployerCompanyIdOrThrow(int userId) {
        try {
            return userServiceClient.getCompanyForEmployerUser(userId).companyId();
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Only employers with a registered company can screen candidates.");
        } catch (Exception ex) {
            log.error("user-service unavailable while resolving company for employer user {}", userId, ex);
            throw new BadRequestException("Could not verify your company right now. Please try again.");
        }
    }

    private String fetchDefaultResumeBase64(int jobSeekerProfileId) {
        try {
            UserServiceClient.ResumeDownload resume = userServiceClient.downloadDefaultResume(jobSeekerProfileId);
            return resume.base64Content();
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Please upload a resume before using this feature.");
        } catch (Exception ex) {
            log.error("user-service unavailable while fetching default resume for profile {}", jobSeekerProfileId, ex);
            throw new BadRequestException("Could not retrieve your resume right now. Please try again.");
        }
    }

    private JobServiceClient.JobLookup fetchJob(int jobId) {
        try {
            return jobServiceClient.getJob(jobId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Job not found.");
        } catch (Exception ex) {
            log.error("job-service unavailable while resolving job {}", jobId, ex);
            throw new BadRequestException("Could not verify the job right now. Please try again.");
        }
    }

    /**
     * A Gemini failure used to surface as one flat "please try again", which
     * hid the difference between "your key is wrong" (an operator problem that
     * retrying will never fix) and a genuine hiccup. Configuration/quota
     * failures now say so; anything else keeps the neutral message so internal
     * detail is not leaked to end users. The full exception is logged either
     * way.
     */
    private String userFacingAiError(Exception ex, String fallback) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof GeminiException) {
                String message = cursor.getMessage() == null ? "" : cursor.getMessage();
                if (message.contains("(401)") || message.contains("(403)") || message.contains("not configured")) {
                    return "The AI service is not configured correctly. Please contact the administrator.";
                }
                if (message.contains("(404)")) {
                    return "The AI service is pointing at a model that no longer exists. Please contact the administrator.";
                }
                if (message.contains("(429)")) {
                    return "The AI service is rate limited right now. Please try again in a few minutes.";
                }
                if (message.startsWith("Gemini declined to respond")) {
                    return "The AI declined to process this content. Please try a different resume or job.";
                }
                break;
            }
            cursor = cursor.getCause();
        }
        return fallback;
    }

    private List<String> toStringList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            arrayNode.forEach(node -> list.add(node.asText()));
        }
        return list;
    }

    private ScreeningRecommendation parseRecommendation(String value) {
        if (value == null) {
            return ScreeningRecommendation.Consider;
        }
        for (ScreeningRecommendation r : ScreeningRecommendation.values()) {
            if (r.name().equalsIgnoreCase(value.trim())) {
                return r;
            }
        }
        return ScreeningRecommendation.Consider;
    }

    private void logRequest(int userId, AiRequestType type, boolean success, String errorMessage) {
        try {
            AiRequestLog entry = new AiRequestLog();
            entry.setUserId(userId);
            entry.setType(type);
            entry.setSuccess(success);
            entry.setErrorMessage(errorMessage != null && errorMessage.length() > 1000 ? errorMessage.substring(0, 1000) : errorMessage);
            requestLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to write AI request audit log", ex);
        }
    }

    private record SimpleJobForPrompt(Integer jobId, String title, String description, String requirements) {
    }
}

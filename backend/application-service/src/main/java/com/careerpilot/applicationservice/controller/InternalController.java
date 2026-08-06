package com.careerpilot.applicationservice.controller;

import com.careerpilot.applicationservice.dto.internal.ApplicationLookupResponse;
import com.careerpilot.applicationservice.dto.internal.ApplicationStatsResponse;
import com.careerpilot.applicationservice.entity.JobApplication;
import com.careerpilot.applicationservice.enums.ApplicationStatus;
import com.careerpilot.applicationservice.repository.JobApplicationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Service-to-service only, same story as every other service's
 * InternalController - no /internal/** route exists in api-gateway.yml.
 */
@RestController
@RequestMapping("/internal/applications")
public class InternalController {

    private final JobApplicationRepository applicationRepository;

    public InternalController(JobApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    /** Called by job-service to populate JobDto.applicationCount - matches the contract job-service's ApplicationServiceClient already expects. */
    @GetMapping("/count-by-job/{jobId}")
    public int countByJob(@PathVariable("jobId") Integer jobId) {
        return applicationRepository.countByJobId(jobId);
    }

    /** Called by auth-service's admin dashboard aggregation. */
    @GetMapping("/stats")
    public ApplicationStatsResponse getStats() {
        return new ApplicationStatsResponse(
                (int) applicationRepository.count(),
                applicationRepository.countByStatus(ApplicationStatus.Pending),
                applicationRepository.countByStatus(ApplicationStatus.Shortlisted));
    }

    /**
     * Called by auth-service's employer dashboard aggregation with the ids of
     * the jobs that employer owns (resolved from job-service first). An empty
     * list short-circuits rather than issuing an "IN ()" query, which is
     * invalid SQL on several databases.
     */
    @GetMapping("/stats/by-jobs")
    public ApplicationStatsResponse getStatsByJobs(@RequestParam(value = "jobIds", required = false) List<Integer> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return ApplicationStatsResponse.empty();
        }
        return new ApplicationStatsResponse(
                applicationRepository.countByJobIdIn(jobIds),
                applicationRepository.countByJobIdInAndStatus(jobIds, ApplicationStatus.Pending),
                applicationRepository.countByJobIdInAndStatus(jobIds, ApplicationStatus.Shortlisted));
    }

    /** Called by auth-service's job seeker dashboard aggregation. */
    @GetMapping("/stats/by-profile/{jobSeekerProfileId}")
    public ApplicationStatsResponse getStatsByProfile(@PathVariable("jobSeekerProfileId") Integer jobSeekerProfileId) {
        return new ApplicationStatsResponse(
                applicationRepository.countByJobSeekerProfileId(jobSeekerProfileId),
                applicationRepository.countByJobSeekerProfileIdAndStatus(jobSeekerProfileId, ApplicationStatus.Pending),
                applicationRepository.countByJobSeekerProfileIdAndStatus(jobSeekerProfileId, ApplicationStatus.Shortlisted));
    }

    /** Called by ai-service's candidate screening feature to get the applicant list for a job. */
    @GetMapping("/by-job/{jobId}")
    public List<ApplicationLookupResponse> getByJob(@PathVariable("jobId") Integer jobId) {
        return applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId).stream()
                .map(a -> new ApplicationLookupResponse(a.getId(), a.getUserId(), a.getJobSeekerProfileId(), a.getResumeId(), a.getStatus().name()))
                .toList();
    }
}

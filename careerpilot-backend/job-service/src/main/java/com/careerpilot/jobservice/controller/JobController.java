package com.careerpilot.jobservice.controller;

import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.dto.PagedResult;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.security.SecurityUtils;
import com.careerpilot.jobservice.dto.CreateJobRequest;
import com.careerpilot.jobservice.dto.JobDto;
import com.careerpilot.jobservice.dto.UpdateJobRequest;
import com.careerpilot.jobservice.enums.ExperienceLevel;
import com.careerpilot.jobservice.enums.JobType;
import com.careerpilot.jobservice.service.JobService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    /** Upper bound for the public search endpoint - see search(). */
    private static final int MAX_PAGE_SIZE = 100;

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * jobType/experienceLevel arrive as plain numeric strings from
     * Jobs.jsx's <option value="0">...</option> filters - a raw
     * @RequestParam JobType would make Spring try Enum.valueOf("0") and
     * fail, so these are bound as Integer ordinals here and converted
     * manually, mirroring how ASP.NET Core's query-string model binder
     * (a different mechanism than System.Text.Json) accepts numeric enum
     * values by default.
     */
    @GetMapping
    public ApiResponse<PagedResult<JobDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer jobType,
            @RequestParam(required = false) Integer experienceLevel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        JobType jobTypeEnum = toEnum(JobType.values(), jobType, "jobType");
        ExperienceLevel experienceLevelEnum = toEnum(ExperienceLevel.values(), experienceLevel, "experienceLevel");

        // /api/jobs is public and unauthenticated, and every JobDto costs two
        // cross-service calls to hydrate (company name + application count).
        // An unbounded pageSize therefore let an anonymous caller ask for the
        // entire table and fan that out across the platform. Clamp it.
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        return ApiResponse.ok(jobService.searchJobs(keyword, location, jobTypeEnum, experienceLevelEnum, page, safePageSize));
    }

    @GetMapping("/my")
    public ApiResponse<List<JobDto>> getMyJobs() {
        return ApiResponse.ok(jobService.getMyJobs(SecurityUtils.currentUserId()));
    }

    @GetMapping("/admin/all")
    public ApiResponse<List<JobDto>> getAllAdmin() {
        return ApiResponse.ok(jobService.getAllJobsAdmin());
    }

    @GetMapping("/{id}")
    public ApiResponse<JobDto> getById(@PathVariable int id) {
        return ApiResponse.ok(jobService.getJobById(id));
    }

    @PostMapping
    public ApiResponse<JobDto> create(@Valid @RequestBody CreateJobRequest request) {
        return ApiResponse.ok(jobService.createJob(SecurityUtils.currentUserId(), request), "Job created");
    }

    @PutMapping("/{id}")
    public ApiResponse<JobDto> update(@PathVariable int id, @Valid @RequestBody UpdateJobRequest request) {
        return ApiResponse.ok(jobService.updateJob(SecurityUtils.currentUserId(), id, request), "Job updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable int id) {
        jobService.deleteJob(SecurityUtils.currentUserId(), id);
        return ApiResponse.success("Job deleted");
    }

    @DeleteMapping("/admin/{id}")
    public ApiResponse<Void> adminDelete(@PathVariable int id) {
        jobService.adminDeleteJob(id);
        return ApiResponse.success("Job deleted");
    }

    @PutMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable int id) {
        jobService.publishJob(SecurityUtils.currentUserId(), id);
        return ApiResponse.success("Job published");
    }

    @PutMapping("/{id}/close")
    public ApiResponse<Void> close(@PathVariable int id) {
        jobService.closeJob(SecurityUtils.currentUserId(), id);
        return ApiResponse.success("Job closed");
    }

    private <T extends Enum<T>> T toEnum(T[] values, Integer ordinal, String fieldName) {
        if (ordinal == null) {
            return null;
        }
        if (ordinal < 0 || ordinal >= values.length) {
            throw new BadRequestException("Invalid " + fieldName + " value.");
        }
        return values[ordinal];
    }
}

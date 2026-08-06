package com.careerpilot.jobservice.service;

import com.careerpilot.common.dto.PagedResult;
import com.careerpilot.jobservice.dto.CreateJobRequest;
import com.careerpilot.jobservice.dto.JobDto;
import com.careerpilot.jobservice.dto.UpdateJobRequest;
import com.careerpilot.jobservice.enums.ExperienceLevel;
import com.careerpilot.jobservice.enums.JobType;

import java.util.List;

public interface JobService {

    JobDto createJob(int userId, CreateJobRequest request);

    JobDto updateJob(int userId, int jobId, UpdateJobRequest request);

    void deleteJob(int userId, int jobId);

    void adminDeleteJob(int jobId);

    JobDto getJobById(int jobId);

    PagedResult<JobDto> searchJobs(String keyword, String location, JobType jobType, ExperienceLevel experienceLevel, int page, int pageSize);

    List<JobDto> getMyJobs(int userId);

    List<JobDto> getAllJobsAdmin();

    void publishJob(int userId, int jobId);

    void closeJob(int userId, int jobId);
}

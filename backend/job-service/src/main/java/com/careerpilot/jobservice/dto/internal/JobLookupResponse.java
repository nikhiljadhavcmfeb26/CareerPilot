package com.careerpilot.jobservice.dto.internal;

/**
 * For application-service: validating a job exists/is published before
 * accepting an application, and hydrating ApplicationDto.JobTitle. Status is
 * a plain string rather than the JobType enum, so application-service isn't
 * forced to depend on job-service's enum type just to compare against
 * "Published". description/requirements are additionally used by ai-service
 * for candidate screening against the job's actual requirements.
 */
public record JobLookupResponse(Integer id, String title, Integer companyId, String status, String description, String requirements) {
}

package com.careerpilot.userservice.service;

import com.careerpilot.userservice.dto.ResumeDto;
import com.careerpilot.userservice.dto.internal.ResumeDownloadResponse;
import com.careerpilot.userservice.dto.internal.ResumeReferenceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    ResumeDto uploadResume(int userId, MultipartFile file);

    List<ResumeDto> getMyResumes(int userId);

    void deleteResume(int userId, int resumeId);

    void setDefaultResume(int userId, int resumeId);

    /**
     * NEW - the original app has no download/view endpoint at all, not even
     * for a job seeker's own resumes. Returns raw bytes for a direct HTTP
     * response, unlike downloadForInternalCaller below - this one is called
     * straight from a browser via the gateway, not through Feign.
     */
    ResumeFileContent downloadOwnResume(int userId, int resumeId);

    /**
     * NEW - for the internal endpoint application-service calls after it has
     * already verified the requesting employer owns the job. No ownership
     * check here; that decision was already made by the caller. Base64-wrapped
     * because this one travels over a plain JSON Feign call.
     */
    ResumeDownloadResponse downloadForInternalCaller(int resumeId);

    /**
     * Resolves which resume an application should be attached to, without
     * transferring the file. If resumeId is supplied it must belong to this
     * job seeker (otherwise a candidate could attach someone else's resume to
     * their application); if it is null the candidate's default resume is used,
     * and null is returned when they have none - applying without a resume
     * stays permitted, exactly as before.
     */
    ResumeReferenceResponse resolveForApplication(int jobSeekerProfileId, Integer resumeId);

    /** NEW - for ai-service, which operates on "the calling candidate's default resume" rather than requiring a resumeId in each AI request body. Throws ResourceNotFoundException if the profile has no resumes at all. */
    ResumeDownloadResponse downloadDefaultForProfile(int jobSeekerProfileId);

    record ResumeFileContent(String fileName, String contentType, byte[] content) {
    }
}

package com.careerpilot.userservice.service.impl;

import com.careerpilot.userservice.dto.ResumeDto;
import com.careerpilot.userservice.dto.internal.ResumeDownloadResponse;
import com.careerpilot.userservice.dto.internal.ResumeReferenceResponse;
import com.careerpilot.userservice.entity.JobSeekerProfile;
import com.careerpilot.userservice.entity.Resume;
import com.careerpilot.userservice.repository.JobSeekerProfileRepository;
import com.careerpilot.userservice.repository.ResumeRepository;
import com.careerpilot.userservice.service.ResumeService;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;

@Service
public class ResumeServiceImpl implements ResumeService {

    private static final String RESUME_FOLDER = "resumes";

    private final ResumeRepository resumeRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final FileStorageService fileStorageService;

    public ResumeServiceImpl(ResumeRepository resumeRepository, JobSeekerProfileRepository jobSeekerProfileRepository,
                              FileStorageService fileStorageService) {
        this.resumeRepository = resumeRepository;
        this.jobSeekerProfileRepository = jobSeekerProfileRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public ResumeDto uploadResume(int userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file uploaded");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are allowed.");
        }

        // The extension check above is trivially bypassed by renaming a file.
        // Every download path in this service serves resumes as
        // application/pdf, so anything that isn't actually a PDF would be sent
        // to a browser (or to Gemini) under a lying content type.
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf")) {
            throw new BadRequestException("Only PDF files are allowed.");
        }

        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Only job seekers can upload resumes."));

        String storedPath;
        try {
            storedPath = fileStorageService.saveFile(file, RESUME_FOLDER);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store resume file.", ex);
        }

        boolean isFirstResume = resumeRepository.findByJobSeekerProfileIdOrderByUploadedAtDesc(profile.getId()).isEmpty();

        Resume resume = new Resume();
        resume.setJobSeekerProfile(profile);
        resume.setFileName(originalName);
        resume.setFilePath(storedPath);
        resume.setDefault(isFirstResume);

        resumeRepository.save(resume);
        return toDto(resume);
    }

    @Override
    public List<ResumeDto> getMyResumes(int userId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Only job seekers have resumes."));
        return resumeRepository.findByJobSeekerProfileIdOrderByUploadedAtDesc(profile.getId())
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void deleteResume(int userId, int resumeId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Only job seekers can delete resumes."));
        Resume resume = findOwnedResume(resumeId, profile.getId(), "You can only delete your own resumes.");

        boolean wasDefault = resume.isDefault();

        fileStorageService.deleteFile(resume.getFilePath());
        resumeRepository.delete(resume);

        // Deleting the default previously left the candidate with resumes but
        // no default, which silently broke everything that resolves "the
        // candidate's default resume" - AI resume feedback, cover letters, job
        // recommendations, rejection feedback, and now the apply flow. Promote
        // the most recently uploaded survivor instead.
        if (wasDefault) {
            resumeRepository.findByJobSeekerProfileIdOrderByUploadedAtDesc(profile.getId())
                    .stream().findFirst().ifPresent(newest -> {
                        newest.setDefault(true);
                        resumeRepository.save(newest);
                    });
        }
    }

    @Override
    @Transactional
    public void setDefaultResume(int userId, int resumeId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Only job seekers can set default resume."));
        findOwnedResume(resumeId, profile.getId(), "You can only set your own resume as default.");

        List<Resume> resumes = resumeRepository.findByJobSeekerProfileIdOrderByUploadedAtDesc(profile.getId());
        for (Resume r : resumes) {
            r.setDefault(r.getId() == resumeId);
        }
        resumeRepository.saveAll(resumes);
    }

    @Override
    public ResumeFileContent downloadOwnResume(int userId, int resumeId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Only job seekers have resumes."));
        Resume resume = findOwnedResume(resumeId, profile.getId(), "You can only view your own resumes.");
        return readAsFileContent(resume);
    }

    @Override
    public ResumeReferenceResponse resolveForApplication(int jobSeekerProfileId, Integer resumeId) {
        if (resumeId != null) {
            Resume resume = findOwnedResume(resumeId, jobSeekerProfileId, "You can only attach your own resume.");
            return new ResumeReferenceResponse(resume.getId(), resume.getFileName());
        }

        return resumeRepository.findByJobSeekerProfileIdAndIsDefaultTrue(jobSeekerProfileId)
                .map(resume -> new ResumeReferenceResponse(resume.getId(), resume.getFileName()))
                .orElseGet(() -> new ResumeReferenceResponse(null, null));
    }

    @Override
    public ResumeDownloadResponse downloadForInternalCaller(int resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found."));
        ResumeFileContent file = readAsFileContent(resume);
        String base64 = Base64.getEncoder().encodeToString(file.content());
        return new ResumeDownloadResponse(file.fileName(), file.contentType(), base64);
    }

    @Override
    public ResumeDownloadResponse downloadDefaultForProfile(int jobSeekerProfileId) {
        Resume resume = resumeRepository.findByJobSeekerProfileIdAndIsDefaultTrue(jobSeekerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("This candidate has no resume on file."));
        ResumeFileContent file = readAsFileContent(resume);
        String base64 = Base64.getEncoder().encodeToString(file.content());
        return new ResumeDownloadResponse(file.fileName(), file.contentType(), base64);
    }

    private Resume findOwnedResume(int resumeId, Integer jobSeekerProfileId, String errorMessage) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found."));
        if (!resume.getJobSeekerProfile().getId().equals(jobSeekerProfileId)) {
            throw new BadRequestException(errorMessage);
        }
        return resume;
    }

    private ResumeFileContent readAsFileContent(Resume resume) {
        try {
            byte[] content = fileStorageService.readFile(resume.getFilePath());
            return new ResumeFileContent(resume.getFileName(), "application/pdf", content);
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Resume file could not be read.");
        }
    }

    private ResumeDto toDto(Resume resume) {
        ResumeDto dto = new ResumeDto();
        dto.setId(resume.getId());
        dto.setFileName(resume.getFileName());
        dto.setDefault(resume.isDefault());
        dto.setUploadedAt(resume.getUploadedAt());
        return dto;
    }
}

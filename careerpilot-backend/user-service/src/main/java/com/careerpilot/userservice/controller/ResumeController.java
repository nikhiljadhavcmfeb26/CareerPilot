package com.careerpilot.userservice.controller;

import com.careerpilot.userservice.dto.ResumeDto;
import com.careerpilot.userservice.service.ResumeService;
import com.careerpilot.userservice.service.ResumeService.ResumeFileContent;
import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.SecurityUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping
    public ApiResponse<List<ResumeDto>> getMyResumes() {
        return ApiResponse.ok(resumeService.getMyResumes(SecurityUtils.currentUserId()));
    }

    @PostMapping("/upload")
    public ApiResponse<ResumeDto> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(resumeService.uploadResume(SecurityUtils.currentUserId(), file), "Resume uploaded");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable int id) {
        resumeService.deleteResume(SecurityUtils.currentUserId(), id);
        return ApiResponse.success("Resume deleted");
    }

    @PutMapping("/{id}/default")
    public ApiResponse<Void> setDefault(@PathVariable int id) {
        resumeService.setDefaultResume(SecurityUtils.currentUserId(), id);
        return ApiResponse.success("Default resume set");
    }

    /**
     * NEW - the original app never exposed a way to view/download a resume,
     * not even one's own. Serves the raw PDF so it opens directly in the
     * browser rather than downloading a wrapped JSON response.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable int id) {
        ResumeFileContent file = resumeService.downloadOwnResume(SecurityUtils.currentUserId(), id);

        ContentDisposition disposition = ContentDisposition.inline().filename(file.fileName()).build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }
}

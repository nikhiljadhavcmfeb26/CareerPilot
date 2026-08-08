package com.careerpilot.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * filePath was removed in Phase 4: it exposed the server's on-disk storage
 * location ("resumes/<uuid>_<name>.pdf") to the browser, which is internal
 * detail the client can neither use nor should see. Resumes are fetched via
 * GET /api/resumes/{id}/download instead.
 */
public class ResumeDto {

    private Integer id;
    private String fileName;
    private boolean isDefault;
    private LocalDateTime uploadedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @JsonProperty("isDefault")
    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}

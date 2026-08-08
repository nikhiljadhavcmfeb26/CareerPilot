package com.careerpilot.userservice.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Mirrors CareerPilot.Infrastructure.Services.FileStorageService: files are
 * saved under {uploadDir}/{folder}/{uuid}_{originalFileName}, and the
 * returned/stored path is folder-relative ("resumes/uuid_file.pdf"), the
 * same shape DeleteFileAsync expected back in the .NET version.
 */
@Component
public class FileStorageService {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public String saveFile(MultipartFile file, String folder) throws IOException {
        Path folderPath = Path.of(uploadDir, folder).toAbsolutePath().normalize();
        Files.createDirectories(folderPath);

        String uniqueName = UUID.randomUUID() + "_" + safeFileName(file.getOriginalFilename());
        Path targetPath = folderPath.resolve(uniqueName);
        file.transferTo(targetPath);

        return folder + "/" + uniqueName;
    }

    /**
     * The uploaded filename is attacker-controlled. It was previously
     * concatenated straight into the stored path, so a multipart part named
     * "../../../../etc/cron.d/x.pdf" would have been written outside the
     * uploads directory. Keep only the final path segment and strip anything
     * that isn't a safe filename character.
     */
    private String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "resume.pdf";
        }
        String baseName = Paths.get(originalName.replace('\\', '/')).getFileName().toString();
        String cleaned = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isBlank() ? "resume.pdf" : cleaned;
    }

    public byte[] readFile(String storedRelativePath) throws IOException {
        return Files.readAllBytes(resolveWithinUploadDir(storedRelativePath));
    }

    public void deleteFile(String storedRelativePath) {
        try {
            Files.deleteIfExists(resolveWithinUploadDir(storedRelativePath));
        } catch (IOException ignored) {
            // Matches the .NET DeleteFileAsync behavior: best-effort, doesn't
            // fail the surrounding operation (e.g. deleting a Resume row)
            // just because the underlying file was already gone.
        }
    }

    /**
     * Second line of defence for the read/delete side: even if a malformed
     * path somehow reached the database, refuse to touch anything outside the
     * configured uploads directory.
     */
    private Path resolveWithinUploadDir(String storedRelativePath) throws IOException {
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        Path resolved = root.resolve(storedRelativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("Refusing to access a path outside the uploads directory.");
        }
        return resolved;
    }
}

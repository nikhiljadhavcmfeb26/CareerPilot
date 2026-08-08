package com.careerpilot.userservice.dto.internal;

/**
 * Base64-wrapped file content rather than a raw byte[] response - this is an
 * internal Feign contract, not a browser-facing endpoint, and JSON (with a
 * base64 payload) is what a plain, unconfigured Feign client decodes without
 * needing a custom binary Decoder. application-service base64-decodes this
 * and streams it to the browser with the right headers itself. Fine for
 * resume-sized PDFs; not the shape you'd want for large media files.
 */
public record ResumeDownloadResponse(String fileName, String contentType, String base64Content) {
}

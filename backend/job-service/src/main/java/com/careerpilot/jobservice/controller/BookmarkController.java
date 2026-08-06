package com.careerpilot.jobservice.controller;

import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.SecurityUtils;
import com.careerpilot.jobservice.dto.BookmarkDto;
import com.careerpilot.jobservice.dto.CreateBookmarkRequest;
import com.careerpilot.jobservice.service.BookmarkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public ApiResponse<List<BookmarkDto>> getSaved() {
        return ApiResponse.ok(bookmarkService.getSavedJobs(SecurityUtils.currentUserId()));
    }

    @PostMapping
    public ApiResponse<BookmarkDto> save(@Valid @RequestBody CreateBookmarkRequest request) {
        return ApiResponse.ok(bookmarkService.saveJob(SecurityUtils.currentUserId(), request), "Job saved");
    }

    @DeleteMapping("/{jobId}")
    public ApiResponse<Void> remove(@PathVariable int jobId) {
        bookmarkService.removeBookmark(SecurityUtils.currentUserId(), jobId);
        return ApiResponse.success("Bookmark removed");
    }
}

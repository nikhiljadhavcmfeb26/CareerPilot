package com.careerpilot.jobservice.service;

import com.careerpilot.jobservice.dto.BookmarkDto;
import com.careerpilot.jobservice.dto.CreateBookmarkRequest;

import java.util.List;

public interface BookmarkService {
    BookmarkDto saveJob(int userId, CreateBookmarkRequest request);
    void removeBookmark(int userId, int jobId);
    List<BookmarkDto> getSavedJobs(int userId);
}

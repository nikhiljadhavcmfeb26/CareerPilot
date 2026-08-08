package com.careerpilot.jobservice.service.impl;

import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import com.careerpilot.jobservice.client.UserServiceClient;
import com.careerpilot.jobservice.dto.BookmarkDto;
import com.careerpilot.jobservice.dto.CreateBookmarkRequest;
import com.careerpilot.jobservice.entity.Bookmark;
import com.careerpilot.jobservice.entity.Job;
import com.careerpilot.jobservice.repository.BookmarkRepository;
import com.careerpilot.jobservice.repository.JobRepository;
import com.careerpilot.jobservice.service.BookmarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookmarkServiceImpl implements BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkServiceImpl.class);

    private final BookmarkRepository bookmarkRepository;
    private final JobRepository jobRepository;
    private final UserServiceClient userServiceClient;

    public BookmarkServiceImpl(BookmarkRepository bookmarkRepository, JobRepository jobRepository,
                                UserServiceClient userServiceClient) {
        this.bookmarkRepository = bookmarkRepository;
        this.jobRepository = jobRepository;
        this.userServiceClient = userServiceClient;
    }

    @Override
    @Transactional
    public BookmarkDto saveJob(int userId, CreateBookmarkRequest request) {
        int jobSeekerProfileId = getJobSeekerProfileId(userId, "Only job seekers can save jobs.");

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found."));

        bookmarkRepository.findByJobIdAndJobSeekerProfileId(request.getJobId(), jobSeekerProfileId).ifPresent(b -> {
            throw new BadRequestException("Job already saved.");
        });

        Bookmark bookmark = new Bookmark();
        bookmark.setJob(job);
        bookmark.setJobSeekerProfileId(jobSeekerProfileId);
        bookmarkRepository.save(bookmark);

        return toDto(bookmark);
    }

    @Override
    @Transactional
    public void removeBookmark(int userId, int jobId) {
        int jobSeekerProfileId = getJobSeekerProfileId(userId, "Only job seekers can manage bookmarks.");

        Bookmark bookmark = bookmarkRepository.findByJobIdAndJobSeekerProfileId(jobId, jobSeekerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found."));

        bookmarkRepository.delete(bookmark);
    }

    @Override
    public List<BookmarkDto> getSavedJobs(int userId) {
        int jobSeekerProfileId = getJobSeekerProfileId(userId, "Only job seekers have saved jobs.");
        return bookmarkRepository.findByJobSeekerProfileId(jobSeekerProfileId).stream().map(this::toDto).toList();
    }

    private int getJobSeekerProfileId(int userId, String errorMessage) {
        UserServiceClient.JobSeekerProfileLookup lookup;
        try {
            lookup = userServiceClient.getJobSeekerProfileForUser(userId);
        } catch (Exception ex) {
            throw new BadRequestException(errorMessage);
        }
        if (lookup == null || lookup.jobSeekerProfileId() == null) {
            throw new BadRequestException(errorMessage);
        }
        return lookup.jobSeekerProfileId();
    }

    private BookmarkDto toDto(Bookmark bookmark) {
        BookmarkDto dto = new BookmarkDto();
        dto.setId(bookmark.getId());
        dto.setJobId(bookmark.getJob().getId());
        dto.setJobTitle(bookmark.getJob().getTitle());
        dto.setLocation(bookmark.getJob().getLocation());
        dto.setCreatedAt(bookmark.getCreatedAt());

        try {
            UserServiceClient.CompanySummary summary = userServiceClient.getCompanySummary(bookmark.getJob().getCompanyId());
            dto.setCompanyName(summary != null ? summary.name() : null);
        } catch (Exception ex) {
            log.debug("user-service unavailable while hydrating company name for bookmark {}", bookmark.getId());
        }

        return dto;
    }
}

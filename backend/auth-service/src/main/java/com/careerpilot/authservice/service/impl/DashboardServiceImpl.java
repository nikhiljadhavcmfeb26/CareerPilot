package com.careerpilot.authservice.service.impl;

import com.careerpilot.authservice.client.ApplicationServiceClient;
import com.careerpilot.authservice.client.JobServiceClient;
import com.careerpilot.authservice.client.UserServiceClient;
import com.careerpilot.authservice.dto.dashboard.AdminDashboardDto;
import com.careerpilot.authservice.dto.dashboard.EmployerDashboardDto;
import com.careerpilot.authservice.dto.dashboard.JobSeekerDashboardDto;
import com.careerpilot.authservice.repository.UserRepository;
import com.careerpilot.authservice.service.DashboardService;
import com.careerpilot.common.exception.BadRequestException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

/**
 * Dashboard aggregation, hosted in auth-service and fanned out over OpenFeign.
 *
 * The original .NET DashboardService read every figure from one shared
 * database. Post-split, users live here, jobs and bookmarks live in
 * job-service, applications in application-service, and companies and resumes
 * in user-service - so the equivalent is a fan-out. auth-service was chosen as
 * the host because it already owns the caller's identity and role, and because
 * putting it anywhere else would have meant a service reaching sideways for
 * data it has no other reason to know about.
 *
 * DEGRADED-MODE POLICY: a dashboard is a read-only summary screen. If one
 * downstream service is momentarily unavailable, showing the rest of the
 * numbers with a zero in that tile is far better than failing the whole page.
 * Every remote figure therefore goes through safeCount()/safeStats(), which
 * logs the failure and substitutes a zero. This is deliberately NOT how the
 * transactional paths behave - apply(), updateStatus() and friends still fail
 * loudly, because a wrong number on a dashboard is cosmetic and a wrong write
 * is not.
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private static final String ROLE_EMPLOYER = "Employer";
    private static final String ROLE_JOB_SEEKER = "JobSeeker";

    private static final JobServiceClient.JobStats NO_JOBS = new JobServiceClient.JobStats(0, 0);
    private static final JobServiceClient.EmployerJobStats NO_EMPLOYER_JOBS =
            new JobServiceClient.EmployerJobStats(0, 0, List.of());
    private static final ApplicationServiceClient.ApplicationStats NO_APPLICATIONS =
            new ApplicationServiceClient.ApplicationStats(0, 0, 0);

    private final UserRepository userRepository;
    private final UserServiceClient userServiceClient;
    private final JobServiceClient jobServiceClient;
    private final ApplicationServiceClient applicationServiceClient;

    public DashboardServiceImpl(UserRepository userRepository, UserServiceClient userServiceClient,
                                 JobServiceClient jobServiceClient,
                                 ApplicationServiceClient applicationServiceClient) {
        this.userRepository = userRepository;
        this.userServiceClient = userServiceClient;
        this.jobServiceClient = jobServiceClient;
        this.applicationServiceClient = applicationServiceClient;
    }

    @Override
    public AdminDashboardDto getAdminDashboard() {
        // Users are local to this service - no remote call needed.
        int totalUsers = (int) userRepository.count();
        int totalEmployers = userRepository.countByRoleName(ROLE_EMPLOYER);
        int totalJobSeekers = userRepository.countByRoleName(ROLE_JOB_SEEKER);

        JobServiceClient.JobStats jobs = safe("job-service stats", jobServiceClient::getStats, NO_JOBS);
        ApplicationServiceClient.ApplicationStats applications =
                safe("application-service stats", applicationServiceClient::getStats, NO_APPLICATIONS);
        int pendingApprovals = safe("user-service pending companies", userServiceClient::countPendingCompanies, 0);

        return new AdminDashboardDto(totalUsers, totalEmployers, totalJobSeekers,
                jobs.totalJobs(), jobs.publishedJobs(), applications.total(), pendingApprovals);
    }

    @Override
    public EmployerDashboardDto getEmployerDashboard(int userId) {
        // An employer who hasn't registered a company yet is a normal state,
        // not an error - they land here immediately after registering. Show an
        // empty, not-approved dashboard so the "pending approval" banner and
        // the "Company Profile" call to action both render correctly.
        UserServiceClient.CompanyLookup company;
        try {
            company = userServiceClient.getCompanyForEmployerUser(userId);
        } catch (FeignException.NotFound ex) {
            return new EmployerDashboardDto(false, 0, 0, 0, 0);
        } catch (Exception ex) {
            log.error("user-service unavailable while loading employer dashboard for user {}", userId, ex);
            throw new BadRequestException("Could not load your dashboard right now. Please try again.");
        }

        JobServiceClient.EmployerJobStats jobs = safe("job-service employer stats",
                () -> jobServiceClient.getStatsByCompany(company.companyId()), NO_EMPLOYER_JOBS);

        ApplicationServiceClient.ApplicationStats applications = jobs.jobIds().isEmpty()
                ? NO_APPLICATIONS
                : safe("application-service employer stats",
                        () -> applicationServiceClient.getStatsByJobs(jobs.jobIds()), NO_APPLICATIONS);

        return new EmployerDashboardDto(company.isApproved(), jobs.totalJobs(), jobs.publishedJobs(),
                applications.total(), applications.pending());
    }

    @Override
    public JobSeekerDashboardDto getJobSeekerDashboard(int userId) {
        Integer profileId;
        try {
            UserServiceClient.JobSeekerProfileLookup lookup = userServiceClient.getJobSeekerProfileForUser(userId);
            profileId = lookup != null ? lookup.jobSeekerProfileId() : null;
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Only job seekers have a job seeker dashboard.");
        } catch (Exception ex) {
            log.error("user-service unavailable while loading job seeker dashboard for user {}", userId, ex);
            throw new BadRequestException("Could not load your dashboard right now. Please try again.");
        }

        if (profileId == null) {
            throw new BadRequestException("Only job seekers have a job seeker dashboard.");
        }

        final Integer resolvedProfileId = profileId;

        ApplicationServiceClient.ApplicationStats applications = safe("application-service job seeker stats",
                () -> applicationServiceClient.getStatsByProfile(resolvedProfileId), NO_APPLICATIONS);
        int savedJobs = safe("job-service bookmark count",
                () -> jobServiceClient.countBookmarksByProfile(resolvedProfileId), 0);
        int resumeCount = safe("user-service resume count",
                () -> userServiceClient.countResumesByProfile(resolvedProfileId), 0);

        return new JobSeekerDashboardDto(resumeCount > 0, applications.total(), savedJobs,
                applications.pending(), applications.shortlisted());
    }

    /** See the degraded-mode policy in this class's javadoc. */
    private <T> T safe(String what, Supplier<T> call, T fallback) {
        try {
            return call.get();
        } catch (Exception ex) {
            log.warn("Dashboard: {} unavailable, showing zeros for that section", what, ex);
            return fallback;
        }
    }
}

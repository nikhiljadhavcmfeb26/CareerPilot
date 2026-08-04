package com.careerpilot.jobservice.repository;

import com.careerpilot.jobservice.entity.Job;
import com.careerpilot.jobservice.enums.ExperienceLevel;
import com.careerpilot.jobservice.enums.JobStatus;
import com.careerpilot.jobservice.enums.JobType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors JobRepository.cs's SearchAsync exactly, with one unavoidable
 * change: the original also matched Company.Name via a SQL join, which no
 * longer exists once Company moves to user-service's own database. See
 * matchingCompanyIds - JobServiceImpl resolves that list via a Feign call to
 * user-service first, and this specification OR's it in, so keyword search
 * still matches on company name, just via a different mechanism than a join.
 */
public final class JobSpecifications {

    private JobSpecifications() {
    }

    public static Specification<Job> search(String keyword, String location, JobType jobType,
                                             ExperienceLevel experienceLevel, JobStatus status,
                                             List<Integer> matchingCompanyIds) {
        return (Root<Job> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descriptionMatch = cb.like(cb.lower(root.get("description")), pattern);

                if (matchingCompanyIds != null && !matchingCompanyIds.isEmpty()) {
                    Predicate companyMatch = root.get("companyId").in(matchingCompanyIds);
                    predicates.add(cb.or(titleMatch, descriptionMatch, companyMatch));
                } else {
                    predicates.add(cb.or(titleMatch, descriptionMatch));
                }
            }

            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
            }

            if (jobType != null) {
                predicates.add(cb.equal(root.get("jobType"), jobType));
            }

            if (experienceLevel != null) {
                predicates.add(cb.equal(root.get("experienceLevel"), experienceLevel));
            }

            // ORDER BY COALESCE(published_at, created_at) DESC, same as the .NET version.
            Expression<?> orderExpr = cb.coalesce(root.get("publishedAt"), root.get("createdAt"));
            query.orderBy(cb.desc(orderExpr));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

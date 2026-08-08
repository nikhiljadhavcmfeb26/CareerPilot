import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { jobApi, applicationApi, bookmarkApi, resumeApi } from '../../api/services';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../../components/LoadingSpinner';

const JobDetails = () => {

  const { id } = useParams();

  const { user, isAuthenticated } = useAuth();

  const [job, setJob] = useState(null);

  const [loading, setLoading] = useState(true);

  const [coverLetter, setCoverLetter] = useState('');

  const [applied, setApplied] = useState(false);

  // Applications used to be submitted with no resumeId at all, which left
  // every row with resume_id = NULL - breaking the employer's "View Resume"
  // and AI candidate screening. The candidate now picks one here.
  const [resumes, setResumes] = useState([]);

  const [selectedResumeId, setSelectedResumeId] = useState('');

  useEffect(() => {

    loadData();

  }, [id]);

  const loadData = async () => {

    try {

      // Load job details

      const jobResponse =
        await jobApi.getById(
          Number(id)
        );

      if (
        jobResponse.data.success
      ) {

        setJob(
          jobResponse.data.data
        );

      }

      // Load application status

      if (
        isAuthenticated &&
        user?.role === 'JobSeeker'
      ) {

        try {

          const statusResponse =
            await applicationApi.checkStatus(
              Number(id)
            );

          if (
            statusResponse.data.success
          ) {

            setApplied(
              statusResponse
                .data
                .data
                .applied
            );

          }

        }
        catch {

          console.log(
            "Could not load application status"
          );

        }

        try {

          const resumeResponse =
            await resumeApi.getMy();

          if (
            resumeResponse.data.success
          ) {

            const list =
              resumeResponse.data.data;

            setResumes(list);

            // Preselect the default resume so the common case is one click.
            const preferred =
              list.find((r) => r.isDefault) ||
              list[0];

            if (preferred) {
              setSelectedResumeId(
                String(preferred.id)
              );
            }

          }

        }
        catch {

          console.log(
            "Could not load resumes"
          );

        }

      }

    }
    catch {

      toast.error(
        "Failed to load job"
      );

    }
    finally {

      setLoading(false);

    }

  };

  const handleApply = async () => {

    try {

      await applicationApi.apply({

        jobId: Number(id),

        resumeId: selectedResumeId
          ? Number(selectedResumeId)
          : null,

        coverLetter

      });

      setApplied(true);

      toast.success(
        "Application submitted successfully!"
      );

    }
    catch (err) {

      const message =
        err.response?.data?.Message ||
        err.response?.data?.message;

      if (
        message === "Already applied"
      ) {

        setApplied(true);

        toast.info(
          "You already applied for this job"
        );

      }
      else {

        toast.error(
          message ||
          "Application failed"
        );

      }

    }

  };

  const handleSave = async () => {

    try {

      await bookmarkApi.save(
        Number(id)
      );

      toast.success(
        "Job saved!"
      );

    }
    catch (err) {

      const message =
        err.response?.data?.Message ||
        err.response?.data?.message;

      if (
        message ===
        "Job already saved."
      ) {

        toast.info(
          "Job already saved"
        );

        return;

      }

      toast.error(
        message ||
        "Failed to save"
      );

    }

  };

  if (loading)
    return (
      <LoadingSpinner fullPage />
    );

  if (!job)
    return (
      <div className="container py-5 text-center">
        Job not found
      </div>
    );

  return (

    <div className="container py-4">

      <div className="card border-0 shadow-sm p-4">

        <div className="d-flex justify-content-between align-items-start mb-3">

          <div>

            <h2 className="fw-bold">
              {job.title}
            </h2>

            <p className="text-primary fs-5 mb-1">
              {job.companyName}
            </p>

            <p className="text-muted">

              {job.location}
              {" · "}
              {job.jobType}
              {" · "}
              {job.experienceLevel}

            </p>

          </div>

          <span className="badge bg-success fs-6">
            {job.status}
          </span>

        </div>

        {job.salaryMin && (

          <p className="text-success fw-semibold">

            Salary:

            ₹
            {(job.salaryMin / 100000)
              .toFixed(1)}

            L -

            ₹
            {(job.salaryMax / 100000)
              .toFixed(1)}

            L per annum

          </p>

        )}

        <hr />

        <h5 className="fw-bold">
          Description
        </h5>

        <p>
          {job.description}
        </p>

        <h5 className="fw-bold mt-4">
          Requirements
        </h5>

        <p>
          {job.requirements}
        </p>

        {isAuthenticated &&
          user?.role === "JobSeeker" &&
          job.status === "Published" && (

          <div className="mt-4 p-3 bg-light rounded">

            <h5 className="fw-bold">
              Apply for this job
            </h5>

            {resumes.length > 0 ? (

              <div className="mb-3">

                <label className="form-label fw-semibold">
                  Attach resume
                </label>

                <select
                  className="form-select"
                  value={selectedResumeId}
                  onChange={(e) =>
                    setSelectedResumeId(
                      e.target.value
                    )
                  }
                  disabled={applied}
                >

                  {resumes.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.fileName}
                      {r.isDefault ? " (default)" : ""}
                    </option>
                  ))}

                </select>

              </div>

            ) : (

              <div className="alert alert-warning py-2 small">
                You have no resume uploaded. You can still apply, but employers
                won't be able to view a resume.{" "}
                <Link to="/jobseeker/resume" className="fw-semibold">
                  Upload one
                </Link>
              </div>

            )}

            <textarea
              className="form-control mb-3"
              rows="3"
              placeholder="Cover letter (optional)"
              value={coverLetter}
              onChange={(e) =>
                setCoverLetter(
                  e.target.value
                )
              }
            />

            <div className="d-flex gap-2">

              <button
                className="btn btn-primary"
                onClick={handleApply}
                disabled={applied}
              >

                {
                  applied
                  ? "Applied"
                  : "Apply Now"
                }

              </button>

              <button
                className="btn btn-outline-secondary"
                onClick={handleSave}
              >

                Save Job

              </button>

              <Link
                className="btn btn-outline-warning"
                to={`/jobseeker/ai/cover-letter?jobId=${id}`}
              >

                <i className="bi bi-stars me-1"></i>
                AI Cover Letter

              </Link>

            </div>

          </div>

        )}

      </div>

    </div>

  );

};

export default JobDetails;
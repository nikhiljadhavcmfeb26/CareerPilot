import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { resumeApi, openPdfResponse } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

const Resume = () => {
  const [resumes, setResumes] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchResumes = () => {
    resumeApi.getMy().then(({ data }) => {
      if (data.success) setResumes(data.data);
    }).catch((err) => {
      toast.error(err.response?.data?.message || 'Could not load your resumes');
    }).finally(() => setLoading(false));
  };

  useEffect(() => { fetchResumes(); }, []);

  const handleView = async (resume) => {
    try {
      const response = await resumeApi.download(resume.id);
      openPdfResponse(response, resume.fileName);
    } catch {
      toast.error('Could not open this resume');
    }
  };

  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    try {
      const { data } = await resumeApi.upload(file);
      if (data.success) {
        toast.success('Resume uploaded!');
        fetchResumes();
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Upload failed');
    }
  };

  const handleDelete = async (id) => {
    try {
      await resumeApi.delete(id);
      toast.success('Resume deleted');
      fetchResumes();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Delete failed');
    }
  };

  const handleSetDefault = async (id) => {
    try {
      await resumeApi.setDefault(id);
      toast.success('Default resume set');
      fetchResumes();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed');
    }
  };

  if (loading) return <LoadingSpinner fullPage />;

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-4">My Resumes</h2>
      <div className="card border-0 shadow-sm p-4 mb-4 col-lg-8">
        <label className="form-label fw-semibold">Upload Resume (PDF only)</label>
        <input type="file" className="form-control" accept=".pdf" onChange={handleUpload} />
      </div>
      <div className="col-lg-8">
        {resumes.length === 0 ? (
          <p className="text-muted">No resumes uploaded yet.</p>
        ) : resumes.map((r) => (
          <div key={r.id} className="card border-0 shadow-sm p-3 mb-2 d-flex flex-row justify-content-between align-items-center">
            <div>
              <span className="fw-semibold">{r.fileName}</span>
              {r.isDefault && <span className="badge bg-primary ms-2">Default</span>}
              <small className="text-muted d-block">{new Date(r.uploadedAt).toLocaleDateString()}</small>
            </div>
            <div>
              <button className="btn btn-sm btn-outline-secondary me-2" onClick={() => handleView(r)}>View</button>
              {!r.isDefault && <button className="btn btn-sm btn-outline-primary me-2" onClick={() => handleSetDefault(r.id)}>Set Default</button>}
              <button className="btn btn-sm btn-outline-danger" onClick={() => handleDelete(r.id)}>Delete</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Resume;

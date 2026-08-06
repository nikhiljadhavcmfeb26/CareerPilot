import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { bookmarkApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

const SavedJobs = () => {
  const [bookmarks, setBookmarks] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    bookmarkApi.getSaved().then(({ data }) => {
      if (data.success) setBookmarks(data.data);
    }).catch((err) => {
      toast.error(err.response?.data?.message || 'Could not load saved jobs');
    }).finally(() => setLoading(false));
  }, []);

  const handleRemove = async (jobId) => {
    try {
      await bookmarkApi.remove(jobId);
      toast.success('Removed from saved jobs');
      setBookmarks(bookmarks.filter((b) => b.jobId !== jobId));
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to remove');
    }
  };

  if (loading) return <LoadingSpinner fullPage />;

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-4">Saved Jobs</h2>
      {bookmarks.length === 0 ? (
        <p className="text-muted">No saved jobs. <Link to="/jobs">Browse jobs</Link></p>
      ) : bookmarks.map((b) => (
        <div key={b.id} className="card border-0 shadow-sm p-3 mb-3 d-flex flex-row justify-content-between align-items-center">
          <div>
            <Link to={`/jobs/${b.jobId}`} className="fw-semibold text-decoration-none">{b.jobTitle}</Link>
            <p className="text-muted mb-0 small">{b.companyName} · {b.location}</p>
          </div>
          <button className="btn btn-sm btn-outline-danger" onClick={() => handleRemove(b.jobId)}>Remove</button>
        </div>
      ))}
    </div>
  );
};

export default SavedJobs;

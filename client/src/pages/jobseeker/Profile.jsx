import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { profileApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

const Profile = () => {
  const [form, setForm] = useState({ headline: '', summary: '', skills: '', experience: '', education: '', location: '' });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    profileApi.get().then(({ data }) => {
      if (data.success) setForm(data.data);
    }).catch((err) => {
      toast.error(err.response?.data?.message || 'Could not load your profile');
    }).finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const { data } = await profileApi.update(form);
      if (data.success) toast.success('Profile updated!');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update');
    }
  };

  if (loading) return <LoadingSpinner fullPage />;

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-4">My Profile</h2>
      <div className="card border-0 shadow-sm p-4 col-lg-8">
        <form onSubmit={handleSubmit}>
          {[
            { key: 'headline', label: 'Headline', rows: 1 },
            { key: 'summary', label: 'Summary', rows: 3 },
            { key: 'skills', label: 'Skills', rows: 2 },
            { key: 'experience', label: 'Experience', rows: 2 },
            { key: 'education', label: 'Education', rows: 2 },
            { key: 'location', label: 'Location', rows: 1 }
          ].map(({ key, label, rows }) => (
            <div key={key} className="mb-3">
              <label className="form-label">{label}</label>
              {rows === 1 ? (
                <input className="form-control" value={form[key] || ''} onChange={(e) => setForm({ ...form, [key]: e.target.value })} />
              ) : (
                <textarea className="form-control" rows={rows} value={form[key] || ''} onChange={(e) => setForm({ ...form, [key]: e.target.value })} />
              )}
            </div>
          ))}
          <button type="submit" className="btn btn-primary">Save Profile</button>
        </form>
      </div>
    </div>
  );
};

export default Profile;

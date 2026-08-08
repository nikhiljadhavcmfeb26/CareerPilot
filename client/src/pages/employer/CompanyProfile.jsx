import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { companyApi } from '../../api/services';
import LoadingSpinner from '../../components/LoadingSpinner';

const CompanyProfile = () => {
  const [form, setForm] = useState({ name: '', description: '', website: '', industry: '', location: '' });
  const [loading, setLoading] = useState(true);
  const [isNew, setIsNew] = useState(true);

  useEffect(() => {
    companyApi.getMy().then(({ data }) => {
      if (data.success) {
        setForm(data.data);
        setIsNew(false);
      }
    }).catch(() => setIsNew(true)).finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const api = isNew ? companyApi.register : companyApi.update;
      const { data } = await api(form);
      if (data.success) {
        toast.success(isNew ? 'Company registered!' : 'Company updated!');
        setIsNew(false);
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save');
    }
  };

  if (loading) return <LoadingSpinner fullPage />;

  return (
    <div className="container py-4">
      <h2 className="fw-bold mb-4">{isNew ? 'Register Company' : 'Company Profile'}</h2>
      <div className="card border-0 shadow-sm p-4 col-lg-8">
        <form onSubmit={handleSubmit}>
          {['name', 'industry', 'location', 'website'].map((field) => (
            <div key={field} className="mb-3">
              <label className="form-label text-capitalize">{field}</label>
              <input className="form-control" required={field !== 'website'} value={form[field] || ''}
                onChange={(e) => setForm({ ...form, [field]: e.target.value })} />
            </div>
          ))}
          <div className="mb-3">
            <label className="form-label">Description</label>
            <textarea className="form-control" rows="4" required value={form.description || ''}
              onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <button type="submit" className="btn btn-primary">{isNew ? 'Register' : 'Update'}</button>
        </form>
      </div>
    </div>
  );
};

export default CompanyProfile;

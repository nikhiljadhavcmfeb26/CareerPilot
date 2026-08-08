import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ProtectedRoute from './components/ProtectedRoute';

import HomeRoute from './components/HomeRoute';
import About from './pages/public/About';
import Contact from './pages/public/Contact';
import Login from './pages/public/Login';
import Register from './pages/public/Register';
import Jobs from './pages/public/Jobs';
import JobDetails from './pages/public/JobDetails';
import NotFound from './pages/public/NotFound';
import ForgotPassword from './pages/public/ForgotPassword';
import Settings from './pages/Settings';
import Premium from './pages/Premium';
import PremiumGate from './components/PremiumGate';

import AiResume from './pages/jobseeker/ai/AiResume';
import AiCoverLetter from './pages/jobseeker/ai/AiCoverLetter';
import AiRecommendations from './pages/jobseeker/ai/AiRecommendations';
import AiScreening from './pages/employer/ai/AiScreening';

import EmployerDashboard from './pages/employer/Dashboard';
import CompanyProfile from './pages/employer/CompanyProfile';
import CreateJob from './pages/employer/CreateJob';
import ManageJobs from './pages/employer/ManageJobs';
import Applicants from './pages/employer/Applicants';

import JobSeekerDashboard from './pages/jobseeker/Dashboard';
import Profile from './pages/jobseeker/Profile';
import Resume from './pages/jobseeker/Resume';
import SavedJobs from './pages/jobseeker/SavedJobs';
import AppliedJobs from './pages/jobseeker/AppliedJobs';

import AdminDashboard from './pages/admin/Dashboard';
import AdminUsers from './pages/admin/Users';
import AdminEmployers from './pages/admin/Employers';
import AdminJobs from './pages/admin/Jobs';
import AdminApplications from './pages/admin/Applications';
import AdminSubscriptions from './pages/admin/Subscriptions';
import AdminAiSettings from './pages/admin/AiSettings';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="d-flex flex-column min-vh-100">
          <Navbar />
          <main className="flex-grow-1">
            <Routes>
              <Route path="/" element={<HomeRoute />} />
              <Route path="/about" element={<About />} />
              <Route path="/contact" element={<Contact />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />
              <Route path="/jobs" element={<Jobs />} />
              <Route path="/jobs/:id" element={<JobDetails />} />

              {/* Premium is available to JobSeekers and Employers - both have
                  AI features gated behind it. Admin has no AI features, so no
                  role list is needed beyond "authenticated". */}
              <Route path="/premium" element={<ProtectedRoute><Premium /></ProtectedRoute>} />

              <Route path="/employer/dashboard" element={<ProtectedRoute roles={['Employer']}><EmployerDashboard /></ProtectedRoute>} />
              <Route path="/employer/company" element={<ProtectedRoute roles={['Employer']}><CompanyProfile /></ProtectedRoute>} />
              <Route path="/employer/jobs/create" element={<ProtectedRoute roles={['Employer']}><CreateJob /></ProtectedRoute>} />
              <Route path="/employer/jobs" element={<ProtectedRoute roles={['Employer']}><ManageJobs /></ProtectedRoute>} />
              <Route path="/employer/applicants/:jobId" element={<ProtectedRoute roles={['Employer']}><Applicants /></ProtectedRoute>} />
              <Route path="/employer/settings" element={<ProtectedRoute roles={['Employer']}><Settings /></ProtectedRoute>} />

              {/* AI features. Two independent layers, on purpose:
                  ProtectedRoute enforces the ROLE (ai-service also checks it),
                  PremiumGate shows the upgrade prompt to non-premium users.
                  Neither is the security boundary - ai-service re-checks both
                  the role and the subscription on every request. */}
              <Route path="/employer/ai/screening" element={<ProtectedRoute roles={['Employer']}><PremiumGate feature="AI candidate screening"><AiScreening /></PremiumGate></ProtectedRoute>} />
              <Route path="/employer/ai/screening/:jobId" element={<ProtectedRoute roles={['Employer']}><PremiumGate feature="AI candidate screening"><AiScreening /></PremiumGate></ProtectedRoute>} />

              <Route path="/jobseeker/dashboard" element={<ProtectedRoute roles={['JobSeeker']}><JobSeekerDashboard /></ProtectedRoute>} />
              <Route path="/jobseeker/profile" element={<ProtectedRoute roles={['JobSeeker']}><Profile /></ProtectedRoute>} />
              <Route path="/jobseeker/resume" element={<ProtectedRoute roles={['JobSeeker']}><Resume /></ProtectedRoute>} />
              <Route path="/jobseeker/saved" element={<ProtectedRoute roles={['JobSeeker']}><SavedJobs /></ProtectedRoute>} />
              <Route path="/jobseeker/applied" element={<ProtectedRoute roles={['JobSeeker']}><AppliedJobs /></ProtectedRoute>} />
              <Route path="/jobseeker/settings" element={<ProtectedRoute roles={['JobSeeker']}><Settings /></ProtectedRoute>} />

              <Route path="/jobseeker/ai/resume" element={<ProtectedRoute roles={['JobSeeker']}><PremiumGate feature="AI resume analysis"><AiResume /></PremiumGate></ProtectedRoute>} />
              <Route path="/jobseeker/ai/cover-letter" element={<ProtectedRoute roles={['JobSeeker']}><PremiumGate feature="The AI cover letter generator"><AiCoverLetter /></PremiumGate></ProtectedRoute>} />
              <Route path="/jobseeker/ai/recommendations" element={<ProtectedRoute roles={['JobSeeker']}><PremiumGate feature="AI job recommendations"><AiRecommendations /></PremiumGate></ProtectedRoute>} />

              <Route path="/admin/dashboard" element={<ProtectedRoute roles={['Admin']}><AdminDashboard /></ProtectedRoute>} />
              <Route path="/admin/users" element={<ProtectedRoute roles={['Admin']}><AdminUsers /></ProtectedRoute>} />
              <Route path="/admin/employers" element={<ProtectedRoute roles={['Admin']}><AdminEmployers /></ProtectedRoute>} />
              <Route path="/admin/jobs" element={<ProtectedRoute roles={['Admin']}><AdminJobs /></ProtectedRoute>} />
              <Route path="/admin/applications" element={<ProtectedRoute roles={['Admin']}><AdminApplications /></ProtectedRoute>} />
              <Route path="/admin/subscriptions" element={<ProtectedRoute roles={['Admin']}><AdminSubscriptions /></ProtectedRoute>} />
              <Route path="/admin/ai" element={<ProtectedRoute roles={['Admin']}><AdminAiSettings /></ProtectedRoute>} />
              <Route path="/admin/settings" element={<ProtectedRoute roles={['Admin']}><Settings /></ProtectedRoute>} />

              <Route path="*" element={<NotFound />} />
            </Routes>
          </main>
          <Footer />
        </div>
        <ToastContainer position="top-right" autoClose={3000} />
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;

import { useParams, useNavigate, Link } from 'react-router-dom';
import { useProfile, useProfilePermissions, useDeleteProfile } from '../hooks/useProfile';
import { useSubmitFeedback } from '../hooks/useFeedback';
import { useSubmitAbsenceRequest } from '../hooks/useAbsence';
import { useAuthStore } from '../store/authStore';
import { useState } from 'react';

export const ProfileDetail: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);
  const { mutate: deleteProfile } = useDeleteProfile();
  const { mutate: submitFeedback } = useSubmitFeedback();
  const { mutate: submitAbsenceRequest } = useSubmitAbsenceRequest();

  const [showFeedbackForm, setShowFeedbackForm] = useState(false);
  const [feedbackContent, setFeedbackContent] = useState('');
  const [showAbsenceForm, setShowAbsenceForm] = useState(false);
  const [absenceData, setAbsenceData] = useState({
    startDate: '',
    endDate: '',
    type: 'VACATION',
    reason: '',
  });

  const id = userId ? parseInt(userId, 10) : 0;
  const { data: profile, isLoading, error } = useProfile(id);
  const { data: permissions } = useProfilePermissions(id);

  if (isLoading) {
    return (
      <div className="flex justify-center items-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
        Error loading profile: {error instanceof Error ? error.message : 'Profile not found'}
      </div>
    );
  }

  const handleDelete = () => {
    if (confirm(`Are you sure you want to delete ${profile.firstName} ${profile.lastName}?`)) {
      deleteProfile(
        { userId: profile.id },
        {
          onSuccess: () => {
            navigate('/profiles');
          },
          onError: (error: any) => {
            alert('Error deleting profile: ' + (error.message || 'Unknown error'));
          },
        }
      );
    }
  };

  const handleSubmitFeedback = () => {
    if (!feedbackContent.trim()) {
      alert('Please enter feedback content');
      return;
    }
    if (!currentUser) return;
    
    submitFeedback(
      {
        fromUserId: currentUser.id,
        toUserId: profile.id,
        content: feedbackContent,
      },
      {
        onSuccess: () => {
          setFeedbackContent('');
          setShowFeedbackForm(false);
          alert('Feedback submitted successfully!');
        },
        onError: (error: any) => {
          alert('Error submitting feedback: ' + (error.message || 'Unknown error'));
        },
      }
    );
  };

  const handleSubmitAbsence = () => {
    if (!absenceData.startDate || !absenceData.endDate) {
      alert('Please fill in all required fields');
      return;
    }
    
    submitAbsenceRequest(
      {
        userId: profile.id,
        startDate: absenceData.startDate,
        endDate: absenceData.endDate,
        type: absenceData.type,
        reason: absenceData.reason,
      },
      {
        onSuccess: () => {
          setAbsenceData({ startDate: '', endDate: '', type: 'VACATION', reason: '' });
          setShowAbsenceForm(false);
          alert('Absence request submitted successfully!');
        },
        onError: (error: any) => {
          alert('Error submitting absence request: ' + (error.message || 'Unknown error'));
        },
      }
    );
  };

  return (
    <div className="max-w-4xl mx-auto">
      <Link to="/profiles" className="text-blue-600 hover:text-blue-800 mb-4 inline-block">
        ← Back to Profiles
      </Link>

      <div className="bg-white rounded-lg shadow p-8">
        <div className="flex justify-between items-start mb-6">
          <div>
            <h1 className="text-3xl font-bold">
              {profile.firstName} {profile.lastName}
            </h1>
            <p className="text-gray-600 text-lg">{profile.email}</p>
          </div>
          <span className="inline-block bg-blue-100 text-blue-800 px-4 py-2 rounded-full font-semibold">
            {profile.role}
          </span>
        </div>

        <div className="grid grid-cols-2 gap-6 mb-8">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Phone</label>
            <p className="text-gray-900">{profile.phone || 'Not provided'}</p>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Department</label>
            <p className="text-gray-900">{profile.department || 'Not provided'}</p>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Member Since</label>
            <p className="text-gray-900">
              {profile.createdAt ? new Date(profile.createdAt).toLocaleDateString() : 'N/A'}
            </p>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Last Updated</label>
            <p className="text-gray-900">
              {profile.updatedAt ? new Date(profile.updatedAt).toLocaleDateString() : 'N/A'}
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-wrap gap-4 border-t pt-6 mb-6">
          {permissions?.canEdit && (
            <Link
              to={`/profiles/${profile.id}/edit`}
              className="bg-green-500 text-white px-6 py-2 rounded hover:bg-green-600 transition"
            >
              Edit Profile
            </Link>
          )}

          {permissions?.canDelete && (
            <button
              onClick={handleDelete}
              className="bg-red-500 text-white px-6 py-2 rounded hover:bg-red-600 transition"
            >
              Delete Profile
            </button>
          )}

          {currentUser?.id !== profile.id && (
            <>
              <button
                onClick={() => setShowFeedbackForm(!showFeedbackForm)}
                className="bg-blue-500 text-white px-6 py-2 rounded hover:bg-blue-600 transition"
              >
                {showFeedbackForm ? 'Cancel Feedback' : 'Give Feedback'}
              </button>

              <button
                onClick={() => setShowAbsenceForm(!showAbsenceForm)}
                className="bg-purple-500 text-white px-6 py-2 rounded hover:bg-purple-600 transition"
              >
                {showAbsenceForm ? 'Cancel Request' : 'Request Absence'}
              </button>
            </>
          )}

          {!permissions?.canEdit && !permissions?.canDelete && currentUser?.id === profile.id && (
            <div className="text-gray-600 italic">
              This is your profile
            </div>
          )}
        </div>

        {/* Feedback Form */}
        {showFeedbackForm && (
          <div className="bg-blue-50 border border-blue-200 rounded p-6 mb-6">
            <h3 className="text-lg font-semibold mb-4">Give Feedback to {profile.firstName}</h3>
            <textarea
              value={feedbackContent}
              onChange={(e) => setFeedbackContent(e.target.value)}
              placeholder="Enter your feedback..."
              className="w-full border rounded px-3 py-2 mb-4 h-24 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              onClick={handleSubmitFeedback}
              className="bg-blue-500 text-white px-6 py-2 rounded hover:bg-blue-600 transition"
            >
              Submit Feedback
            </button>
          </div>
        )}

        {/* Absence Request Form */}
        {showAbsenceForm && (
          <div className="bg-purple-50 border border-purple-200 rounded p-6">
            <h3 className="text-lg font-semibold mb-4">Request Absence for {profile.firstName}</h3>
            <div className="grid grid-cols-2 gap-4 mb-4">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">Start Date</label>
                <input
                  type="date"
                  value={absenceData.startDate}
                  onChange={(e) => setAbsenceData({ ...absenceData, startDate: e.target.value })}
                  className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">End Date</label>
                <input
                  type="date"
                  value={absenceData.endDate}
                  onChange={(e) => setAbsenceData({ ...absenceData, endDate: e.target.value })}
                  className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
            </div>
            <div className="mb-4">
              <label className="block text-sm font-semibold text-gray-700 mb-2">Type</label>
              <select
                value={absenceData.type}
                onChange={(e) => setAbsenceData({ ...absenceData, type: e.target.value })}
                className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
              >
                <option value="VACATION">Vacation</option>
                <option value="SICK">Sick Leave</option>
                <option value="PERSONAL">Personal</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div className="mb-4">
              <label className="block text-sm font-semibold text-gray-700 mb-2">Reason</label>
              <textarea
                value={absenceData.reason}
                onChange={(e) => setAbsenceData({ ...absenceData, reason: e.target.value })}
                placeholder="Enter reason (optional)"
                className="w-full border rounded px-3 py-2 h-20 focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
            </div>
            <button
              onClick={handleSubmitAbsence}
              className="bg-purple-500 text-white px-6 py-2 rounded hover:bg-purple-600 transition"
            >
              Submit Request
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

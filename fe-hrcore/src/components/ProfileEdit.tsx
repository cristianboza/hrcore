import { useParams, useNavigate, Link } from 'react-router-dom';
import { useState } from 'react';
import { useProfile, useUpdateProfile } from '../hooks/useProfile';
import { useAuthStore } from '../store/authStore';
import { User } from '../services/userService';

export const ProfileEdit: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);
  const { mutate: updateProfile, isPending } = useUpdateProfile();

  const id = userId ? parseInt(userId, 10) : 0;
  const { data: profile, isLoading, error } = useProfile(id);

  const [formData, setFormData] = useState<Partial<User>>({
    firstName: profile?.firstName || '',
    lastName: profile?.lastName || '',
    email: profile?.email || '',
    phone: profile?.phone || '',
    department: profile?.department || '',
    role: profile?.role || 'EMPLOYEE',
  });

  const [submitError, setSubmitError] = useState<string | null>(null);

  // Update form data when profile loads
  if (profile && (formData.firstName !== profile.firstName)) {
    setFormData({
      firstName: profile.firstName,
      lastName: profile.lastName,
      email: profile.email,
      phone: profile.phone,
      department: profile.department,
      role: profile.role,
    });
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);

    updateProfile(
      {
        userId: id,
        updateData: formData,
      },
      {
        onSuccess: () => {
          navigate(`/profiles/${id}`);
        },
        onError: (error) => {
          setSubmitError(error instanceof Error ? error.message : 'Failed to update profile');
        },
      }
    );
  };

  // Security: Only allow edit if currentUser is owner, manager, or super admin
  if (!currentUser || (currentUser.id !== id && currentUser.role !== 'MANAGER' && currentUser.role !== 'SUPER_ADMIN')) {
    return (
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
        You do not have permission to edit this profile.
      </div>
    );
  }

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

  return (
    <div className="max-w-2xl mx-auto">
      <Link to={`/profiles/${id}`} className="text-blue-600 hover:text-blue-800 mb-4 inline-block">
        ← Back to Profile
      </Link>

      <div className="bg-white rounded-lg shadow p-8">
        <h1 className="text-3xl font-bold mb-6">Edit Profile</h1>

        {submitError && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">
            {submitError}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid grid-cols-2 gap-6">
            {/* First Name */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">First Name *</label>
              <input
                type="text"
                name="firstName"
                value={formData.firstName || ''}
                onChange={handleInputChange}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Last Name */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Last Name *</label>
              <input
                type="text"
                name="lastName"
                value={formData.lastName || ''}
                onChange={handleInputChange}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Email *</label>
            <input
              type="email"
              name="email"
              value={formData.email || ''}
              onChange={handleInputChange}
              required
              disabled
              className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-100 text-gray-600 cursor-not-allowed"
            />
            <p className="text-sm text-gray-600 mt-1">Email cannot be changed</p>
          </div>

          <div className="grid grid-cols-2 gap-6">
            {/* Phone */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Phone</label>
              <input
                type="tel"
                name="phone"
                value={formData.phone || ''}
                onChange={handleInputChange}
                placeholder="+1-555-1234"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Department */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Department</label>
              <input
                type="text"
                name="department"
                value={formData.department || ''}
                onChange={handleInputChange}
                placeholder="Engineering"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Role - Only editable by managers and super admin */}
          {(currentUser?.role === 'SUPER_ADMIN' || currentUser?.role === 'MANAGER') && (
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Role</label>
              <select
                name="role"
                value={formData.role || 'EMPLOYEE'}
                onChange={handleInputChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="EMPLOYEE">Employee</option>
                <option value="MANAGER">Manager</option>
                <option value="SUPER_ADMIN">Super Admin</option>
              </select>
            </div>
          )}

          {/* Submit Buttons */}
          <div className="flex gap-4 border-t pt-6">
            <button
              type="submit"
              disabled={isPending}
              className="bg-blue-500 text-white px-6 py-2 rounded hover:bg-blue-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isPending ? 'Saving...' : 'Save Changes'}
            </button>
            <Link
              to={`/profiles/${id}`}
              className="bg-gray-400 text-white px-6 py-2 rounded hover:bg-gray-500 transition"
            >
              Cancel
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
};

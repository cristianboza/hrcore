import { Link } from 'react-router-dom';
import { User } from '../services/userService';

interface ProfileCardProps {
  user: User;
  canEdit?: boolean;
  canDelete?: boolean;
  onDelete?: (userId: number) => void;
}

export const ProfileCard: React.FC<ProfileCardProps> = ({ user, canEdit = false, canDelete = false, onDelete }) => {
  const handleDelete = () => {
    if (confirm(`Are you sure you want to delete ${user.firstName} ${user.lastName}?`)) {
      if (onDelete) {
        onDelete(user.id);
      }
    }
  };

  return (
    <div className="bg-white rounded-lg shadow p-6 mb-4 hover:shadow-lg transition-shadow">
      <div className="flex justify-between items-start">
        <div className="flex-1">
          <h3 className="text-xl font-bold text-gray-900">
            {user.firstName} {user.lastName}
          </h3>
          <p className="text-gray-600">{user.email}</p>

          <div className="mt-4 grid grid-cols-2 gap-4 text-sm">
            {user.phone && (
              <div>
                <span className="font-semibold text-gray-700">Phone:</span>
                <p className="text-gray-600">{user.phone}</p>
              </div>
            )}
            {user.department && (
              <div>
                <span className="font-semibold text-gray-700">Department:</span>
                <p className="text-gray-600">{user.department}</p>
              </div>
            )}
          </div>

          <div className="mt-4">
            <span className="inline-block bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-semibold">
              {user.role}
            </span>
          </div>
        </div>

        <div className="flex gap-2">
          <Link
            to={`/profiles/${user.id}`}
            className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 transition"
          >
            View
          </Link>

          {canEdit && (
            <Link
              to={`/profiles/${user.id}/edit`}
              className="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-600 transition"
            >
              Edit
            </Link>
          )}

          {canDelete && (
            <button
              onClick={handleDelete}
              className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600 transition"
            >
              Delete
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

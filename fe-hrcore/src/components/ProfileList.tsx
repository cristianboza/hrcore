import { useProfiles } from '../hooks/useProfile';
import { useDeleteProfile } from '../hooks/useProfile';
import { ProfileCard } from './ProfileCard';
import { useAuthStore } from '../store/authStore';

export const ProfileList: React.FC = () => {
  const currentUser = useAuthStore((state) => state.currentUser);
  const { data: profiles, isLoading, error, refetch } = useProfiles();
  const { mutate: deleteProfile } = useDeleteProfile();

  const handleDelete = (userId: number) => {
    deleteProfile(
      { userId },
      {
        onSuccess: () => {
          alert('Profile deleted successfully');
          refetch();
        },
        onError: (error: any) => {
          console.error('Delete error:', error);
          alert('Error deleting profile: ' + (error.message || 'Unknown error'));
        },
      }
    );
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
        Error loading profiles: {error instanceof Error ? error.message : 'Unknown error'}
      </div>
    );
  }

  if (!profiles || profiles.length === 0) {
    return (
      <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded">
        No profiles available
      </div>
    );
  }

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">Employee Profiles</h2>
      <div className="space-y-4">
        {profiles.map((profile) => (
          <ProfileCard
            key={profile.id}
            user={profile}
            canEdit={currentUser?.id === profile.id || currentUser?.role === 'SUPER_ADMIN' || currentUser?.role === 'MANAGER'}
            canDelete={currentUser?.role === 'SUPER_ADMIN' || currentUser?.role === 'MANAGER'}
            onDelete={handleDelete}
          />
        ))}
      </div>
    </div>
  );
};

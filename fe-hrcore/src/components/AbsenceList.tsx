import { useAbsenceRequests } from '../hooks/useAbsence';
import { useAuthStore } from '../store/authStore';
import { AbsenceCard } from './AbsenceCard';

export const AbsenceList: React.FC = () => {
  const currentUser = useAuthStore((state) => state.currentUser);
  const { data: requests, isLoading, error } = useAbsenceRequests(currentUser?.id ?? 0);

  if (!currentUser) return null;

  if (isLoading) {
    return <div>Loading your absence requests...</div>;
  }
  if (error) {
    return <div>Error loading absence requests</div>;
  }

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">My Absence Requests</h2>
      {requests && requests.length > 0 ? (
        requests.map((req) => <AbsenceCard key={req.id} request={req} />)
      ) : (
        <div>No absence requests found.</div>
      )}
    </div>
  );
};


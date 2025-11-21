import { useAbsenceRequests } from '../hooks/useAbsence';
import { useAuthStore } from '../store/authStore';
import { AbsenceCard } from './AbsenceCard';

export const AbsenceList: React.FC = () => {
  const currentUser = useAuthStore((state) => state.currentUser);
  const { data: requestsPage, isLoading, error } = useAbsenceRequests(currentUser?.id ?? '', 0, 10);

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
      {requestsPage && requestsPage.content.length > 0 ? (
        requestsPage.content.map((req) => <AbsenceCard key={req.id} request={req} />)
      ) : (
        <div>No absence requests found.</div>
      )}
    </div>
  );
};


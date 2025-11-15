import { usePendingAbsenceRequests } from '../hooks/useAbsence';
import { useAuthStore } from '../store/authStore';
import { AbsenceCard } from './AbsenceCard';
import { useManagerUpdateAbsenceRequest } from '../hooks/useAbsence';

export const AbsenceManagerPanel: React.FC = () => {
  const currentUser = useAuthStore((state) => state.currentUser);
  const { data: requests, isLoading, error } = usePendingAbsenceRequests();
  const { mutate: managerUpdate } = useManagerUpdateAbsenceRequest();

  if (!currentUser || (currentUser.role !== 'MANAGER' && currentUser.role !== 'SUPER_ADMIN')) {
    return null;
  }

  if (isLoading) {
    return <div>Loading absence requests...</div>;
  }
  if (error) {
    return <div>Error loading absence requests</div>;
  }

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">Pending Absence Requests (Manager)</h2>
      {requests && requests.length > 0 ? (
        requests.map((req) => (
          <AbsenceCard
            key={req.id}
            request={req}
            onManagerUpdate={(id, status, comment) =>
              managerUpdate({ requestId: id, managerId: currentUser.id, status, managerComment: comment })
            }
          />
        ))
      ) : (
        <div>No pending requests.</div>
      )}
    </div>
  );
};


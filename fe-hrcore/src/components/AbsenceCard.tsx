import { AbsenceRequest, ABSENCE_STATUS } from '../types/absence';

interface AbsenceCardProps {
  request: AbsenceRequest;
  onApprove?: (id: number) => void;
  onReject?: (id: number, reason: string) => void;
  onManagerUpdate?: (id: number, status: string, comment?: string) => void;
}

export const AbsenceCard: React.FC<AbsenceCardProps> = ({ request, onApprove, onReject, onManagerUpdate }) => {
  const statusColor = {
    [ABSENCE_STATUS.PENDING]: 'bg-yellow-100 text-yellow-800',
    [ABSENCE_STATUS.APPROVED]: 'bg-green-100 text-green-800',
    [ABSENCE_STATUS.REJECTED]: 'bg-red-100 text-red-800',
  };

  const startDate = new Date(request.startDate).toLocaleDateString();
  const endDate = new Date(request.endDate).toLocaleDateString();

  return (
    <div className="bg-white rounded-lg shadow p-6 mb-4">
      <div className="flex justify-between items-start mb-4">
        <div>
          <p className="text-sm text-gray-600">User ID: {request.userId}</p>
          <p className="text-lg font-semibold text-gray-900">{request.type}</p>
        </div>
        <span className={`px-3 py-1 rounded-full text-sm font-semibold ${statusColor[request.status]}`}>
          {request.status}
        </span>
      </div>

      <div className="mb-4">
        <p className="text-gray-700">
          <span className="font-semibold">Dates:</span> {startDate} to {endDate}
        </p>
        {request.reason && (
          <p className="text-gray-700 mt-2">
            <span className="font-semibold">Reason:</span> {request.reason}
          </p>
        )}
        {request.rejectionReason && (
          <p className="text-red-600 mt-2">
            <span className="font-semibold">Rejection Reason:</span> {request.rejectionReason}
          </p>
        )}
      </div>

      <div className="flex gap-2 mt-4">
        {onApprove && request.status === ABSENCE_STATUS.PENDING && (
          <button
            onClick={() => onApprove(request.id)}
            className="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-600"
          >
            Approve
          </button>
        )}

        {onReject && request.status === ABSENCE_STATUS.PENDING && (
          <button
            onClick={() => {
              const reason = prompt('Reason for rejection:');
              if (reason) onReject(request.id, reason);
            }}
            className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600"
          >
            Reject
          </button>
        )}

        {request.status === ABSENCE_STATUS.PENDING && onManagerUpdate && (
          <button
            className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
            onClick={() => {
              const status = prompt('New status (APPROVED/REJECTED/PENDING):', request.status);
              const managerComment = prompt('Manager comment:');
              if (status) {
                onManagerUpdate(request.id, status, managerComment === null ? undefined : managerComment);
              }
            }}
          >
            Manager Update
          </button>
        )}
      </div>

      <p className="text-xs text-gray-500 mt-4">
        {request.createdAt ? new Date(request.createdAt).toLocaleString() : ''}
      </p>
    </div>
  );
};

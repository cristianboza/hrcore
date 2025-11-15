import { Feedback, FEEDBACK_STATUS } from '../types/feedback';

interface FeedbackCardProps {
  feedback: Feedback;
  onPolish?: (id: number) => void;
  onApprove?: (id: number) => void;
  onReject?: (id: number) => void;
  isPolishing?: boolean;
}

export const FeedbackCard: React.FC<FeedbackCardProps> = ({
  feedback,
  onPolish,
  onApprove,
  onReject,
  isPolishing = false,
}) => {
  const statusColor = {
    [FEEDBACK_STATUS.PENDING]: 'bg-yellow-100 text-yellow-800',
    [FEEDBACK_STATUS.APPROVED]: 'bg-green-100 text-green-800',
    [FEEDBACK_STATUS.REJECTED]: 'bg-red-100 text-red-800',
  };

  return (
    <div className="bg-white rounded-lg shadow p-6 mb-4">
      <div className="flex justify-between items-start mb-4">
        <div>
          <p className="text-sm text-gray-600">From User ID: {feedback.fromUserId}</p>
          <p className="text-sm text-gray-600">To User ID: {feedback.toUserId}</p>
        </div>
        <span className={`px-3 py-1 rounded-full text-sm font-semibold ${statusColor[feedback.status]}`}>
          {feedback.status}
        </span>
      </div>

      <div className="mb-4">
        <h4 className="font-semibold text-gray-900 mb-2">Original Feedback</h4>
        <p className="text-gray-700 bg-gray-50 p-3 rounded">{feedback.content}</p>
      </div>

      {feedback.polishedContent && (
        <div className="mb-4">
          <h4 className="font-semibold text-gray-900 mb-2">Polished Feedback</h4>
          <p className="text-gray-700 bg-blue-50 p-3 rounded">{feedback.polishedContent}</p>
        </div>
      )}

      <div className="flex gap-2 mt-4">
        {onPolish && feedback.status === FEEDBACK_STATUS.PENDING && (
          <button
            onClick={() => onPolish(feedback.id)}
            disabled={isPolishing}
            className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 disabled:opacity-50"
          >
            {isPolishing ? 'Polishing...' : 'Polish with AI'}
          </button>
        )}

        {onApprove && feedback.status === FEEDBACK_STATUS.PENDING && (
          <button
            onClick={() => onApprove(feedback.id)}
            className="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-600"
          >
            Approve
          </button>
        )}

        {onReject && feedback.status === FEEDBACK_STATUS.PENDING && (
          <button
            onClick={() => onReject(feedback.id)}
            className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600"
          >
            Reject
          </button>
        )}
      </div>

      <p className="text-xs text-gray-500 mt-4">
        {feedback.createdAt ? new Date(feedback.createdAt).toLocaleString() : ''}
      </p>
    </div>
  );
};


import { useState } from 'react';
import { useSubmitFeedback } from '../hooks/useFeedback';
import { useAuthStore } from '../store/authStore';

interface FeedbackFormProps {
  toUserId: number;
  onSuccess?: () => void;
}

export const FeedbackForm: React.FC<FeedbackFormProps> = ({ toUserId, onSuccess }) => {
  const [content, setContent] = useState('');
  const currentUser = useAuthStore((state) => state.currentUser);
  const { mutate: submitFeedback, isPending } = useSubmitFeedback();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim() || !currentUser) return;

    submitFeedback(
      {
        fromUserId: currentUser.id,
        toUserId,
        content,
      },
      {
        onSuccess: () => {
          setContent('');
          onSuccess?.();
        },
      }
    );
  };

  return (
    <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 mb-6">
      <h3 className="text-lg font-semibold mb-4">Leave Feedback</h3>
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="Share your feedback..."
        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 mb-4"
        rows={4}
      />
      <button
        type="submit"
        disabled={isPending || !content.trim()}
        className="bg-blue-500 text-white px-6 py-2 rounded hover:bg-blue-600 disabled:opacity-50"
      >
        {isPending ? 'Submitting...' : 'Submit Feedback'}
      </button>
    </form>
  );
};


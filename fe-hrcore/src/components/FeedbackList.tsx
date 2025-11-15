import { useAuthStore } from '../store/authStore';
import { Feedback } from '../types/feedback';
import { useFeedback } from '../hooks/useFeedback';
import { FeedbackCard } from './FeedbackCard';

export const FeedbackList: React.FC = () => {
  const currentUser = useAuthStore((state) => state.currentUser);
  const { data: given, isLoading: loadingGiven } = useFeedback(currentUser?.id ?? 0, 'given');
  const { data: received, isLoading: loadingReceived } = useFeedback(currentUser?.id ?? 0, 'received');

  if (!currentUser) return null;

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">My Feedback</h2>
      <div className="mb-8">
        <h3 className="font-semibold mb-2">Feedback Given</h3>
        {loadingGiven ? (
          <div>Loading...</div>
        ) : given && given.length > 0 ? (
          given.map((fb: Feedback) => <FeedbackCard key={fb.id} feedback={fb} />)
        ) : (
          <div>No feedback given.</div>
        )}
      </div>
      <div>
        <h3 className="font-semibold mb-2">Feedback Received</h3>
        {loadingReceived ? (
          <div>Loading...</div>
        ) : received && received.length > 0 ? (
          received.map((fb: Feedback) => <FeedbackCard key={fb.id} feedback={fb} />)
        ) : (
          <div>No feedback received.</div>
        )}
      </div>
    </div>
  );
};

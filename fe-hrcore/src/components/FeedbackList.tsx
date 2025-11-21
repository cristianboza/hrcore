import { useAuthStore } from '../store/authStore';
import { Feedback } from '../types/feedback';
import { useFeedback } from '../hooks/useFeedback';
import { FeedbackCard } from './FeedbackCard';

export const FeedbackList: React.FC = () => {
  const currentUser = useAuthStore((state) => state.currentUser);
  const { data: givenPage, isLoading: loadingGiven } = useFeedback(currentUser?.id ?? '', 'given');
  const { data: receivedPage, isLoading: loadingReceived } = useFeedback(currentUser?.id ?? '', 'received');

  if (!currentUser) return null;

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">My Feedback</h2>
      <div className="mb-8">
        <h3 className="font-semibold mb-2">Feedback Given</h3>
        {loadingGiven ? (
          <div>Loading...</div>
        ) : givenPage && givenPage.content.length > 0 ? (
          givenPage.content.map((fb: Feedback) => <FeedbackCard key={fb.id} feedback={fb} />)
        ) : (
          <div>No feedback given.</div>
        )}
      </div>
      <div>
        <h3 className="font-semibold mb-2">Feedback Received</h3>
        {loadingReceived ? (
          <div>Loading...</div>
        ) : receivedPage && receivedPage.content.length > 0 ? (
          receivedPage.content.map((fb: Feedback) => <FeedbackCard key={fb.id} feedback={fb} />)
        ) : (
          <div>No feedback received.</div>
        )}
      </div>
    </div>
  );
};

import { useState } from 'react';
import { useSubmitFeedback } from '../hooks/useFeedback';
import { useAuthStore } from '../store/authStore';
import { Button } from './ui/button';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import { UserSearchSelect } from './ui/user-search-select';

interface FeedbackFormProps {
  toUserId?: string; // If provided, form is for this specific user (disabled input)
  onSuccess?: () => void;
}

export const FeedbackForm: React.FC<FeedbackFormProps> = ({ toUserId, onSuccess }) => {
  const [selectedToUserId, setSelectedToUserId] = useState(toUserId || '');
  const [content, setContent] = useState('');
  const currentUser = useAuthStore((state) => state.currentUser);
  const { mutate: submitFeedback, isPending } = useSubmitFeedback();

  const isFormForSpecificUser = !!toUserId;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const targetUserId = toUserId || selectedToUserId;
    if (!targetUserId || !content || !currentUser) return;

    submitFeedback(
      {
        fromUserId: currentUser.id,
        toUserId: targetUserId,
        content,
      },
      {
        onSuccess: () => {
          if (!toUserId) {
            setSelectedToUserId('');
          }
          setContent('');
          onSuccess?.();
        },
      }
    );
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {!isFormForSpecificUser && (
        <div className="space-y-2">
          <Label htmlFor="toUser">Recipient *</Label>
          <UserSearchSelect
            value={selectedToUserId}
            onValueChange={setSelectedToUserId}
            placeholder="Select a colleague to give feedback to..."
            excludeUserId={currentUser?.id}
          />
          <p className="text-xs text-muted-foreground">
            Choose who you want to give feedback to
          </p>
        </div>
      )}

      <div className="space-y-2">
        <Label htmlFor="content">Feedback</Label>
        <Textarea
          id="content"
          value={content}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setContent(e.target.value)}
          placeholder="Share your constructive feedback here...

Examples:
• Recognition: 'Great job on the presentation! Your clear communication helped the team understand the project goals.'
• Constructive: 'I noticed the report had some inconsistencies. Let's review the data together to improve accuracy.'
• Growth: 'You have excellent technical skills. Consider sharing your knowledge with the team through a workshop.'"
          rows={8}
          required
        />
        <p className="text-xs text-muted-foreground">
          Be specific, constructive, and respectful
        </p>
      </div>

      <div className="flex gap-2 justify-end">
        <Button
          type="submit"
          disabled={isPending || (!toUserId && !selectedToUserId) || !content.trim()}
          className="w-full sm:w-auto"
        >
          {isPending ? 'Submitting...' : 'Submit Feedback'}
        </Button>
      </div>
    </form>
  );
};

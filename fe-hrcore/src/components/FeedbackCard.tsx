import { useState } from 'react';
import { Feedback } from '../types/feedback';
import { useApproveFeedback, useRejectFeedback, usePolishFeedback } from '../hooks/useFeedback';
import { useFeature, FEATURE_FLAGS } from '../hooks/useFeatureFlags';
import { Card, CardContent, CardHeader } from './ui/card';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { MessageSquare, CheckCircle, XCircle, Sparkles, User, ArrowRight } from 'lucide-react';

interface FeedbackCardProps {
  feedback: Feedback;
  type?: 'received' | 'given' | 'pending';
  showActions?: boolean;
}

export const FeedbackCard: React.FC<FeedbackCardProps> = ({ feedback, showActions = false }) => {
  const [showPolished, setShowPolished] = useState(false);
  const { mutate: approve, isPending: isApproving } = useApproveFeedback();
  const { mutate: reject, isPending: isRejecting } = useRejectFeedback();
  const { mutate: polish, isPending: isPolishing } = usePolishFeedback();
  const isAiPolishEnabled = useFeature(FEATURE_FLAGS.FEEDBACK_AI_POLISH);

  const handleApprove = () => {
    approve(feedback.id);
  };

  const handleReject = () => {
    reject(feedback.id);
  };

  const handlePolish = () => {
    polish(feedback.id);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Card className="hover:shadow-md transition-shadow">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="space-y-2 flex-1">
            {/* From/To User Info - Always show both */}
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div className="flex items-start gap-2">
                <User className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                <div className="flex flex-col">
                  <span className="text-xs text-muted-foreground uppercase tracking-wide">From</span>
                  <span className="font-medium text-foreground">{feedback.fromUser.firstName} {feedback.fromUser.lastName}</span>
                  <span className="text-xs text-muted-foreground">{feedback.fromUser.email}</span>
                </div>
              </div>
              
              <div className="flex items-start gap-2">
                <ArrowRight className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                <div className="flex flex-col">
                  <span className="text-xs text-muted-foreground uppercase tracking-wide">To</span>
                  <span className="font-medium text-foreground">{feedback.toUser.firstName} {feedback.toUser.lastName}</span>
                  <span className="text-xs text-muted-foreground">{feedback.toUser.email}</span>
                </div>
              </div>
            </div>

            {/* Status Badge */}
            <div className="flex items-center gap-2">
              <Badge className={getStatusColor(feedback.status)} variant="outline">
                {feedback.status}
              </Badge>
              {feedback.polishedContent && (
                <Badge variant="outline" className="gap-1">
                  <Sparkles className="h-3 w-3" />
                  Polished
                </Badge>
              )}
            </div>
          </div>

          {/* Manager Actions */}
          {showActions && feedback.status === 'PENDING' && (
            <div className="flex gap-2">
              {isAiPolishEnabled && (
                <Button
                  size="sm"
                  variant="outline"
                  className="gap-1 text-purple-600 hover:text-purple-700 hover:bg-purple-50"
                  onClick={handlePolish}
                  disabled={isPolishing || !!feedback.polishedContent}
                  title="Polish with AI"
                >
                  <Sparkles className="h-4 w-4" />
                  Polish
                </Button>
              )}
              <Button
                size="sm"
                variant="outline"
                className="gap-1 text-green-600 hover:text-green-700 hover:bg-green-50"
                onClick={handleApprove}
                disabled={isApproving}
              >
                <CheckCircle className="h-4 w-4" />
                Approve
              </Button>
              <Button
                size="sm"
                variant="outline"
                className="gap-1 text-red-600 hover:text-red-700 hover:bg-red-50"
                onClick={handleReject}
                disabled={isRejecting}
              >
                <XCircle className="h-4 w-4" />
                Reject
              </Button>
            </div>
          )}
        </div>
      </CardHeader>

      <CardContent className="space-y-3">
        {/* Original Content */}
        <div>
          <div className="flex items-center gap-2 mb-2">
            <MessageSquare className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm font-medium">
              {feedback.polishedContent && showPolished ? 'Original Feedback' : 'Feedback'}
            </span>
          </div>
          <p className="text-sm text-foreground pl-6">{feedback.content}</p>
        </div>

        {/* Polished Content */}
        {feedback.polishedContent && (
          <div className="bg-purple-50 dark:bg-purple-950/20 p-3 rounded-md border border-purple-200">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-purple-600" />
                <span className="text-sm font-medium text-purple-900 dark:text-purple-100">
                  AI-Polished Version
                </span>
              </div>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setShowPolished(!showPolished)}
                className="h-6 text-xs"
              >
                {showPolished ? 'Hide' : 'Show'}
              </Button>
            </div>
            {showPolished && (
              <p className="text-sm text-purple-900 dark:text-purple-100 pl-6">
                {feedback.polishedContent}
              </p>
            )}
          </div>
        )}

        {/* Timestamp */}
        <div className="text-xs text-muted-foreground pt-2 border-t">
          {formatDate(feedback.createdAt)}
        </div>
      </CardContent>
    </Card>
  );
};

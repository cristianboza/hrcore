import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { feedbackService } from '@/services/feedbackService';
import { FeedbackCard } from './FeedbackCard';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Label } from './ui/label';
import { MessageSquare } from 'lucide-react';

interface ProfileFeedbackTabProps {
  userId: string;
  isOwnProfile: boolean;
  canManage: boolean;
}

export const ProfileFeedbackTab: React.FC<ProfileFeedbackTabProps> = ({ 
  userId, 
  isOwnProfile, 
  canManage 
}) => {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const pageSize = 10;

  const buildSearchRequest = () => {
    return {
      status: statusFilter === 'all' ? undefined : statusFilter as 'PENDING' | 'APPROVED' | 'REJECTED',
      page,
      size: pageSize,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    };
  };

  const { data: feedbackData, isLoading } = useQuery({
    queryKey: ['profile-feedback', userId, page, statusFilter],
    queryFn: () => feedbackService.getUserFeedback(userId, buildSearchRequest()),
  });

  const feedback = feedbackData?.content || [];
  const totalPages = feedbackData?.totalPages || 0;
  const totalElements = feedbackData?.totalElements || 0;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">
          Feedback {totalElements > 0 && `(${totalElements})`}
        </h3>
        
        {canManage && (
          <div className="w-48">
            <Label htmlFor="status-filter" className="sr-only">Status</Label>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger id="status-filter">
                <SelectValue placeholder="All Statuses" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Statuses</SelectItem>
                <SelectItem value="APPROVED">Approved</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
                <SelectItem value="REJECTED">Rejected</SelectItem>
              </SelectContent>
            </Select>
          </div>
        )}
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
      ) : feedback.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <MessageSquare className="h-12 w-12 text-muted-foreground mb-4" />
            <h4 className="text-lg font-semibold mb-2">No feedback found</h4>
            <p className="text-muted-foreground text-center">
              {isOwnProfile && !canManage
                ? "You haven't received any approved feedback yet"
                : canManage
                ? 'This employee has no feedback records'
                : 'You have not given feedback to this employee'}
            </p>
          </CardContent>
        </Card>
      ) : (
        <>
          <div className="space-y-3">
            {feedback.map((fb: any) => (
              <FeedbackCard
                key={fb.id}
                feedback={fb}
                type="received"
                showActions={canManage && fb.status === 'PENDING'}
              />
            ))}
          </div>

          {totalPages > 1 && (
            <div className="flex justify-between items-center">
              <span className="text-sm text-muted-foreground">
                Page {page + 1} of {totalPages}
              </span>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

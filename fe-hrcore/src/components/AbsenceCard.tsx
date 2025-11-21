import { useState } from 'react';
import { AbsenceRequest } from '../types/absence';
import { useApproveAbsenceRequest, useRejectAbsenceRequest } from '../hooks/useAbsence';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Calendar, CheckCircle, XCircle, AlertCircle, MessageSquare } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Textarea } from './ui/textarea';

interface AbsenceCardProps {
  request: AbsenceRequest;
  showActions?: boolean;
}

export const AbsenceCard: React.FC<AbsenceCardProps> = ({ request, showActions = false }) => {
  const [isRejectDialogOpen, setIsRejectDialogOpen] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const { mutate: approve, isPending: isApproving } = useApproveAbsenceRequest();
  const { mutate: reject, isPending: isRejecting } = useRejectAbsenceRequest();

  const handleApprove = () => {
    approve(
      { requestId: request.id },
      {
        onSuccess: () => {
          console.log('Request approved');
        },
      }
    );
  };

  const handleReject = () => {
    if (!rejectionReason.trim()) return;
    reject(
      { requestId: request.id, reason: rejectionReason },
      {
        onSuccess: () => {
          setIsRejectDialogOpen(false);
          setRejectionReason('');
        },
      }
    );
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

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'VACATION':
        return 'bg-blue-100 text-blue-800';
      case 'SICK':
        return 'bg-purple-100 text-purple-800';
      case 'OTHER':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const calculateDays = () => {
    const start = new Date(request.startDate);
    const end = new Date(request.endDate);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    return diffDays;
  };

  return (
    <>
      <Card className="hover:shadow-md transition-shadow">
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <Calendar className="h-4 w-4 text-muted-foreground" />
                <CardTitle className="text-base">
                  {formatDate(request.startDate)} - {formatDate(request.endDate)}
                </CardTitle>
                <Badge variant="outline" className="ml-2">
                  {calculateDays()} {calculateDays() === 1 ? 'day' : 'days'}
                </Badge>
              </div>
              <CardDescription className="flex items-center gap-2">
                <Badge className={getTypeColor(request.type)}>{request.type}</Badge>
                <Badge className={getStatusColor(request.status)} variant="outline">
                  {request.status}
                </Badge>
              </CardDescription>
            </div>

            {showActions && request.status === 'PENDING' && request.canApprove && (
              <div className="flex gap-2">
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
                  onClick={() => setIsRejectDialogOpen(true)}
                  disabled={isRejecting}
                >
                  <XCircle className="h-4 w-4" />
                  Reject
                </Button>
              </div>
            )}
          </div>
        </CardHeader>

        {request.reason && (
          <CardContent className="pt-0">
            <div className="flex gap-2 text-sm text-muted-foreground">
              <MessageSquare className="h-4 w-4 mt-0.5 flex-shrink-0" />
              <p>{request.reason}</p>
            </div>
          </CardContent>
        )}

        {request.rejectionReason && (
          <CardContent className="pt-0">
            <div className="flex gap-2 text-sm text-red-600 bg-red-50 p-3 rounded-md border border-red-200">
              <AlertCircle className="h-4 w-4 mt-0.5 flex-shrink-0" />
              <div>
                <p className="font-medium">Rejection Reason:</p>
                <p>{request.rejectionReason}</p>
              </div>
            </div>
          </CardContent>
        )}

        <CardContent className="pt-0 pb-3">
          <div className="flex flex-col gap-1 text-xs text-muted-foreground">
            {request.user && (
              <div>Employee: {request.user.firstName} {request.user.lastName}</div>
            )}
            {request.createdBy && (
              <div>Created by: {request.createdBy.firstName} {request.createdBy.lastName}</div>
            )}
            <div>Requested on {request.createdAt ? formatDate(request.createdAt) : 'Unknown'}</div>
          </div>
        </CardContent>
      </Card>

      {/* Rejection Dialog */}
      <Dialog open={isRejectDialogOpen} onOpenChange={setIsRejectDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Reject Absence Request</DialogTitle>
            <DialogDescription>
              Please provide a reason for rejecting this request.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <Textarea
              placeholder="Enter rejection reason..."
              value={rejectionReason}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setRejectionReason(e.target.value)}
              rows={4}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsRejectDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={handleReject}
              disabled={!rejectionReason.trim() || isRejecting}
            >
              {isRejecting ? 'Rejecting...' : 'Reject Request'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};

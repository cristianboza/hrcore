import { useState } from 'react';
import { useSearchAbsenceRequests } from '@/hooks/useAbsence';
import { AbsenceCard } from './AbsenceCard';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Label } from './ui/label';
import { Calendar } from 'lucide-react';

interface ProfileAbsenceTabProps {
  userId: string;
  isOwnProfile: boolean;
  canManage: boolean;
}

export const ProfileAbsenceTab: React.FC<ProfileAbsenceTabProps> = ({ 
  userId, 
  isOwnProfile, 
  canManage 
}) => {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const pageSize = 10;

  const buildSearchRequest = () => {
    const request: any = {
      userId,
      page,
      size: pageSize,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    };

    if (statusFilter && statusFilter !== 'all') {
      request.status = statusFilter;
    }

    if (typeFilter && typeFilter !== 'all') {
      request.type = typeFilter;
    }

    return request;
  };

  const { data: requestsData, isLoading } = useSearchAbsenceRequests(buildSearchRequest());

  const requests = requestsData?.content || [];
  const totalPages = requestsData?.totalPages || 0;
  const totalElements = requestsData?.totalElements || 0;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">
          Absence Requests {totalElements > 0 && `(${totalElements})`}
        </h3>
        
        <div className="flex gap-2">
          <div className="w-40">
            <Label htmlFor="status-filter" className="sr-only">Status</Label>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger id="status-filter">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Statuses</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
                <SelectItem value="APPROVED">Approved</SelectItem>
                <SelectItem value="REJECTED">Rejected</SelectItem>
              </SelectContent>
            </Select>
          </div>
          
          <div className="w-40">
            <Label htmlFor="type-filter" className="sr-only">Type</Label>
            <Select value={typeFilter} onValueChange={setTypeFilter}>
              <SelectTrigger id="type-filter">
                <SelectValue placeholder="Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Types</SelectItem>
                <SelectItem value="VACATION">Vacation</SelectItem>
                <SelectItem value="SICK_LEAVE">Sick Leave</SelectItem>
                <SelectItem value="PERSONAL">Personal</SelectItem>
                <SelectItem value="OTHER">Other</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
      ) : requests.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Calendar className="h-12 w-12 text-muted-foreground mb-4" />
            <h4 className="text-lg font-semibold mb-2">No absence requests found</h4>
            <p className="text-muted-foreground text-center">
              {isOwnProfile
                ? "You haven't submitted any absence requests"
                : 'This employee has no absence requests'}
            </p>
          </CardContent>
        </Card>
      ) : (
        <>
          <div className="space-y-3">
            {requests.map((request: any) => (
              <AbsenceCard
                key={request.id}
                request={request}
                showActions={canManage && request.status === 'PENDING'}
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

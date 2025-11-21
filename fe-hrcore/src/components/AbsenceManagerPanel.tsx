import { useState } from 'react';
import { useSearchAbsenceRequests } from '../hooks/useAbsence';
import { useAuthStore } from '../store/authStore';
import { AbsenceCard } from './AbsenceCard';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Filter } from 'lucide-react';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import {UserSearchSelect} from "@/components/ui/user-search-select.tsx";

export const AbsenceManagerPanel: React.FC = () => {
  const currentUser = useAuthStore((state) => state.currentUser);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string | undefined>('PENDING');
  const [userFilter, setUserFilter] = useState<string | undefined>(undefined);

  const { data: requestsPage, isLoading, error } = useSearchAbsenceRequests({
    status: statusFilter as any,
    userId: userFilter,
    page,
    size: 10,
    sortBy: 'createdAt',
    sortDirection: 'DESC',
  });

  if (!currentUser || (currentUser.role !== 'MANAGER' && currentUser.role !== 'SUPER_ADMIN')) {
    return null;
  }

  if (isLoading) {
    return <div className="p-4">Loading absence requests...</div>;
  }

  if (error) {
    return <div className="p-4 text-red-600">Error loading absence requests</div>;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Absence Request Management</CardTitle>
        <CardDescription>
          {currentUser.role === 'SUPER_ADMIN'
            ? 'View and manage all absence requests. You can approve/reject any request.'
            : 'View all absence requests. You can only approve/reject requests from your direct reports.'}
        </CardDescription>
      </CardHeader>
      <CardContent>
        {/* Filters */}
        <div className="flex gap-4 mb-6">
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-muted-foreground" />
            <Select value={statusFilter || 'ALL'} onValueChange={(val) => setStatusFilter(val === 'ALL' ? undefined : val)}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Filter by status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Statuses</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
                <SelectItem value="APPROVED">Approved</SelectItem>
                <SelectItem value="REJECTED">Rejected</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="flex-1">
            <UserSearchSelect
              value={userFilter}
              onValueChange={setUserFilter}
              placeholder="Filter by user..."
              allowClear
            />
          </div>
        </div>

        {/* Absence Requests List */}
        <div className="space-y-4">
          {requestsPage && requestsPage.content.length > 0 ? (
            requestsPage.content.map((req) => (
              <AbsenceCard
                key={req.id}
                request={req}
                showActions={true}
              />
            ))
          ) : (
            <div className="text-center py-8 text-muted-foreground">
              No absence requests found.
            </div>
          )}
        </div>

        {/* Pagination */}
        {requestsPage && requestsPage.totalPages > 1 && (
          <div className="flex items-center justify-between mt-6">
            <Button
              variant="outline"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              Previous
            </Button>
            <span className="text-sm text-muted-foreground">
              Page {page + 1} of {requestsPage.totalPages}
            </span>
            <Button
              variant="outline"
              disabled={page >= requestsPage.totalPages - 1}
              onClick={() => setPage(page + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
};


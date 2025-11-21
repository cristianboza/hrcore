import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { useSearchFeedback } from '../hooks/useFeedback';
import { FeedbackCard } from './FeedbackCard';
import { FeedbackForm } from './FeedbackForm';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { UserSearchSelect } from './ui/user-search-select';
import { Search, X, Plus, MessageSquare, AlertCircle } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from './ui/dialog';

const useDebounce = (value: string, delay: number) => {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);

  return debouncedValue;
};

export const FeedbackPage: React.FC = () => {
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [fromUserFilter, setFromUserFilter] = useState('');
  const [toUserFilter, setToUserFilter] = useState('');
  const [sortBy, setSortBy] = useState('createdAt');
  const [page, setPage] = useState(0);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const pageSize = 10;

  const isManager = currentUser?.role === 'MANAGER' || currentUser?.role === 'SUPER_ADMIN';

  // Redirect if not manager/admin
  useEffect(() => {
    if (currentUser && !isManager) {
      navigate('/');
    }
  }, [currentUser, isManager, navigate]);

  // Debounce search term (500ms delay)
  const debouncedSearchTerm = useDebounce(searchTerm, 500);

  // Reset page when filters change
  useEffect(() => {
    setPage(0);
  }, [debouncedSearchTerm, statusFilter, fromUserFilter, toUserFilter, sortBy]);

  // Build search request for managers
  const buildSearchRequest = () => {
    const request: any = {
      page,
      size: pageSize,
      sortBy,
      sortDirection: 'DESC',
    };

    if (statusFilter && statusFilter !== 'all') {
      request.status = statusFilter;
    }

    if (debouncedSearchTerm) {
      request.contentContains = debouncedSearchTerm;
    }

    if (fromUserFilter) {
      request.fromUserId = fromUserFilter;
    }

    if (toUserFilter) {
      request.toUserId = toUserFilter;
    }

    return request;
  };

  const { data: feedbackData, isLoading } = useSearchFeedback(buildSearchRequest());

  if (!currentUser || !isManager) {
    return (
      <Card className="max-w-2xl mx-auto mt-8">
        <CardContent className="flex flex-col items-center justify-center py-12">
          <AlertCircle className="h-12 w-12 text-destructive mb-4" />
          <h3 className="text-lg font-semibold mb-2">Access Denied</h3>
          <p className="text-muted-foreground text-center">
            This page is only accessible to managers and administrators.
          </p>
        </CardContent>
      </Card>
    );
  }

  const feedback = feedbackData?.content || [];
  const totalPages = feedbackData?.totalPages || 0;
  const totalElements = feedbackData?.totalElements || 0;

  const handleClearFilters = () => {
    setSearchTerm('');
    setStatusFilter('all');
    setFromUserFilter('');
    setToUserFilter('');
    setSortBy('createdAt');
    setPage(0);
  };

  return (
    <div className="container max-w-7xl mx-auto p-6 space-y-6">
      {/* Header with Quick Action */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-4xl font-bold tracking-tight">Feedback Management</h1>
          <p className="text-muted-foreground mt-2">
            Manage and review all team feedback submissions
          </p>
        </div>

        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button size="lg" className="gap-2">
              <Plus className="h-5 w-5" />
              Give Feedback
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>Give Feedback</DialogTitle>
              <DialogDescription>
                Share constructive feedback with a colleague
              </DialogDescription>
            </DialogHeader>
            <FeedbackForm onSuccess={() => setIsDialogOpen(false)} />
          </DialogContent>
        </Dialog>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="p-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div>
              <Label htmlFor="search">Search Content</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  id="search"
                  type="text"
                  placeholder="Search feedback content..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-9"
                />
                {searchTerm && (
                  <button
                    onClick={() => setSearchTerm('')}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>
            </div>

            <div>
              <Label htmlFor="status">Status</Label>
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger id="status">
                  <SelectValue placeholder="All Statuses" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Statuses</SelectItem>
                  <SelectItem value="PENDING">Pending</SelectItem>
                  <SelectItem value="APPROVED">Approved</SelectItem>
                  <SelectItem value="REJECTED">Rejected</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="sortBy">Sort By</Label>
              <Select value={sortBy} onValueChange={setSortBy}>
                <SelectTrigger id="sortBy">
                  <SelectValue placeholder="Sort by" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="createdAt">Created Date</SelectItem>
                  <SelectItem value="updatedAt">Updated Date</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="fromUser">From User</Label>
              <UserSearchSelect
                value={fromUserFilter}
                onValueChange={setFromUserFilter}
                placeholder="Filter by submitter..."
                allowClear
              />
            </div>

            <div>
              <Label htmlFor="toUser">To User</Label>
              <UserSearchSelect
                value={toUserFilter}
                onValueChange={setToUserFilter}
                placeholder="Filter by recipient..."
                allowClear
              />
            </div>
          </div>

          {(searchTerm || statusFilter !== 'all' || fromUserFilter || toUserFilter || sortBy !== 'createdAt') && (
            <div className="mt-3">
              <Button variant="ghost" size="sm" onClick={handleClearFilters}>
                <X className="h-4 w-4 mr-1.5" />
                Clear Filters
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Results */}
      <div className="space-y-4">
        {/* Results Count */}
        <div className="flex justify-between items-center text-sm text-muted-foreground">
          <span>
            {totalElements > 0 ? (
              <>
                Showing {page * pageSize + 1}-{Math.min((page + 1) * pageSize, totalElements)} of {totalElements} feedback
              </>
            ) : (
              'No feedback found'
            )}
          </span>
          {totalPages > 1 && (
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </Button>
              <span className="flex items-center px-3">
                Page {page + 1} of {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
              >
                Next
              </Button>
            </div>
          )}
        </div>

        {/* Loading State */}
        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          </div>
        ) : feedback.length === 0 ? (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <MessageSquare className="h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold mb-2">No feedback found</h3>
              <p className="text-muted-foreground text-center mb-4">
                No feedback matches your current filters
              </p>
              <Button onClick={() => setIsDialogOpen(true)} className="gap-2">
                <Plus className="h-4 w-4" />
                Give Feedback
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-3">
            {feedback.map((fb: any) => (
              <FeedbackCard 
                key={fb.id} 
                feedback={fb} 
                type={fb.status === 'PENDING' ? 'pending' : 'received'}
                showActions={fb.status === 'PENDING'}
              />
            ))}
          </div>
        )}

        {/* Bottom Pagination */}
        {totalPages > 1 && feedback.length > 0 && (
          <div className="flex justify-end">
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </Button>
              <span className="flex items-center px-3 text-sm">
                Page {page + 1} of {totalPages}
              </span>
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
      </div>
    </div>
  );
};

import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useProfiles, useDeleteProfile } from '@/hooks/useProfile';
import { ProfileCard } from './ProfileCard';
import { useAuthStore } from '@/store/authStore';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Search, X, UserPlus } from 'lucide-react';

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

export const ProfileList: React.FC = () => {
  const { t } = useTranslation();
  const currentUser = useAuthStore((state) => state.currentUser);
  
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState<{ id: string; name: string } | null>(null);

  // Debounce search term (500ms delay)
  const debouncedSearchTerm = useDebounce(searchTerm, 500);

  // Reset page when filters change
  useEffect(() => {
    setPage(0);
  }, [debouncedSearchTerm, roleFilter, departmentFilter]);
  
  const { data: profilesData, isLoading, error, refetch } = useProfiles({
    search: debouncedSearchTerm || undefined,
    role: (roleFilter && roleFilter !== 'all') ? roleFilter : undefined,
    department: departmentFilter || undefined,
    page,
    size: pageSize,
  });
  
  const { mutate: deleteProfile } = useDeleteProfile();

  const handleDeleteClick = (userId: string, userName: string) => {
    setUserToDelete({ id: userId, name: userName });
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = () => {
    if (!userToDelete) return;
    
    deleteProfile(userToDelete.id, {
      onSuccess: () => {
        refetch();
        setUserToDelete(null);
      },
      onError: (error: any) => {
        alert('Error deleting profile: ' + (error.message || 'Unknown error'));
      },
    });
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setRoleFilter('all');
    setDepartmentFilter('');
    setPage(0);
  };

  const profiles = profilesData?.content?.filter(p => p.id !== currentUser?.id) || [];
  const totalPages = profilesData?.totalPages || 0;
  const totalElements = profilesData?.totalElements || 0;

  if (isLoading) {
    return (
      <div className="flex justify-center items-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        <span className="ml-2">{t('common.loading')}</span>
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertDescription>
          Error loading profiles: {error instanceof Error ? error.message : 'Unknown error'}
        </AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold">{t('profile.employeeProfiles')}</h2>
          {(currentUser?.role === 'SUPER_ADMIN' || currentUser?.role === 'MANAGER') && (
            <Button onClick={() => window.location.href = '/profiles/create'}>
              <UserPlus className="h-4 w-4 mr-2" />
              {t('profile.createUser')}
            </Button>
          )}
        </div>
        
        <Card className="mb-4">
          <CardContent className="p-4">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <Label htmlFor="search">{t('common.search')}</Label>
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="search"
                    type="text"
                    placeholder="Search by name or email..."
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
                <Label htmlFor="role">{t('profile.role')}</Label>
                <Select value={roleFilter} onValueChange={setRoleFilter}>
                  <SelectTrigger>
                    <SelectValue placeholder="All Roles" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">{t('profile.allRoles')}</SelectItem>
                    <SelectItem value="EMPLOYEE">{t('roles.EMPLOYEE')}</SelectItem>
                    <SelectItem value="MANAGER">{t('roles.MANAGER')}</SelectItem>
                    {currentUser?.role === 'SUPER_ADMIN' && (
                      <SelectItem value="SUPER_ADMIN">{t('roles.SUPER_ADMIN')}</SelectItem>
                    )}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label htmlFor="department">{t('profile.department')}</Label>
                <Input
                  id="department"
                  type="text"
                  placeholder="Filter by department..."
                  value={departmentFilter}
                  onChange={(e) => setDepartmentFilter(e.target.value)}
                />
              </div>
            </div>

            {(searchTerm || (roleFilter && roleFilter !== 'all') || departmentFilter) && (
              <div className="mt-3">
                <Button variant="ghost" size="sm" onClick={handleClearFilters}>
                  <X className="h-4 w-4 mr-1.5" />
                  {t('common.clearFilters')}
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        <div className="flex justify-between items-center text-sm text-muted-foreground mb-2">
          <span>
            {totalElements > 0 && (
              <>
                Showing {page * pageSize + 1}-{Math.min((page + 1) * pageSize, totalElements)} of {totalElements} profile{totalElements !== 1 ? 's' : ''}
              </>
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
      </div>

      {!profiles || profiles.length === 0 ? (
        <Alert>
          <AlertDescription>{t('profile.noProfilesFound')}</AlertDescription>
        </Alert>
      ) : (
        <div className="space-y-4">
          {profiles.map((profile) => {
            // Only allow editing/deleting if:
            // - You're the user yourself (can edit own profile)
            // - You're a SUPER_ADMIN (can do anything)
            // - You're a MANAGER and the user is managed by you (managerId matches)
            const isOwnProfile = currentUser?.id === profile.id;
            const isSuperAdmin = currentUser?.role === 'SUPER_ADMIN';
            const isManagerOfUser = currentUser?.role === 'MANAGER' && profile.managerId === currentUser?.id;
            
            return (
              <ProfileCard
                key={profile.id}
                user={profile}
                canEdit={isOwnProfile || isSuperAdmin || isManagerOfUser}
                canDelete={isSuperAdmin || isManagerOfUser}
                onDelete={() => handleDeleteClick(profile.id, `${profile.firstName} ${profile.lastName}`)}
              />
            );
          })}
        </div>
      )}

      <ConfirmDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        onConfirm={handleDeleteConfirm}
        title="Delete Profile"
        description={`Are you sure you want to delete ${userToDelete?.name}? This action cannot be undone.`}
        confirmText="Delete"
        cancelText="Cancel"
        variant="destructive"
      />
    </div>
  );
};

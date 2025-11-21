import { useParams, useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { 
  useProfile, 
  useProfilePermissions, 
  useDeleteProfile,
  useDirectReports,
  useManager
} from '@/hooks/useProfile';
import { useAuthStore } from '@/store/authStore';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Separator } from '@/components/ui/separator';
import { Label } from '@/components/ui/label';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ProfileFeedbackTab } from './ProfileFeedbackTab';
import { ProfileAbsenceTab } from './ProfileAbsenceTab';
import { FeedbackForm } from './FeedbackForm';
import { AbsenceForm } from './AbsenceForm';

export const ProfileDetail: React.FC = () => {
  const { t } = useTranslation();
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);
  const { mutate: deleteProfile } = useDeleteProfile();

  const [showFeedbackForm, setShowFeedbackForm] = useState(false);
  const [showAbsenceForm, setShowAbsenceForm] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  const { data: profile, isLoading, error } = useProfile(userId);
  const { data: permissions } = useProfilePermissions(userId);
  const { data: directReports } = useDirectReports(userId);
  const { data: manager } = useManager(userId);

  const isOwnProfile = currentUser?.id === userId;
  
  // Check if current user is direct manager
  const isDirectManager = manager?.id === currentUser?.id || profile?.manager?.id === currentUser?.id;
  
  // Tabs are visible to: own user, direct manager, or admin
  const canViewTabs = isOwnProfile || isDirectManager || currentUser?.role === 'SUPER_ADMIN';

  if (isLoading) {
    return (
      <div className="flex justify-center items-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        <span className="ml-2">{t('common.loading')}</span>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <Alert variant="destructive">
        <AlertDescription>
          {t('profile.loadError')}: {error instanceof Error ? error.message : 'Profile not found'}
        </AlertDescription>
      </Alert>
    );
  }

  const handleDelete = () => {
    if (!profile) return;
    deleteProfile(profile.id, {
      onSuccess: () => navigate('/profiles'),
      onError: (error: any) => {
        alert('Error deleting profile: ' + (error.message || 'Unknown error'));
      },
    });
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <Button variant="ghost" asChild className="mb-4">
        <Link to="/profiles">← {t('common.back')}</Link>
      </Button>

      <Card>
        <CardHeader>
          <div className="flex justify-between items-start">
            <div>
              <CardTitle className="text-3xl">
                {profile.firstName} {profile.lastName}
              </CardTitle>
              <CardDescription className="text-lg">{profile.email}</CardDescription>
            </div>
            <Badge variant="secondary" className="text-sm">
              {t(`roles.${profile.role}`)}
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid grid-cols-2 gap-6">
            <div>
              <Label className="text-sm font-semibold">{t('profile.phoneNumber')}</Label>
              <p className="text-foreground mt-1">{profile.phone || 'Not provided'}</p>
            </div>

            <div>
              <Label className="text-sm font-semibold">{t('profile.department')}</Label>
              <p className="text-foreground mt-1">{profile.department || 'Not provided'}</p>
            </div>

            <div>
              <Label className="text-sm font-semibold">Member Since</Label>
              <p className="text-foreground mt-1">
                {profile.createdAt ? new Date(profile.createdAt).toLocaleDateString() : 'N/A'}
              </p>
            </div>

            <div>
              <Label className="text-sm font-semibold">Last Updated</Label>
              <p className="text-foreground mt-1">
                {profile.updatedAt ? new Date(profile.updatedAt).toLocaleDateString() : 'N/A'}
              </p>
            </div>
          </div>

          {(manager || profile.manager) && (
            <>
              <Separator />
              <div>
                <h3 className="text-lg font-semibold mb-3">{t('profile.reportsTo')}</h3>
                <Card>
                  <CardContent className="p-4 flex items-center gap-3">
                    <div className="bg-primary/10 rounded-full w-12 h-12 flex items-center justify-center text-primary font-bold text-lg">
                      {(manager?.firstName || profile.manager?.firstName || 'U')[0]}
                      {(manager?.lastName || profile.manager?.lastName || 'U')[0]}
                    </div>
                    <div className="flex-1">
                      <p className="font-semibold">
                        {manager?.firstName || profile.manager?.firstName} {manager?.lastName || profile.manager?.lastName}
                      </p>
                      <p className="text-sm text-muted-foreground">{manager?.email || profile.manager?.email}</p>
                    </div>
                    <Button variant="outline" size="sm" asChild>
                      <Link to={`/profiles/${manager?.id || profile.manager?.id}`}>
                        View Profile
                      </Link>
                    </Button>
                  </CardContent>
                </Card>
              </div>
            </>
          )}

          {directReports && directReports.length > 0 && (
            <>
              <Separator />
              <div>
                <h3 className="text-lg font-semibold mb-3">{t('profile.directReports')}</h3>
                <div className="space-y-2">
                  {directReports.map((report) => (
                    <Card key={report.id}>
                      <CardContent className="p-4 flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <div className="bg-secondary rounded-full w-10 h-10 flex items-center justify-center font-bold">
                            {report.firstName[0]}{report.lastName[0]}
                          </div>
                          <div>
                            <p className="font-semibold">{report.firstName} {report.lastName}</p>
                            <p className="text-sm text-muted-foreground">{report.email}</p>
                          </div>
                        </div>
                        <Button variant="outline" size="sm" asChild>
                          <Link to={`/profiles/${report.id}`}>View</Link>
                        </Button>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </div>
            </>
          )}

          <Separator />

          <div className="space-y-3">
            <div className="flex flex-wrap gap-2">
              {permissions?.canEdit && (
                <Button asChild>
                  <Link to={`/profiles/${profile.id}/edit`}>{t('common.edit')}</Link>
                </Button>
              )}
              {permissions?.canDelete && (
                <Button variant="destructive" onClick={() => setDeleteDialogOpen(true)}>
                  {t('common.delete')}
                </Button>
              )}
            </div>

            {(permissions?.canGiveFeedback || permissions?.canRequestAbsence) && (
              <>
                <Separator className="my-3" />
                <div className="grid grid-cols-2 gap-3">
                  {permissions?.canGiveFeedback && (
                    <Button 
                      variant={showFeedbackForm ? "secondary" : "outline"} 
                      onClick={() => {
                        setShowFeedbackForm(!showFeedbackForm);
                        setShowAbsenceForm(false);
                      }}
                      className="w-full"
                    >
                      <svg 
                        className="mr-2 h-4 w-4" 
                        fill="none" 
                        stroke="currentColor" 
                        viewBox="0 0 24 24"
                      >
                        <path 
                          strokeLinecap="round" 
                          strokeLinejoin="round" 
                          strokeWidth={2} 
                          d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" 
                        />
                      </svg>
                      {showFeedbackForm ? 'Hide Feedback' : 'Give Feedback'}
                    </Button>
                  )}
                  {permissions?.canRequestAbsence && (
                    <Button 
                      variant={showAbsenceForm ? "secondary" : "outline"}
                      onClick={() => {
                        setShowAbsenceForm(!showAbsenceForm);
                        setShowFeedbackForm(false);
                      }}
                      className="w-full"
                    >
                      <svg 
                        className="mr-2 h-4 w-4" 
                        fill="none" 
                        stroke="currentColor" 
                        viewBox="0 0 24 24"
                      >
                        <path 
                          strokeLinecap="round" 
                          strokeLinejoin="round" 
                          strokeWidth={2} 
                          d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" 
                        />
                      </svg>
                      {showAbsenceForm ? 'Hide Request' : 'Request Absence'}
                    </Button>
                  )}
                </div>
              </>
            )}
          </div>

          {showFeedbackForm && (
            <Card className="bg-muted/30 border-2 border-primary/20 mt-4">
              <CardHeader className="pb-4">
                <CardTitle className="text-lg flex items-center gap-2">
                  <svg 
                    className="h-5 w-5 text-primary" 
                    fill="none" 
                    stroke="currentColor" 
                    viewBox="0 0 24 24"
                  >
                    <path 
                      strokeLinecap="round" 
                      strokeLinejoin="round" 
                      strokeWidth={2} 
                      d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" 
                    />
                  </svg>
                  Give Feedback to {profile.firstName} {profile.lastName}
                </CardTitle>
                <CardDescription>
                  {isOwnProfile 
                    ? 'Self-reflection and feedback will be reviewed by your manager' 
                    : 'Feedback will be reviewed by their manager before being visible to the recipient'}
                </CardDescription>
              </CardHeader>
              <CardContent>
                <FeedbackForm 
                  toUserId={profile.id}
                  onSuccess={() => {
                    setShowFeedbackForm(false);
                    alert('Feedback submitted successfully!');
                  }} 
                />
              </CardContent>
            </Card>
          )}

          {showAbsenceForm && (
            <Card className="bg-muted/30 border-2 border-primary/20 mt-4">
              <CardHeader className="pb-4">
                <CardTitle className="text-lg flex items-center gap-2">
                  <svg 
                    className="h-5 w-5 text-primary" 
                    fill="none" 
                    stroke="currentColor" 
                    viewBox="0 0 24 24"
                  >
                    <path 
                      strokeLinecap="round" 
                      strokeLinejoin="round" 
                      strokeWidth={2} 
                      d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" 
                    />
                  </svg>
                  {isOwnProfile ? 'Request Absence' : `Request Absence for ${profile.firstName} ${profile.lastName}`}
                </CardTitle>
                <CardDescription>
                  {isOwnProfile 
                    ? 'Your absence request will be reviewed and approved by your manager' 
                    : 'This absence request will be reviewed and approved by their manager'}
                </CardDescription>
              </CardHeader>
              <CardContent>
                <AbsenceForm 
                  userId={profile.id}
                  onSuccess={() => {
                    setShowAbsenceForm(false);
                    alert('Absence request submitted successfully!');
                  }} 
                />
              </CardContent>
            </Card>
          )}
        </CardContent>
      </Card>

      {canViewTabs && (
        <Card className="mt-6">
          <CardContent className="pt-6">
            <Tabs defaultValue="details" className="w-full">
              <TabsList className="grid w-full grid-cols-3">
                <TabsTrigger value="details">Details</TabsTrigger>
                <TabsTrigger value="feedback">Feedback</TabsTrigger>
                <TabsTrigger value="absence">Absence</TabsTrigger>
              </TabsList>
              
              <TabsContent value="details" className="mt-6">
                <div className="text-muted-foreground text-center py-8">
                  <p>Profile details are shown above.</p>
                </div>
              </TabsContent>
              
              <TabsContent value="feedback" className="mt-6">
                <ProfileFeedbackTab 
                  userId={userId!} 
                  isOwnProfile={isOwnProfile} 
                  canManage={isDirectManager || currentUser?.role === 'SUPER_ADMIN'}
                />
              </TabsContent>
              
              <TabsContent value="absence" className="mt-6">
                <ProfileAbsenceTab 
                  userId={userId!} 
                  isOwnProfile={isOwnProfile} 
                  canManage={isDirectManager || currentUser?.role === 'SUPER_ADMIN'}
                />
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      )}

      <ConfirmDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        onConfirm={handleDelete}
        title="Delete Profile"
        description={`Are you sure you want to delete ${profile.firstName} ${profile.lastName}? This action cannot be undone.`}
        confirmText="Delete"
        cancelText="Cancel"
        variant="destructive"
      />
    </div>
  );
};

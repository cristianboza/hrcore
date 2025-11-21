import { useParams, useNavigate, Link } from 'react-router-dom';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import { 
  useProfile, 
  useUpdateProfile, 
  useAvailableManagers,
  useAssignManager,
  useRemoveManager
} from '@/hooks/useProfile';
import { useAuthStore } from '@/store/authStore';
import { profileEditSchema, ProfileEditFormData } from '@/schemas/profileSchema';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';

export const ProfileEdit: React.FC = () => {
  const { t } = useTranslation();
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);
  const { mutate: updateProfile, isPending } = useUpdateProfile();
  const { mutate: assignManager } = useAssignManager();
  const { mutate: removeManager } = useRemoveManager();

  const { data: profile, isLoading, error } = useProfile(userId);
  const { data: availableManagers } = useAvailableManagers();

  const form = useForm<ProfileEditFormData>({
    resolver: zodResolver(profileEditSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      department: '',
      role: 'EMPLOYEE',
      managerId: null,
    },
  });

  useEffect(() => {
    if (profile) {
      form.reset({
        firstName: profile.firstName,
        lastName: profile.lastName,
        email: profile.email,
        phone: profile.phone || '',
        department: profile.department || '',
        role: profile.role as 'EMPLOYEE' | 'MANAGER' | 'SUPER_ADMIN',
        managerId: profile.managerId || null,
      });
    }
  }, [profile, form]);

  const onSubmit = (data: ProfileEditFormData) => {
    if (!userId) return;

    const { managerId, email, ...profileData } = data;

    updateProfile(
      {
        userId: userId,
        updateData: profileData,
      },
      {
        onSuccess: () => {
          const currentManagerId = profile?.managerId || null;
          
          if (managerId !== currentManagerId) {
            if (managerId === null) {
              removeManager(userId, {
                onSuccess: () => navigate(`/profiles/${userId}`),
                onError: (error) => console.error('Failed to update manager:', error),
              });
            } else if (managerId !== undefined) {
              assignManager(
                { userId: userId, managerId: managerId },
                {
                  onSuccess: () => navigate(`/profiles/${userId}`),
                  onError: (error) => console.error('Failed to assign manager:', error),
                }
              );
            }
          } else {
            navigate(`/profiles/${userId}`);
          }
        },
        onError: (error) => {
          console.error('Failed to update profile:', error);
        },
      }
    );
  };

  if (!currentUser || !userId || (currentUser.id !== userId && currentUser.role !== 'MANAGER' && currentUser.role !== 'SUPER_ADMIN')) {
    return (
      <Alert variant="destructive">
        <AlertDescription>
          {t('common.error')}: You do not have permission to edit this profile.
        </AlertDescription>
      </Alert>
    );
  }

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

  const isManagerOrAbove = currentUser.role === 'MANAGER' || currentUser.role === 'SUPER_ADMIN';

  return (
    <div className="max-w-2xl mx-auto p-6">
      <Button variant="ghost" asChild className="mb-4">
        <Link to={`/profiles/${userId}`}>← {t('common.back')}</Link>
      </Button>

      <Card>
        <CardHeader>
          <CardTitle className="text-3xl">{t('profile.editProfile')}</CardTitle>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <div className="grid grid-cols-2 gap-6">
                <FormField
                  control={form.control}
                  name="firstName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('profile.firstName')} *</FormLabel>
                      <FormControl>
                        <Input {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="lastName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('profile.lastName')} *</FormLabel>
                      <FormControl>
                        <Input {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('profile.email')} *</FormLabel>
                    <FormControl>
                      <Input {...field} disabled />
                    </FormControl>
                    <FormDescription>Email cannot be changed</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="grid grid-cols-2 gap-6">
                <FormField
                  control={form.control}
                  name="phone"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('profile.phoneNumber')}</FormLabel>
                      <FormControl>
                        <Input {...field} placeholder="+1-555-1234" />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="department"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('profile.department')}</FormLabel>
                      <FormControl>
                        <Input {...field} placeholder="Engineering" />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {isManagerOrAbove && (
                <FormField
                  control={form.control}
                  name="role"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('profile.role')}</FormLabel>
                      <Select onValueChange={field.onChange} defaultValue={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="EMPLOYEE">{t('roles.EMPLOYEE')}</SelectItem>
                          <SelectItem value="MANAGER">{t('roles.MANAGER')}</SelectItem>
                          {currentUser.role === 'SUPER_ADMIN' && (
                            <SelectItem value="SUPER_ADMIN">{t('roles.SUPER_ADMIN')}</SelectItem>
                          )}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}

              {isManagerOrAbove && (form.watch('role') === 'MANAGER' || form.watch('role') === 'EMPLOYEE') && (
                <FormField
                  control={form.control}
                  name="managerId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('profile.manager')}</FormLabel>
                      <Select 
                        onValueChange={(value) => field.onChange(value === 'none' ? null : value)}
                        value={field.value || 'none'}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder={t('profile.noManager')} />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="none">{t('profile.noManager')}</SelectItem>
                          {availableManagers?.map((manager) => (
                            <SelectItem key={manager.id} value={manager.id}>
                              {manager.firstName} {manager.lastName} ({manager.email})
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormDescription>
                        {form.watch('role') === 'MANAGER' 
                          ? 'Assign this manager to report to another manager (optional)' 
                          : 'Assign a manager for this employee'}
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}

              <div className="flex gap-4 pt-6 border-t">
                <Button type="submit" disabled={form.formState.isSubmitting || isPending}>
                  {form.formState.isSubmitting || isPending ? t('common.loading') : t('common.save')}
                </Button>
                <Button type="button" variant="outline" asChild>
                  <Link to={`/profiles/${userId}`}>{t('common.cancel')}</Link>
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );
};

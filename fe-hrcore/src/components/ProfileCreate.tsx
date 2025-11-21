import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useCreateProfile } from '@/hooks/useProfile';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { ArrowLeft, UserPlus, Eye, EyeOff } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';

export const ProfileCreate: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);
  const { mutate: createProfile, isPending, error } = useCreateProfile();

  const [formData, setFormData] = useState({
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    phone: '',
    department: '',
    role: 'EMPLOYEE',
    managerId: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [validationError, setValidationError] = useState('');
  const [backendErrors, setBackendErrors] = useState<Record<string, string>>({});

  const handleChange = (field: string, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    
    // Only clear the general validation error
    setValidationError('');
    
    // Clear field-specific backend error for this field only
    if (backendErrors[field]) {
      setBackendErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const validateForm = () => {
    if (!formData.email || !formData.email.includes('@')) {
      setValidationError('Valid email is required');
      return false;
    }
    if (!formData.firstName || !formData.lastName) {
      setValidationError('First name and last name are required');
      return false;
    }
    if (!formData.password || formData.password.length < 8) {
      setValidationError('Password must be at least 8 characters');
      return false;
    }
    return true;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setBackendErrors({});
    createProfile(formData, {
      onSuccess: () => {
        navigate('/profiles');
      },
      onError: (error: any) => {
        console.error('Create user error:', error);
        
        // Handle validation errors from backend
        if (error.response?.data?.validationErrors) {
          setBackendErrors(error.response.data.validationErrors);
        } else if (error.response?.data?.message) {
          setValidationError(error.response.data.message);
        } else {
          setValidationError('Failed to create user. Please try again.');
        }
      },
    });
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <Button variant="ghost" onClick={() => navigate('/profiles')} className="mb-4">
          <ArrowLeft className="h-4 w-4 mr-2" />
          {t('common.back')}
        </Button>
        
        <h2 className="text-2xl font-bold">{t('profile.createNewUser')}</h2>
      </div>

      <Card className="max-w-2xl">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <UserPlus className="h-5 w-5" />
            {t('profile.userDetails')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {(error || validationError || Object.keys(backendErrors).length > 0) && (
              <Alert variant="destructive">
                <AlertDescription>
                  {validationError || (error instanceof Error ? error.message : 'Failed to create user')}
                  {Object.keys(backendErrors).length > 0 && (
                    <ul className="mt-2 list-disc list-inside">
                      {Object.entries(backendErrors).map(([field, message]) => (
                        <li key={field}>{message}</li>
                      ))}
                    </ul>
                  )}
                </AlertDescription>
              </Alert>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="email">{t('profile.email')} *</Label>
                <Input
                  id="email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => handleChange('email', e.target.value)}
                  required
                  className={backendErrors.email ? 'border-red-500' : ''}
                />
                {backendErrors.email && (
                  <p className="text-sm text-red-500 mt-1">{backendErrors.email}</p>
                )}
              </div>

              <div>
                <Label htmlFor="password">{t('profile.password')} *</Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    value={formData.password}
                    onChange={(e) => handleChange('password', e.target.value)}
                    required
                    minLength={8}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-muted-foreground"
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <p className="text-sm text-muted-foreground mt-1">
                  {t('profile.passwordRequirement')}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="firstName">{t('profile.firstName')} *</Label>
                <Input
                  id="firstName"
                  value={formData.firstName}
                  onChange={(e) => handleChange('firstName', e.target.value)}
                  required
                />
              </div>

              <div>
                <Label htmlFor="lastName">{t('profile.lastName')} *</Label>
                <Input
                  id="lastName"
                  value={formData.lastName}
                  onChange={(e) => handleChange('lastName', e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="phone">{t('profile.phoneNumber')}</Label>
                <Input
                  id="phone"
                  type="tel"
                  value={formData.phone}
                  onChange={(e) => handleChange('phone', e.target.value)}
                />
              </div>

              <div>
                <Label htmlFor="department">{t('profile.department')}</Label>
                <Input
                  id="department"
                  value={formData.department}
                  onChange={(e) => handleChange('department', e.target.value)}
                />
              </div>
            </div>

            <div>
              <Label htmlFor="role">{t('profile.role')}</Label>
              <Select value={formData.role} onValueChange={(value) => handleChange('role', value)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="EMPLOYEE">{t('roles.EMPLOYEE')}</SelectItem>
                  {currentUser?.role === 'SUPER_ADMIN' && (
                    <>
                      <SelectItem value="MANAGER">{t('roles.MANAGER')}</SelectItem>
                      <SelectItem value="SUPER_ADMIN">{t('roles.SUPER_ADMIN')}</SelectItem>
                    </>
                  )}
                </SelectContent>
              </Select>
              {currentUser?.role === 'MANAGER' && (
                <p className="text-sm text-muted-foreground mt-1">
                  As a manager, you can only create employees
                </p>
              )}
            </div>

            <div className="flex gap-3 pt-4">
              <Button type="submit" disabled={isPending}>
                <UserPlus className="h-4 w-4 mr-2" />
                {isPending ? t('common.creating') : t('profile.createUser')}
              </Button>
              <Button type="button" variant="outline" onClick={() => navigate('/profiles')}>
                {t('common.cancel')}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { User } from '@/services/userService';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Eye, Pencil, Trash2, Phone, Building2 } from 'lucide-react';

interface ProfileCardProps {
  user: User;
  canEdit?: boolean;
  canDelete?: boolean;
  onDelete?: () => void;
}

export const ProfileCard: React.FC<ProfileCardProps> = ({ user, canEdit = false, canDelete = false, onDelete }) => {
  const { t } = useTranslation();
  
  const handleDelete = () => {
    onDelete?.();
  };

  return (
    <Card className="hover:shadow-lg transition-shadow">
      <CardContent className="p-6">
        <div className="flex justify-between items-start">
          <div className="flex-1">
            <h3 className="text-xl font-bold">
              {user.firstName} {user.lastName}
            </h3>
            <p className="text-muted-foreground mb-3">{user.email}</p>

            <div className="flex items-center gap-6 text-sm flex-wrap">
              <Badge variant="secondary">
                {t(`roles.${user.role}`)}
              </Badge>
              
              {user.phone && (
                <div className="flex items-center gap-1.5 text-muted-foreground">
                  <Phone className="h-4 w-4" />
                  <span>{user.phone}</span>
                </div>
              )}
              
              {user.department && (
                <div className="flex items-center gap-1.5 text-muted-foreground">
                  <Building2 className="h-4 w-4" />
                  <span>{user.department}</span>
                </div>
              )}
            </div>
          </div>

          <div className="flex gap-2 ml-4">
            <Button variant="default" size="sm" asChild>
              <Link to={`/profiles/${user.id}`}>
                <Eye className="h-4 w-4 mr-1.5" />
                {t('profile.viewProfile')}
              </Link>
            </Button>

            {canEdit && (
              <Button variant="outline" size="sm" asChild>
                <Link to={`/profiles/${user.id}/edit`}>
                  <Pencil className="h-4 w-4 mr-1.5" />
                  {t('common.edit')}
                </Link>
              </Button>
            )}

            {canDelete && (
              <Button variant="destructive" size="sm" onClick={handleDelete}>
                <Trash2 className="h-4 w-4 mr-1.5" />
                {t('common.delete')}
              </Button>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

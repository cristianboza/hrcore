import { useState } from 'react';
import { useSubmitAbsenceRequest } from '../hooks/useAbsence';
import { useAuthStore } from '../store/authStore';
import { ABSENCE_TYPE, AbsenceType } from '../types/absence';
import { Button } from './ui/button';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import { UserSearchSelect } from './ui/user-search-select';
import { Calendar } from 'lucide-react';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';

interface AbsenceFormProps {
  userId?: string; // If provided, form is for this specific user (disabled input)
  onSuccess?: () => void;
}

export const AbsenceForm: React.FC<AbsenceFormProps> = ({ userId, onSuccess }) => {
  const [selectedUserId, setSelectedUserId] = useState(userId || '');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [type, setType] = useState<AbsenceType>(ABSENCE_TYPE.VACATION);
  const [reason, setReason] = useState('');
  const currentUser = useAuthStore((state) => state.currentUser);
  const { mutate: submitRequest, isPending } = useSubmitAbsenceRequest();

  const isManager = currentUser?.role === 'MANAGER' || currentUser?.role === 'SUPER_ADMIN';
  const isFormForSpecificUser = !!userId;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const targetUserId = userId || selectedUserId;
    if (!startDate || !endDate || !targetUserId) return;

    submitRequest(
      {
        userId: targetUserId,
        startDate,
        endDate,
        type,
        reason,
      },
      {
        onSuccess: () => {
          if (!userId) {
            setSelectedUserId('');
          }
          setStartDate('');
          setEndDate('');
          setType(ABSENCE_TYPE.VACATION);
          setReason('');
          onSuccess?.();
        },
      }
    );
  };

  const calculateDays = () => {
    if (!startDate || !endDate) return 0;
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {!isFormForSpecificUser && isManager && (
        <div className="space-y-2">
          <Label htmlFor="user">Employee *</Label>
          <UserSearchSelect
            value={selectedUserId}
            onValueChange={setSelectedUserId}
            placeholder="Select employee for absence request..."
            excludeUserId={currentUser?.id}
          />
          <p className="text-xs text-muted-foreground">
            As a manager, you can create absence requests for your team members
          </p>
        </div>
      )}
      
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="startDate">Start Date</Label>
          <div className="relative">
            <Calendar className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <input
              id="startDate"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              min={new Date().toISOString().split('T')[0]}
              required
              className="flex h-10 w-full rounded-md border border-input bg-background pl-10 pr-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="endDate">End Date</Label>
          <div className="relative">
            <Calendar className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <input
              id="endDate"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              min={startDate || new Date().toISOString().split('T')[0]}
              required
              className="flex h-10 w-full rounded-md border border-input bg-background pl-10 pr-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>
        </div>
      </div>

      {startDate && endDate && (
        <div className="text-sm text-muted-foreground bg-muted p-3 rounded-md">
          Duration: <span className="font-medium">{calculateDays()} days</span>
        </div>
      )}

      <div className="space-y-2">
        <Label htmlFor="type">Type</Label>
        <Select value={type} onValueChange={(value) => setType(value as AbsenceType)}>
          <SelectTrigger id="type">
            <SelectValue placeholder="Select type" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ABSENCE_TYPE.VACATION}>🏖️ Vacation</SelectItem>
            <SelectItem value={ABSENCE_TYPE.SICK}>🤒 Sick Leave</SelectItem>
            <SelectItem value={ABSENCE_TYPE.OTHER}>📋 Other</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label htmlFor="reason">Reason (Optional)</Label>
        <Textarea
          id="reason"
          value={reason}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setReason(e.target.value)}
          placeholder="Provide additional details about your absence request..."
          rows={4}
        />
      </div>

      <div className="flex gap-2 justify-end">
        <Button
          type="submit"
          disabled={isPending || !startDate || !endDate || (!userId && !selectedUserId)}
          className="w-full sm:w-auto"
        >
          {isPending ? 'Submitting...' : 'Submit Request'}
        </Button>
      </div>
    </form>
  );
};

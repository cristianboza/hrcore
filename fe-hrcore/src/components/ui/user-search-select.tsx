import { useState, useEffect, useCallback } from 'react';
import { Check, ChevronsUpDown, Loader2, User as UserIcon, X } from 'lucide-react';
import { cn } from '../../lib/utils';
import { Button } from './button';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
} from './command';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from './popover';
import { User } from '../../services/userService';
import { profileService } from '../../services/profileService';
import { useDebounce } from '../../hooks/useDebounce';

interface UserSearchSelectProps {
  value?: string;
  onValueChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  excludeUserId?: string;
  className?: string;
  allowClear?: boolean;
}

export function UserSearchSelect({
  value,
  onValueChange,
  placeholder = 'Select user...',
  disabled = false,
  excludeUserId,
  className,
  allowClear = false,
}: UserSearchSelectProps) {
  const [open, setOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);

  const debouncedSearch = useDebounce(searchQuery, 300);

  const fetchUsers = useCallback(async (search: string) => {
    setLoading(true);
    try {
      const response = await profileService.searchProfiles({
        search,
        page: 0,
        size: 20,
      });
      
      let filteredUsers = response.content;
      if (excludeUserId) {
        filteredUsers = filteredUsers.filter(u => u.id !== excludeUserId);
      }
      
      setUsers(filteredUsers);
    } catch (error) {
      console.error('Failed to fetch users:', error);
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, [excludeUserId]);

  useEffect(() => {
    if (open) {
      fetchUsers(debouncedSearch);
    }
  }, [debouncedSearch, open, fetchUsers]);

  // Fetch selected user when value changes
  useEffect(() => {
    if (value) {
      profileService.getProfile(value)
        .then(user => setSelectedUser(user))
        .catch(() => setSelectedUser(null));
    } else {
      setSelectedUser(null);
    }
  }, [value]);

  return (
    <div className="relative">
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            role="combobox"
            aria-expanded={open}
            disabled={disabled}
            className={cn('w-full justify-between', className)}
          >
            {selectedUser ? (
              <div className="flex items-center gap-2 truncate">
                <UserIcon className="h-4 w-4 flex-shrink-0" />
                <span className="truncate">
                  {selectedUser.firstName} {selectedUser.lastName}
                </span>
                <span className="text-xs text-muted-foreground truncate">
                  ({selectedUser.email})
                </span>
              </div>
            ) : (
              <span className="text-muted-foreground">{placeholder}</span>
            )}
            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-[400px] p-0">
          <Command>
            <CommandInput
              placeholder="Search users..."
              value={searchQuery}
              onValueChange={setSearchQuery}
            />
            <CommandEmpty>
              {loading ? (
                <div className="flex items-center justify-center py-6">
                  <Loader2 className="h-4 w-4 animate-spin" />
                </div>
              ) : (
                'No users found.'
              )}
            </CommandEmpty>
            <CommandGroup className="max-h-[300px] overflow-auto">
              {users.map((user) => (
                <CommandItem
                  key={user.id}
                  value={`${user.firstName} ${user.lastName} ${user.email}`}
                  onSelect={() => {
                    onValueChange(user.id);
                    setSelectedUser(user);
                    setOpen(false);
                    setSearchQuery('');
                  }}
                >
                  <Check
                    className={cn(
                      'mr-2 h-4 w-4',
                      value === user.id ? 'opacity-100' : 'opacity-0'
                    )}
                  />
                  <div className="flex items-center gap-2 truncate">
                    <UserIcon className="h-4 w-4 flex-shrink-0" />
                    <div className="flex flex-col truncate">
                      <span className="truncate">
                        {user.firstName} {user.lastName}
                      </span>
                      <span className="text-xs text-muted-foreground truncate">
                        {user.email}
                        {user.department && ` • ${user.department}`}
                      </span>
                    </div>
                  </div>
                </CommandItem>
              ))}
            </CommandGroup>
          </Command>
        </PopoverContent>
      </Popover>
      {allowClear && value && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onValueChange('');
            setSelectedUser(null);
          }}
          className="absolute right-10 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground p-1 rounded-sm hover:bg-accent"
        >
          <X className="h-3 w-3" />
        </button>
      )}
    </div>
  );
}

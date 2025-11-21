import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {profileService, ProfileSearchFilters} from '../services/profileService';
import {User} from '../services/userService';

/**
 * Hook to fetch all profiles with optional filters
 */
export const useProfiles = (filters?: ProfileSearchFilters) => {
    return useQuery({
        queryKey: ['profiles', filters],
        queryFn: () => profileService.searchProfiles(filters),
        staleTime: 1000 * 60 * 5, // 5 minutes
    });
};

/**
 * Hook to fetch a single profile
 */
export const useProfile = (userId: string | undefined) => {
    return useQuery({
        queryKey: ['profile', userId],
        queryFn: () => profileService.getProfile(userId!),
        staleTime: 1000 * 60 * 5, // 5 minutes
        enabled: !!userId, // Only run if userId is provided
    });
};

/**
 * Hook to fetch profile permissions
 */
export const useProfilePermissions = (userId: string | undefined) => {
    return useQuery({
        queryKey: ['profilePermissions', userId],
        queryFn: () => profileService.getProfilePermissions(userId!),
        staleTime: 1000 * 60 * 5, // 5 minutes
        enabled: !!userId,
    });
};

/**
 * Hook to fetch current authenticated user
 */
export const useCurrentUser = () => {
    return useQuery({
        queryKey: ['currentUser'],
        queryFn: () => profileService.getCurrentUser(),
        staleTime: 1000 * 60 * 10, // 10 minutes
    });
};

/**
 * Hook to fetch direct reports for a user
 */
export const useDirectReports = (userId: string | undefined) => {
    return useQuery({
        queryKey: ['directReports', userId],
        queryFn: () => profileService.getDirectReports(userId!),
        staleTime: 1000 * 60 * 5,
        enabled: !!userId,
    });
};

/**
 * Hook to fetch available managers
 */
export const useAvailableManagers = () => {
    return useQuery({
        queryKey: ['availableManagers'],
        queryFn: () => profileService.getAvailableManagers(),
        staleTime: 1000 * 60 * 5,
    });
};

/**
 * Hook to fetch the manager of a user
 */
export const useManager = (userId: string | undefined) => {
    return useQuery({
        queryKey: ['manager', userId],
        queryFn: () => profileService.getManager(userId!),
        staleTime: 1000 * 60 * 5,
        enabled: !!userId,
    });
};

/**
 * Hook to update a profile
 */
export const useUpdateProfile = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({
                         userId,
                         updateData,
                     }: {
            userId: string;
            updateData: Partial<User>;
        }) => profileService.updateProfile(userId, updateData),
        onSuccess: (_, {userId}) => {
            // Invalidate profile queries to refetch
            queryClient.invalidateQueries({queryKey: ['profile', userId]});
            queryClient.invalidateQueries({queryKey: ['profiles']});
            queryClient.invalidateQueries({queryKey: ['currentUser']});
        },
    });
};

/**
 * Hook to delete a profile
 */
export const useDeleteProfile = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (userId: string) => profileService.deleteProfile(userId),
        onSuccess: () => {
            // Invalidate profiles list
            queryClient.invalidateQueries({queryKey: ['profiles']});
        },
    });
};

/**
 * Hook to assign a manager to a user
 */
export const useAssignManager = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({userId, managerId}: { userId: string; managerId: string }) =>
            profileService.assignManager(userId, managerId),
        onSuccess: (_, {userId}) => {
            // Invalidate related queries
            queryClient.invalidateQueries({queryKey: ['profile', userId]});
            queryClient.invalidateQueries({queryKey: ['profiles']});
            queryClient.invalidateQueries({queryKey: ['manager', userId]});
        },
    });
};

/**
 * Hook to remove a manager from a user
 */
export const useRemoveManager = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (userId: string) => profileService.removeManager(userId),
        onSuccess: (_, userId) => {
            // Invalidate related queries
            queryClient.invalidateQueries({queryKey: ['profile', userId]});
            queryClient.invalidateQueries({queryKey: ['profiles']});
            queryClient.invalidateQueries({queryKey: ['manager', userId]});
        },
    });
};

/**
 * Hook to create a new profile
 */
export const useCreateProfile = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (userData: {
            email: string;
            firstName: string;
            lastName: string;
            password: string;
            phone?: string;
            department?: string;
            role?: string;
            managerId?: string;
        }) => profileService.createProfile(userData),
        onSuccess: () => {
            // Invalidate profiles list
            queryClient.invalidateQueries({queryKey: ['profiles']});
        },
    });
};


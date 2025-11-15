import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {profileService} from '../services/profileService';
import {User} from '../services/userService';

/**
 * Hook to fetch all profiles with role-based filtering
 */
export const useProfiles = () => {
    return useQuery({
        queryKey: ['profiles'],
        queryFn: () => profileService.getAllProfiles(),
        staleTime: 1000 * 60 * 5, // 5 minutes
    });
};

/**
 * Hook to fetch a single profile
 */
export const useProfile = (userId: number) => {
    return useQuery({
        queryKey: ['profile', userId],
        queryFn: () => profileService.getProfile(userId),
        staleTime: 1000 * 60 * 5, // 5 minutes
        enabled: !!userId, // Only run if userId is provided
    });
};

/**
 * Hook to fetch profile permissions
 */
export const useProfilePermissions = (userId: number) => {
    return useQuery({
        queryKey: ['profilePermissions', userId],
        queryFn: () => profileService.getProfilePermissions(userId),
        staleTime: 1000 * 60 * 5, // 5 minutes
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
            userId: number;
            updateData: Partial<User>;
        }) => profileService.updateProfile(userId, updateData),
        onSuccess: (_, {userId}) => {
            // Invalidate profile queries to refetch
            queryClient.invalidateQueries({queryKey: ['profile', userId]});
            queryClient.invalidateQueries({queryKey: ['profiles']});
        },
    });
};

/**
 * Hook to delete a profile
 */
export const useDeleteProfile = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({userId}: {userId: number}) => profileService.deleteProfile(userId),
        onSuccess: () => {
            // Invalidate profiles list
            queryClient.invalidateQueries({queryKey: ['profiles']});
        },
    });
};


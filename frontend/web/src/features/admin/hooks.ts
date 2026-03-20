import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from './api';
import toast from 'react-hot-toast';

const QUERY_KEYS = {
  blockList: ['admin', 'blocks'] as const,
};

export function useBlockList() {
  return useQuery({
    queryKey: QUERY_KEYS.blockList,
    queryFn: () => adminApi.getBlockList(),
    select: (res) => res.data,
  });
}

export function useAddBlock() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: adminApi.addBlock,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.blockList });
      toast.success('사용자를 차단했습니다.');
    },
    onError: (error: any) => {
      const errorMessage = error?.response?.data?.error?.message || '차단 처리에 실패했습니다.';
      toast.error(errorMessage);
    },
  });
}

export function useRemoveBlock() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: adminApi.removeBlock,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.blockList });
      toast.success('차단을 해제했습니다.');
    },
    onError: (error: any) => {
      const errorMessage = error?.response?.data?.error?.message || '차단 해제에 실패했습니다.';
      toast.error(errorMessage);
    },
  });
}

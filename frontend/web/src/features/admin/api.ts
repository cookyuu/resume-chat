import { apiClient } from '@/shared/api';
import type { ApiResponse } from '@/shared/types/api';
import type { BlockedUser, AddBlockRequest, BlockListResponse } from './types';

export const adminApi = {
  // 차단 목록 조회
  getBlockList: () =>
    apiClient.get<ApiResponse<BlockListResponse>>('/admin/blocks').then((res) => res.data),

  // 사용자 차단 추가
  addBlock: (data: AddBlockRequest) =>
    apiClient.post<ApiResponse<BlockedUser>>('/admin/blocks', data).then((res) => res.data),

  // 사용자 차단 해제
  removeBlock: (id: string) =>
    apiClient.delete<ApiResponse<void>>(`/admin/blocks/${id}`).then((res) => res.data),
};

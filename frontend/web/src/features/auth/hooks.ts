import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi } from './api';
import { useAuthStore } from '@/shared/store/auth';
import { queryKeys } from '@/shared/lib/queryKeys';
import type { LoginRequest, JoinRequest } from '@/entities/applicant';

/** 지원자 로그인 */
export function useLogin() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (res) => {
      const { accessToken, uuid, email, name } = res.data;
      setAuth(accessToken, { uuid, email, name });
      toast.success('로그인 성공');
      navigate('/resumes');
    },
  });
}

/** 지원자 회원가입 */
export function useJoin() {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: (data: JoinRequest) => authApi.join(data),
    onSuccess: () => {
      toast.success('회원가입이 완료되었습니다. 로그인해주세요.');
      navigate('/login');
    },
  });
}

/** 지원자 프로필 조회 */
export function useProfile() {
  return useQuery({
    queryKey: queryKeys.applicant.profile,
    queryFn: () => authApi.getProfile(),
    select: (res) => res.data,
  });
}

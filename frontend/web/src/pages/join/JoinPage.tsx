import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useJoin } from '@/features/auth';
import { Button, Input } from '@/shared/ui';

export function JoinPage() {
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const joinMutation = useJoin();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== passwordConfirm) {
      return;
    }
    joinMutation.mutate({ email, password, name });
  };

  const [touched, setTouched] = useState({ name: false, password: false, passwordConfirm: false });
  const handleBlur = (field: keyof typeof touched) => () => setTouched((prev) => ({ ...prev, [field]: true }));

  const nameError = touched.name && name.length > 0 && name.length < 2 ? '이름은 2자 이상이어야 합니다' : undefined;
  const passwordError = touched.password && password.length > 0 && password.length < 8 ? '비밀번호는 8자 이상이어야 합니다' : undefined;
  const passwordMismatch = touched.passwordConfirm && passwordConfirm.length > 0 && password !== passwordConfirm;

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md p-8 bg-white rounded-xl shadow-sm">
        <h1 className="text-2xl font-bold text-center mb-8">회원가입</h1>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Input
            label="이메일"
            type="email"
            placeholder="이메일을 입력하세요"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Input
            label="이름"
            placeholder="이름을 입력하세요 (2~50자)"
            value={name}
            onChange={(e) => setName(e.target.value)}
            onBlur={handleBlur('name')}
            error={nameError}
            required
            minLength={2}
            maxLength={50}
          />
          <Input
            label="비밀번호"
            type="password"
            placeholder="비밀번호를 입력하세요 (8~20자)"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onBlur={handleBlur('password')}
            error={passwordError}
            required
            minLength={8}
            maxLength={20}
          />
          <Input
            label="비밀번호 확인"
            type="password"
            placeholder="비밀번호를 다시 입력하세요"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            onBlur={handleBlur('passwordConfirm')}
            error={passwordMismatch ? '비밀번호가 일치하지 않습니다' : undefined}
            required
          />
          <Button type="submit" loading={joinMutation.isPending} className="mt-2">
            회원가입
          </Button>
        </form>
        <p className="mt-4 text-center text-sm text-gray-500">
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className="text-blue-600 hover:underline">로그인</Link>
        </p>
      </div>
    </div>
  );
}

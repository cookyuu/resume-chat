import { useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useMyResumes, useUploadResume, useDeleteResume } from '@/features/resume';
import { Button, Input, Skeleton, EmptyState } from '@/shared/ui';
import { formatDateTime } from '@/shared/lib/date';

export function ResumesPage() {
  const { data: resumes, isLoading, isError } = useMyResumes();
  const uploadMutation = useUploadResume();
  const deleteMutation = useDeleteResume();

  const [showUpload, setShowUpload] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [file, setFile] = useState<File | null>(null);

  const handleUpload = (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;
    uploadMutation.mutate(
      { title, description, file },
      {
        onSuccess: () => {
          setShowUpload(false);
          setTitle('');
          setDescription('');
          setFile(null);
        },
      },
    );
  };

  const handleDelete = (resumeSlug: string) => {
    if (window.confirm('이력서를 삭제하시겠습니까? 관련 채팅도 함께 삭제됩니다.')) {
      deleteMutation.mutate(resumeSlug);
    }
  };

  const handleCopyLink = (chatLink: string) => {
    navigator.clipboard.writeText(chatLink);
    toast.success('채팅 링크가 복사되었습니다.');
  };

  // ── Loading ──
  if (isLoading) {
    return (
      <div className="p-6 max-w-5xl mx-auto">
        <Skeleton className="h-8 w-40 mb-6" />
        <Skeleton className="h-10 w-full mb-2" />
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-14 w-full mb-1" />
        ))}
      </div>
    );
  }

  // ── Error ──
  if (isError) {
    return (
      <div className="p-6 max-w-5xl mx-auto">
        <p className="text-red-500">이력서 목록을 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">내 이력서</h1>
        <Button onClick={() => setShowUpload(!showUpload)}>
          {showUpload ? '취소' : '이력서 업로드'}
        </Button>
      </div>

      {/* Upload Form */}
      {showUpload && (
        <form onSubmit={handleUpload} className="mb-6 p-5 bg-white border rounded-lg space-y-4">
          <h2 className="font-semibold text-lg">이력서 업로드</h2>
          <Input
            label="제목"
            placeholder="이력서 제목 (2~100자)"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            minLength={2}
            maxLength={100}
          />
          <Input
            label="설명"
            placeholder="이력서 설명 (최대 500자, 선택)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={500}
          />
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700">PDF 파일 (최대 10MB)</label>
            <input
              type="file"
              accept=".pdf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              className="text-sm file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
              required
            />
          </div>
          <div className="flex gap-2">
            <Button type="submit" loading={uploadMutation.isPending}>업로드</Button>
            <Button variant="secondary" type="button" onClick={() => setShowUpload(false)}>취소</Button>
          </div>
        </form>
      )}

      {/* Table */}
      {!resumes || resumes.length === 0 ? (
        <EmptyState message="업로드한 이력서가 없습니다" />
      ) : (
        <div className="bg-white border rounded-lg overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b text-left text-gray-500">
                <th className="px-4 py-3 font-medium">제목</th>
                <th className="px-4 py-3 font-medium">파일명</th>
                <th className="px-4 py-3 font-medium text-center">조회수</th>
                <th className="px-4 py-3 font-medium">업로드일</th>
                <th className="px-4 py-3 font-medium text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {resumes.map((resume) => (
                <tr key={resume.resumeSlug} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3">
                    <div>
                      <p className="font-medium text-gray-900 truncate max-w-[200px]">{resume.title}</p>
                      {resume.description && (
                        <p className="text-xs text-gray-400 truncate max-w-[200px]">{resume.description}</p>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{resume.originalFileName}</td>
                  <td className="px-4 py-3 text-center text-gray-600">{resume.viewCnt}</td>
                  <td className="px-4 py-3 text-gray-500">{formatDateTime(resume.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <Link to={`/resumes/${resume.resumeSlug}/chats`}>
                        <button className="px-2.5 py-1.5 text-xs font-medium text-blue-600 bg-blue-50 rounded hover:bg-blue-100">
                          채팅
                        </button>
                      </Link>
                      <button
                        onClick={() => handleCopyLink(resume.chatLink)}
                        className="px-2.5 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded hover:bg-gray-200"
                      >
                        링크
                      </button>
                      <button
                        onClick={() => handleDelete(resume.resumeSlug)}
                        disabled={deleteMutation.isPending}
                        className="px-2.5 py-1.5 text-xs font-medium text-red-600 bg-red-50 rounded hover:bg-red-100 disabled:opacity-50"
                      >
                        삭제
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

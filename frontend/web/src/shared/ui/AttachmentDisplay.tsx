import { DocumentTextIcon, ArrowDownTrayIcon, DocumentIcon, PhotoIcon, ArchiveBoxIcon, TableCellsIcon } from '@heroicons/react/24/outline';

interface AttachmentDisplayProps {
  attachmentId?: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  fileUrl: string;
}

/**
 * 첨부파일 표시 컴포넌트
 * - 파일 타입별 아이콘
 * - 파일명과 크기 표시
 * - 다운로드 버튼
 * - 이미지 파일은 인라인 표시
 */
export function AttachmentDisplay({ attachmentId, fileName, fileSize, fileType, fileUrl }: AttachmentDisplayProps) {
  // undefined 체크 추가
  const isImage = fileType?.startsWith('image/') ?? false;
  const isPdf = fileType === 'application/pdf';
  const isZip = fileType === 'application/zip' || fileType === 'application/x-zip-compressed';
  const isWord = fileType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
                 fileType === 'application/msword';
  const isExcel = fileType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
                  fileType === 'application/vnd.ms-excel';
  const isPpt = fileType === 'application/vnd.openxmlformats-officedocument.presentationml.presentation';
  const formattedSize = formatFileSize(fileSize ?? 0);

  // fileUrl이 상대 경로인지 확인하여 절대 경로로 변환
  const downloadUrl = fileUrl && !fileUrl.startsWith('http')
    ? window.location.origin + fileUrl
    : fileUrl ?? '';

  if (isImage) {
    return (
      <div className="space-y-2">
        <div className="relative group">
          <img
            src={downloadUrl}
            alt={fileName}
            className="max-w-sm max-h-64 rounded-lg cursor-pointer hover:opacity-90 transition-opacity"
            onClick={() => window.open(downloadUrl, '_blank')}
          />
          <a
            href={downloadUrl}
            download={fileName}
            className="absolute top-2 right-2 p-2 bg-black bg-opacity-50 hover:bg-opacity-70 rounded-full transition-all opacity-0 group-hover:opacity-100"
            title="다운로드"
          >
            <ArrowDownTrayIcon className="w-5 h-5 text-white" />
          </a>
        </div>
        <div className="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400">
          <span>{fileName}</span>
          <span>•</span>
          <span>{formattedSize}</span>
        </div>
      </div>
    );
  }

  // 파일 타입별 아이콘 선택
  const FileIcon = isPdf
    ? DocumentTextIcon
    : isExcel
    ? TableCellsIcon
    : isWord
    ? DocumentTextIcon
    : isPpt
    ? DocumentTextIcon
    : isZip
    ? ArchiveBoxIcon
    : isImage
    ? PhotoIcon
    : DocumentIcon;

  const iconColor = isPdf
    ? 'text-red-500'
    : isExcel
    ? 'text-green-600'
    : isWord
    ? 'text-blue-600'
    : isPpt
    ? 'text-orange-500'
    : isZip
    ? 'text-yellow-500'
    : isImage
    ? 'text-green-500'
    : 'text-blue-500';

  return (
    <div className="flex items-center gap-3 p-3 bg-gray-50 dark:bg-gray-700 rounded-lg border dark:border-gray-600 max-w-sm">
      <div className="flex-shrink-0">
        <FileIcon className={`w-8 h-8 ${iconColor}`} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-900 dark:text-white truncate">{fileName}</p>
        <p className="text-xs text-gray-500 dark:text-gray-400">{formattedSize}</p>
      </div>
      <a
        href={downloadUrl}
        download={fileName}
        className="flex-shrink-0 p-2 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-full transition-colors"
        title="다운로드"
      >
        <ArrowDownTrayIcon className="w-5 h-5 text-gray-600 dark:text-gray-300" />
      </a>
    </div>
  );
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

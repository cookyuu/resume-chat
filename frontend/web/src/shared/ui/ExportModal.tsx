import { useState } from 'react';
import { XMarkIcon } from '@heroicons/react/24/outline';
import { Button } from './Button';

interface ExportModalProps {
  onExport: (options: { format: 'txt' | 'json'; includeAttachments: boolean }) => void;
  onClose: () => void;
  isExporting: boolean;
}

/**
 * 채팅 내보내기 모달
 * - 포맷 선택 (TXT/JSON)
 * - 첨부파일 포함 여부
 */
export function ExportModal({ onExport, onClose, isExporting }: ExportModalProps) {
  const [format, setFormat] = useState<'txt' | 'json'>('txt');
  const [includeAttachments, setIncludeAttachments] = useState(true);

  const handleExport = () => {
    onExport({ format, includeAttachments });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full mx-4 transition-colors">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b dark:border-gray-700">
          <h2 className="text-lg font-semibold dark:text-white">채팅 내보내기</h2>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-colors"
          >
            <XMarkIcon className="w-5 h-5 text-gray-600 dark:text-gray-400" />
          </button>
        </div>

        {/* Content */}
        <div className="px-6 py-4 space-y-4">
          {/* 포맷 선택 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              파일 포맷
            </label>
            <div className="flex gap-3">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="format"
                  value="txt"
                  checked={format === 'txt'}
                  onChange={(e) => setFormat(e.target.value as 'txt')}
                  className="w-4 h-4 text-blue-600 focus:ring-blue-500"
                />
                <span className="text-sm text-gray-700 dark:text-gray-300">텍스트 (.txt)</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="format"
                  value="json"
                  checked={format === 'json'}
                  onChange={(e) => setFormat(e.target.value as 'json')}
                  className="w-4 h-4 text-blue-600 focus:ring-blue-500"
                />
                <span className="text-sm text-gray-700 dark:text-gray-300">JSON (.json)</span>
              </label>
            </div>
          </div>

          {/* 첨부파일 포함 */}
          <div>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={includeAttachments}
                onChange={(e) => setIncludeAttachments(e.target.checked)}
                className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
              />
              <span className="text-sm text-gray-700 dark:text-gray-300">첨부파일 정보 포함</span>
            </label>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 ml-6">
              첨부파일 URL이 텍스트에 포함됩니다
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-gray-50 dark:bg-gray-900 border-t dark:border-gray-700 flex justify-end gap-2 rounded-b-lg">
          <Button variant="secondary" onClick={onClose} disabled={isExporting}>
            취소
          </Button>
          <Button onClick={handleExport} loading={isExporting}>
            내보내기
          </Button>
        </div>
      </div>
    </div>
  );
}

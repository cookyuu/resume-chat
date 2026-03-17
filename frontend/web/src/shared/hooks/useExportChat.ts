import { useState } from 'react';
import toast from 'react-hot-toast';
import { formatDateTime } from '@/shared/lib/date';

interface Message {
  messageId: string;
  message: string;
  senderType: string;
  sentAt: string;
  attachment?: {
    fileName: string;
    fileSize: number;
    fileType: string;
    fileUrl: string;
  };
}

interface ExportOptions {
  format: 'txt' | 'json';
  includeAttachments?: boolean;
}

/**
 * 채팅 내보내기 훅
 * - TXT, JSON 포맷 지원
 * - 첨부파일 정보 포함 옵션
 */
export function useExportChat(messages: Message[] | undefined, sessionInfo?: { recruiterName?: string; recruiterCompany?: string }) {
  const [isExporting, setIsExporting] = useState(false);

  const exportChat = ({ format, includeAttachments = true }: ExportOptions) => {
    if (!messages || messages.length === 0) {
      toast.error('내보낼 메시지가 없습니다');
      return;
    }

    setIsExporting(true);

    try {
      let content: string;
      let filename: string;
      let mimeType: string;

      if (format === 'txt') {
        content = exportAsTxt(messages, includeAttachments, sessionInfo);
        filename = `chat_${Date.now()}.txt`;
        mimeType = 'text/plain';
      } else {
        content = exportAsJson(messages, includeAttachments, sessionInfo);
        filename = `chat_${Date.now()}.json`;
        mimeType = 'application/json';
      }

      // 파일 다운로드
      const blob = new Blob([content], { type: mimeType });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.click();
      URL.revokeObjectURL(url);

      toast.success('채팅이 내보내졌습니다');
    } catch (error) {
      toast.error('내보내기에 실패했습니다');
    } finally {
      setIsExporting(false);
    }
  };

  return {
    exportChat,
    isExporting,
  };
}

function exportAsTxt(messages: Message[], includeAttachments: boolean, sessionInfo?: { recruiterName?: string; recruiterCompany?: string }): string {
  let content = '========== 채팅 히스토리 ==========\n\n';

  if (sessionInfo) {
    content += `채용담당자: ${sessionInfo.recruiterName}\n`;
    content += `회사: ${sessionInfo.recruiterCompany}\n`;
    content += `내보낸 날짜: ${formatDateTime(new Date().toISOString())}\n\n`;
    content += '===================================\n\n';
  }

  messages.forEach((msg) => {
    const sender = msg.senderType === 'APPLICANT' ? '지원자' : '채용담당자';
    content += `[${formatDateTime(msg.sentAt)}] ${sender}\n`;
    content += `${msg.message}\n`;

    if (includeAttachments && msg.attachment) {
      content += `첨부파일: ${msg.attachment.fileName} (${msg.attachment.fileUrl})\n`;
    }

    content += '\n';
  });

  return content;
}

function exportAsJson(messages: Message[], includeAttachments: boolean, sessionInfo?: { recruiterName?: string; recruiterCompany?: string }): string {
  const data = {
    sessionInfo,
    exportedAt: new Date().toISOString(),
    messageCount: messages.length,
    messages: messages.map((msg) => ({
      messageId: msg.messageId,
      message: msg.message,
      senderType: msg.senderType,
      sentAt: msg.sentAt,
      ...(includeAttachments && msg.attachment ? { attachment: msg.attachment } : {}),
    })),
  };

  return JSON.stringify(data, null, 2);
}

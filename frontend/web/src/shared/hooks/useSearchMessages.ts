import { useState, useMemo } from 'react';

interface Message {
  messageId: string;
  message: string;
  senderType: string;
  sentAt: string;
  attachment?: {
    attachmentId: string;
    fileName: string;
    fileSize: number;
    fileType: string;
    fileUrl: string;
  };
}

/**
 * 채팅 메시지 검색 훅
 * - 키워드로 메시지 필터링
 * - 검색 결과 개수 추적
 */
export function useSearchMessages(messages: Message[] | undefined) {
  const [searchKeyword, setSearchKeyword] = useState('');

  const filteredMessages = useMemo(() => {
    if (!messages || !searchKeyword.trim()) {
      return messages || [];
    }

    const keyword = searchKeyword.toLowerCase();
    return messages.filter((msg) =>
      msg.message.toLowerCase().includes(keyword)
    );
  }, [messages, searchKeyword]);

  const searchResultCount = searchKeyword.trim() ? filteredMessages.length : 0;

  return {
    searchKeyword,
    setSearchKeyword,
    filteredMessages,
    searchResultCount,
    isSearching: searchKeyword.trim().length > 0,
  };
}

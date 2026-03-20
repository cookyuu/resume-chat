/**
 * 텍스트에서 키워드를 찾아 하이라이트 처리
 * XSS 방지를 위해 텍스트를 이스케이프 처리
 */
export function highlightKeyword(text: string, keyword: string): string {
  if (!keyword.trim()) {
    return escapeHtml(text);
  }

  const escapedText = escapeHtml(text);
  const escapedKeyword = escapeRegExp(keyword);
  const regex = new RegExp(`(${escapedKeyword})`, 'gi');

  return escapedText.replace(
    regex,
    '<mark class="bg-yellow-200 dark:bg-yellow-600 text-gray-900 dark:text-white px-0.5 rounded">$1</mark>'
  );
}

/**
 * HTML 특수문자 이스케이프
 */
function escapeHtml(text: string): string {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

/**
 * 정규표현식 특수문자 이스케이프
 */
function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

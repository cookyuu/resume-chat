export interface ApiResponse<T = unknown> {
  success: boolean;
  data: T;
  error?: ErrorInfo;
  timestamp: string;
}

export interface ErrorInfo {
  code: string;
  message: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

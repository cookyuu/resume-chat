export interface BlockedUser {
  id: string;
  email: string;
  reason: string;
  blockedAt: string;
  blockedBy: string;
}

export interface AddBlockRequest {
  email: string;
  reason: string;
}

export interface BlockListResponse {
  blockedUsers: BlockedUser[];
}

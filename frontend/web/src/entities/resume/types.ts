export interface Resume {
  resumeSlug: string;
  title: string;
  description: string;
  originalFileName: string;
  fileUrl: string;
  chatLink: string;
  viewCnt: number;
  createdAt: string;
}

export interface UploadResumeParams {
  title: string;
  description?: string;
  file: File;
}

export interface CommonResponse<T> {
  message: string;
  code: number;
  data: T;
  traceId?: string;
  spanId?: string;
}

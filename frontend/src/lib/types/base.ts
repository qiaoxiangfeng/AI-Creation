export interface BaseResponse<T> {
  code: string;
  msg: string;
  data: T;
}

export interface PageReqDto {
  pageNo: number;
  pageSize: number;
}

export interface PageRespDto<T> {
  pageNo: number;
  pageSize: number;
  size: number;
  total: number;
  pages: number;
  list: T[];
}



import { requestClient } from '#/api/request';

export namespace UploadApi {
  export interface ImageResult {
    /** 服务端生成的文件名 */
    name: string;
    /** 文件字节数 */
    size: number;
    /** 可直接使用的访问地址（含 context-path） */
    url: string;
  }
}

/**
 * 上传图片。服务端按文件头校验类型，单图上限 5MB。
 * 走 RequestClient 内置的 upload（显式 multipart），避免默认 JSON 头把 FormData 序列化掉。
 */
export async function uploadImageApi(file: File) {
  return requestClient.upload<UploadApi.ImageResult>('/upload/image', { file });
}

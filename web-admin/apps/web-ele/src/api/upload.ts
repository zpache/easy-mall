import COS from 'cos-js-sdk-v5';

import { requestClient } from '#/api/request';

export namespace UploadApi {
  /** 上传凭证：cos 模式携带 STS 临时密钥，local 模式表示回退服务端转存 */
  export interface Credential {
    allowExtensions: string[];
    bucket?: string;
    domain?: string;
    expiredTime?: number;
    keyPrefix?: string;
    maxSize: number;
    mode: 'cos' | 'local';
    region?: string;
    sessionToken?: string;
    startTime?: number;
    tmpSecretId?: string;
    tmpSecretKey?: string;
  }

  export interface LocalImageResult {
    name: string;
    size: number;
    url: string;
  }

  export interface UploadedFile {
    id: number;
    mime?: string;
    objectKey: string;
    size: number;
    url: string;
  }

  export interface UploadOptions {
    /** 上传进度 0-100 */
    onProgress?: (percent: number) => void;
    /** 使用场景，登记到 sys_file.scene */
    scene?: string;
  }

  export interface UploadResult {
    objectKey: string;
    size: number;
    url: string;
  }
}

export function getUploadCredentialApi() {
  return requestClient.post<UploadApi.Credential>('/upload/credential');
}

export function completeUploadApi(payload: {
  mime?: string;
  objectKey: string;
  scene?: string;
  size: number;
}) {
  return requestClient.post<UploadApi.UploadedFile>('/upload/complete', payload);
}

function localUpload(file: File) {
  return requestClient.upload<UploadApi.LocalImageResult>('/upload/image', {
    file,
  });
}

/**
 * 生成对象键：前缀 + 年月 + 随机名 + 原扩展名。
 * 前缀必须在后端授权范围内，否则回执校验会拒绝。
 */
function buildObjectKey(prefix: string, file: File) {
  const dot = file.name.lastIndexOf('.');
  const ext = dot >= 0 ? file.name.slice(dot + 1).toLowerCase() : 'png';
  const now = new Date();
  const month = `${now.getFullYear()}/${String(now.getMonth() + 1).padStart(2, '0')}`;
  const random =
    globalThis.crypto?.randomUUID?.().replaceAll('-', '') ??
    `${Date.now()}${Math.random().toString(16).slice(2, 10)}`;
  return `${prefix}${month}/${random}.${ext}`;
}

/**
 * 上传图片。
 * COS 模式：取临时密钥 → 浏览器直传 COS → 回传 objectKey 换取访问地址。
 * local 模式：回退到服务端转存（仅本机开发）。
 */
export async function uploadImageApi(
  file: File,
  options: UploadApi.UploadOptions = {},
): Promise<UploadApi.UploadResult> {
  const credential = await getUploadCredentialApi();

  if (credential.mode !== 'cos') {
    const result = await localUpload(file);
    options.onProgress?.(100);
    return { objectKey: result.name, size: result.size, url: result.url };
  }

  if (file.size > credential.maxSize) {
    throw new Error(
      `图片不能超过 ${Math.round(credential.maxSize / 1024 / 1024)}MB`,
    );
  }
  const ext = file.name.slice(file.name.lastIndexOf('.') + 1).toLowerCase();
  if (!credential.allowExtensions.includes(ext)) {
    throw new Error(`仅支持 ${credential.allowExtensions.join(' / ')} 格式`);
  }

  const cos = new COS({
    getAuthorization: (_options, callback) => {
      callback({
        TmpSecretId: credential.tmpSecretId!,
        TmpSecretKey: credential.tmpSecretKey!,
        SecurityToken: credential.sessionToken!,
        StartTime: credential.startTime!,
        ExpiredTime: credential.expiredTime!,
      });
    },
  });

  const objectKey = buildObjectKey(credential.keyPrefix!, file);

  await new Promise<void>((resolve, reject) => {
    cos.putObject(
      {
        Bucket: credential.bucket!,
        Region: credential.region!,
        Key: objectKey,
        Body: file,
        // SDK 运行时支持 onProgress，但类型声明未覆盖
        onProgress: (info: { percent?: number }) => {
          options.onProgress?.(Math.round((info.percent ?? 0) * 100));
        },
      } as any,
      (error: any) => {
        if (error) {
          // 没有 HTTP 状态码说明请求没到 COS：多为桶未配 CORS 或网络中断
          if (!error.statusCode) {
            reject(
              new Error(
                '图片直传失败：请求被跨域拦截或网络中断，请检查 COS 桶的 CORS 配置',
              ),
            );
            return;
          }
          reject(new Error(error.message || '图片上传失败'));
          return;
        }
        resolve();
      },
    );
  });

  const uploaded = await completeUploadApi({
    objectKey,
    size: file.size,
    mime: file.type,
    scene: options.scene,
  });

  options.onProgress?.(100);
  return {
    objectKey: uploaded.objectKey,
    size: uploaded.size,
    url: uploaded.url,
  };
}

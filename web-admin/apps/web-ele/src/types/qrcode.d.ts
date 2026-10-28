/**
 * qrcode 没有随包提供类型，这里补一份最小声明（仅用到 toDataURL/toCanvas）
 */
declare module 'qrcode' {
  interface QRCodeToDataURLOptions {
    color?: { dark?: string; light?: string };
    errorCorrectionLevel?: 'H' | 'L' | 'M' | 'Q';
    margin?: number;
    width?: number;
  }

  interface QRCodeRenderersOptions extends QRCodeToDataURLOptions {}

  const QRCode: {
    toCanvas(
      canvas: HTMLCanvasElement,
      text: string,
      options?: QRCodeRenderersOptions,
    ): Promise<void>;
    toDataURL(text: string, options?: QRCodeToDataURLOptions): Promise<string>;
    toString(text: string, options?: QRCodeRenderersOptions): Promise<string>;
  };

  export default QRCode;
}

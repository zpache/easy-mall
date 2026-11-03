<script setup lang="ts">
import { getCurrentInstance, ref, watch } from 'vue'
import { resolveMediaUrl } from '@/utils/url'
import { SHOP_NAME } from '@/utils/share'

/**
 * 分享海报弹层：canvas 绘制商品图 + 价格 + 标题 + 小程序码，可保存到相册。
 * 仅微信小程序端可用（H5 无保存相册能力，入口处已拦截）。
 */
const props = defineProps<{
  visible: boolean
  /** 商品名 */
  title: string
  /** 已格式化的价格文案，如 ¥99.0 */
  priceText: string
  /** 商品图（绝对地址） */
  image: string
  /** 小程序码图片地址（后端 /files/...） */
  qrcodeUrl: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const instance = getCurrentInstance()

const POSTER_W = 600
const POSTER_H = 960
/** 价格色与全局 $price 一致 */
const PRICE_COLOR = '#ff5a1f'

const generating = ref(false)
const drawing = ref(false)

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      // #ifdef MP-WEIXIN
      generate()
      // #endif
    }
  }
)

function close() {
  emit('update:visible', false)
}

/* ---------------- 绘制 ---------------- */

function getCanvasNode(): Promise<any> {
  return new Promise((resolve, reject) => {
    uni
      .createSelectorQuery()
      .in(instance)
      .select('#posterCanvas')
      .fields({ node: true, size: true } as any, () => undefined)
      .exec((res) => {
        const node = res?.[0]?.node
        if (node) {
          resolve(node)
        } else {
          reject(new Error('海报画布未就绪，请重试'))
        }
      })
  })
}

/** canvas.createImage 加载网络图，失败时返回 null（标题/价格照常绘制） */
function loadImage(canvas: any, src: string): Promise<any | null> {
  return new Promise((resolve) => {
    if (!src) {
      resolve(null)
      return
    }
    const img = canvas.createImage()
    img.onload = () => resolve(img)
    img.onerror = () => resolve(null)
    img.src = src
  })
}

function roundRect(ctx: any, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

/** 标题按宽度断行，最多 maxLines 行，超出加省略号 */
function wrapText(ctx: any, text: string, maxWidth: number, maxLines: number): string[] {
  const lines: string[] = []
  let current = ''
  for (const char of text) {
    if (ctx.measureText(current + char).width > maxWidth) {
      lines.push(current)
      current = char
      if (lines.length === maxLines) {
        lines[maxLines - 1] = lines[maxLines - 1].slice(0, -1) + '…'
        return lines
      }
    } else {
      current += char
    }
  }
  if (current) lines.push(current)
  return lines
}

async function generate() {
  if (generating.value) return
  generating.value = true
  try {
    const canvas = await getCanvasNode()
    const dpr = Math.min(uni.getSystemInfoSync().pixelRatio || 2, 2)
    canvas.width = POSTER_W * dpr
    canvas.height = POSTER_H * dpr
    const ctx = canvas.getContext('2d')
    ctx.setTransform(1, 0, 0, 1, 0, 0)
    ctx.scale(dpr, dpr)

    drawing.value = true
    const [goodsImg, qrcodeImg] = await Promise.all([
      loadImage(canvas, props.image),
      loadImage(canvas, resolveMediaUrl(props.qrcodeUrl))
    ])

    /* 背景 */
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, POSTER_W, POSTER_H)

    /* 商品图：520x520 圆角 */
    if (goodsImg) {
      ctx.save()
      roundRect(ctx, 40, 40, 520, 520, 16)
      ctx.clip()
      ctx.drawImage(goodsImg, 40, 40, 520, 520)
      ctx.restore()
    } else {
      ctx.fillStyle = '#f2f2f2'
      ctx.fillRect(40, 40, 520, 520)
    }

    /* 价格 */
    ctx.fillStyle = PRICE_COLOR
    ctx.font = 'bold 60px sans-serif'
    ctx.textBaseline = 'top'
    ctx.fillText(props.priceText || '', 40, 596)

    /* 标题：最多两行（右侧给小程序码留位） */
    ctx.fillStyle = '#1f1f1f'
    ctx.font = '600 34px sans-serif'
    const titleLines = wrapText(ctx, props.title || '', POSTER_W - 80 - 200, 2)
    titleLines.forEach((line, index) => {
      ctx.fillText(line, 40, 690 + index * 48)
    })

    /* 店铺名 */
    ctx.fillStyle = '#8c8c8c'
    ctx.font = '26px sans-serif'
    ctx.fillText(SHOP_NAME, 40, POSTER_H - 66)

    /* 小程序码 */
    const qrSize = 180
    const qrX = POSTER_W - 40 - qrSize
    const qrY = POSTER_H - 40 - qrSize
    if (qrcodeImg) {
      ctx.drawImage(qrcodeImg, qrX, qrY, qrSize, qrSize)
    } else {
      ctx.fillStyle = '#f2f2f2'
      ctx.fillRect(qrX, qrY, qrSize, qrSize)
    }
    ctx.fillStyle = '#8c8c8c'
    ctx.font = '22px sans-serif'
    ctx.textAlign = 'right'
    ctx.fillText('长按识别小程序码', qrX + qrSize, qrY - 10)
    ctx.textAlign = 'left'
  } catch (e: any) {
    uni.showToast({ title: e?.message || '海报生成失败', icon: 'none' })
  } finally {
    drawing.value = false
    generating.value = false
  }
}

/* ---------------- 保存 ---------------- */

function savePoster() {
  // #ifdef MP-WEIXIN
  if (drawing.value || generating.value) return
  uni
    .createSelectorQuery()
    .in(instance)
    .select('#posterCanvas')
    .fields({ node: true } as any, () => undefined)
    .exec((res) => {
      const canvas = res?.[0]?.node
      if (!canvas) return
      uni.canvasToTempFilePath({
        canvas,
        success: (result) => {
          uni.saveImageToPhotosAlbum({
            filePath: result.tempFilePath,
            success: () => uni.showToast({ title: '已保存到相册', icon: 'success' }),
            fail: (err) => {
              const message = String(err?.errMsg || '')
              if (message.includes('auth') || message.includes('authorize')) {
                uni.showModal({
                  title: '提示',
                  content: '需要相册权限才能保存海报，请在设置中开启',
                  success: (r) => {
                    if (r.confirm) uni.openSetting({})
                  }
                })
              } else if (!message.includes('cancel')) {
                uni.showToast({ title: '保存失败，请重试', icon: 'none' })
              }
            }
          })
        },
        fail: () => uni.showToast({ title: '海报导出失败，请重试', icon: 'none' })
      })
    })
  // #endif
}
</script>

<template>
  <view v-if="visible" class="poster-mask" @tap="close">
    <view class="poster" @tap.stop>
      <view class="poster__title">分享海报</view>
      <!-- #ifdef MP-WEIXIN -->
      <canvas id="posterCanvas" type="2d" class="poster__canvas" />
      <view class="poster__actions">
        <button class="poster__btn" hover-class="poster__btn--hover" @tap="close">取消</button>
        <button
          class="poster__btn poster__btn--primary"
          :disabled="drawing || generating"
          hover-class="poster__btn--hover"
          @tap="savePoster"
        >
          {{ drawing || generating ? '生成中…' : '保存到相册' }}
        </button>
      </view>
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <text class="poster__tip">海报功能请在微信小程序中使用</text>
      <!-- #endif -->
    </view>
  </view>
</template>

<style scoped lang="scss">
.poster-mask {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.6);
}

.poster {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32rpx;
  background-color: transparent;

  &__title {
    margin-bottom: 20rpx;
    color: #fff;
    font-size: 30rpx;
  }

  /* 逻辑 600x960，屏上按 0.5 倍显示 */
  &__canvas {
    width: 300px;
    height: 480px;
    background-color: #fff;
    border-radius: 16rpx;
  }

  &__actions {
    display: flex;
    gap: 24rpx;
    margin-top: 28rpx;
    width: 300px;
  }

  &__btn {
    flex: 1;
    height: 80rpx;
    padding: 0;
    font-size: 28rpx;
    line-height: 80rpx;
    color: #fff;
    background-color: rgba(255, 255, 255, 0.2);
    border: none;
    border-radius: 40rpx;

    &::after {
      border: none;
    }

    &--primary {
      color: #fff;
      background-color: $price;
    }

    &--hover {
      opacity: 0.8;
    }

    &[disabled] {
      color: #fff;
      background-color: $price;
      opacity: 0.6;
    }
  }

  &__tip {
    margin-top: 24rpx;
    color: rgba(255, 255, 255, 0.8);
    font-size: 26rpx;
  }
}
</style>

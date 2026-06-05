import { ref, type Ref } from 'vue'

export function useImageZoom(imageRef: Ref<HTMLElement | null>) {
  const showing = ref(false)
  const lensX = ref(0)
  const lensY = ref(0)
  const zoomBgX = ref(0)
  const zoomBgY = ref(0)

  const LENS_SIZE = 120
  const ZOOM_FACTOR = 2

  function onEnter() {
    showing.value = true
  }

  function onLeave() {
    showing.value = false
  }

  function onMove(e: MouseEvent) {
    const el = imageRef.value
    if (!el) return
    const rect = el.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top

    lensX.value = Math.max(0, Math.min(x - LENS_SIZE / 2, rect.width - LENS_SIZE))
    lensY.value = Math.max(0, Math.min(y - LENS_SIZE / 2, rect.height - LENS_SIZE))

    zoomBgX.value = (x / rect.width) * 100
    zoomBgY.value = (y / rect.height) * 100
  }

  return { showing, lensX, lensY, zoomBgX, zoomBgY, LENS_SIZE, ZOOM_FACTOR, onEnter, onLeave, onMove }
}

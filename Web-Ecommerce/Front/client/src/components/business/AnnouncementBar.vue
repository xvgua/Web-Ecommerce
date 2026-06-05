<template>
  <div v-if="visible && announcements.length" class="announcement-bar" :class="`announcement-bar--${levelClass}`">
    <div class="announcement-bar__inner" @mouseenter="pauseCarousel" @mouseleave="resumeCarousel">
      <span class="announcement-bar__icon">
        <el-icon><Bell /></el-icon>
      </span>
      <span class="announcement-bar__label">公告</span>
      <div class="announcement-bar__carousel" @click="handleClick(current)">
        <Transition :name="slideDirection" mode="out-in">
          <span :key="current?.id" class="announcement-bar__title">
            {{ current?.title }}
          </span>
        </Transition>
      </div>
      <router-link to="/announcements" class="announcement-bar__more">更多</router-link>
      <span class="announcement-bar__close" @click="handleCloseCurrent">
        <el-icon><Close /></el-icon>
      </span>
    </div>

    <el-dialog v-model="dialogVisible" :title="selected?.title" width="560px" center>
      <div class="announcement-detail">
        <div class="announcement-detail__meta">
          {{ selected?.createTime }}
        </div>
        <div class="announcement-detail__content">{{ selected?.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Bell, Close } from '@element-plus/icons-vue'
import { getAnnouncements } from '@/api/announcement'
import type { Announcement } from '@shared/types'

const DISMISSED_KEY = 'announcement_dismissed_ids'
const INTERVAL = 4000

const announcements = ref<Announcement[]>([])
const index = ref(0)
const dialogVisible = ref(false)
const selected = ref<Announcement | null>(null)
const slideDirection = ref('slide-left')
let timer: ReturnType<typeof setInterval> | null = null
let lastIndex = 0

const visible = ref(true)

const current = computed(() => announcements.value[index.value] || null)

const levelClass = computed(() => {
  if (!current.value) return 'info'
  return current.value.level || 'info'
})

function dismissedIds(): number[] {
  try {
    return JSON.parse(localStorage.getItem(DISMISSED_KEY) || '[]')
  } catch {
    return []
  }
}

function handleCloseCurrent() {
  if (!current.value) return
  const existing = dismissedIds()
  existing.push(current.value.id)
  const merged = [...new Set(existing)]
  localStorage.setItem(DISMISSED_KEY, JSON.stringify(merged))

  // Remove current from list; if none left, hide bar
  announcements.value = announcements.value.filter(a => a.id !== current.value!.id)
  if (!announcements.value.length) {
    visible.value = false
    return
  }
  if (index.value >= announcements.value.length) {
    index.value = 0
  }
  advanceCarousel()
}

function handleClick(item: Announcement | null) {
  if (!item) return
  selected.value = item
  dialogVisible.value = true
}

function advanceCarousel() {
  if (announcements.value.length <= 1) return
  lastIndex = index.value
  slideDirection.value = 'slide-left'
  index.value = (index.value + 1) % announcements.value.length
}

function startCarousel() {
  if (timer) clearInterval(timer)
  if (announcements.value.length <= 1) return
  timer = setInterval(advanceCarousel, INTERVAL)
}

function pauseCarousel() {
  if (timer) clearInterval(timer)
}

function resumeCarousel() {
  if (announcements.value.length <= 1) return
  // Restart with a short delay so the user has time to click
  timer = setInterval(advanceCarousel, INTERVAL)
}

onMounted(async () => {
  try {
    const res = await getAnnouncements(5)
    const dismissed = dismissedIds()
    announcements.value = (res.data || []).filter(a => !dismissed.includes(a.id))
    if (announcements.value.length) {
      startCarousel()
    }
  } catch {
    visible.value = false
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
/* ── Base bar ── */
.announcement-bar {
  margin: 0 0 20px;

  &__inner {
    display: flex;
    align-items: center;
    height: 42px;
    border-radius: 8px;
    padding: 0 16px;
    gap: 10px;
    border: 1px solid;
  }

  &__icon {
    font-size: 18px;
    display: flex;
    align-items: center;
    flex-shrink: 0;
  }

  &__label {
    font-size: 13px;
    font-weight: 600;
    flex-shrink: 0;
    padding-right: 10px;
    border-right: 1px solid;
  }

  &__carousel {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    cursor: pointer;
    position: relative;
    height: 22px;
    display: flex;
    align-items: center;
  }

  &__title {
    display: block;
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 22px;
    width: 100%;
  }

  &__more {
    flex-shrink: 0;
    font-size: 12px;
    text-decoration: none;
    padding: 2px 8px;
    border-radius: 4px;
    transition: all .15s;
  }

  &__close {
    flex-shrink: 0;
    cursor: pointer;
    font-size: 14px;
    display: flex;
    align-items: center;
    padding: 4px;
    border-radius: 4px;
    transition: all .15s;
  }
}

/* ── Level: info (mint sage) ── */
.announcement-bar--info {
  .announcement-bar__inner {
    background: linear-gradient(90deg, #EEF7F3 0%, #E6F3ED 100%);
    border-color: #C4DDD3;
  }
  .announcement-bar__icon { color: var(--brand-primary, #4EAB8E); }
  .announcement-bar__label { color: var(--brand-primary, #4EAB8E); border-right-color: #C4DDD3; }
  .announcement-bar__title { color: #2A5C4E; }
  .announcement-bar__close {
    color: #7AACA4;
    &:hover { color: var(--brand-primary, #4EAB8E); background: rgba(78, 171, 142, .08); }
  }
  .announcement-bar__more {
    color: #7AACA4;
    &:hover { color: var(--brand-primary, #4EAB8E); background: rgba(78, 171, 142, .08); }
  }
}

/* ── Level: warning (amber) ── */
.announcement-bar--warning {
  .announcement-bar__inner {
    background: linear-gradient(90deg, #fdf6ec 0%, #fcf0e0 100%);
    border-color: #e8c888;
  }
  .announcement-bar__icon { color: #c08030; }
  .announcement-bar__label { color: #c08030; border-right-color: #e8c888; }
  .announcement-bar__title { color: #5d4030; }
  .announcement-bar__close {
    color: #c8a878;
    &:hover { color: #c08030; background: rgba(192, 128, 48, .08); }
  }
  .announcement-bar__more {
    color: #c8a878;
    &:hover { color: #c08030; background: rgba(192, 128, 48, .08); }
  }
}

/* ── Level: important (red) ── */
.announcement-bar--important {
  .announcement-bar__inner {
    background: linear-gradient(90deg, #fef0f0 0%, #fde8e8 100%);
    border-color: #f0b0b0;
  }
  .announcement-bar__icon { color: #d9534f; }
  .announcement-bar__label { color: #d9534f; border-right-color: #f0b0b0; }
  .announcement-bar__title { color: #6b3030; }
  .announcement-bar__close {
    color: #e8a8a8;
    &:hover { color: #d9534f; background: rgba(217, 83, 79, .08); }
  }
  .announcement-bar__more {
    color: #e8a8a8;
    &:hover { color: #d9534f; background: rgba(217, 83, 79, .08); }
  }
}

/* ── Slide transition ── */
.slide-left-enter-active,
.slide-left-leave-active {
  transition: all .3s ease;
}
.slide-left-enter-from {
  transform: translateX(40px);
  opacity: 0;
}
.slide-left-leave-to {
  transform: translateX(-40px);
  opacity: 0;
}

/* ── Detail dialog ── */
.announcement-detail {
  &__meta {
    font-size: 12px;
    color: #909399;
    margin-bottom: 16px;
  }

  &__content {
    font-size: 14px;
    color: #303133;
    line-height: 1.8;
    white-space: pre-wrap;
  }
}
</style>

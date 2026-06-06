<template>
  <div class="logistics-tracker">
    <h2 class="logistics-tracker__title">物流信息</h2>
    <div class="logistics-tracker__timeline">
      <div
        v-for="(step, i) in steps"
        :key="i"
        class="timeline-step"
        :class="{
          'timeline-step--done': step.done,
          'timeline-step--current': step.current,
          'timeline-step--pending': !step.done && !step.current,
        }"
      >
        <div class="timeline-step__dot">
          <span v-if="step.done || step.current" class="timeline-step__dot-inner" />
        </div>
        <div class="timeline-step__content">
          <div class="timeline-step__title">{{ step.title }}</div>
          <div class="timeline-step__desc" v-if="step.desc">{{ step.desc }}</div>
          <div class="timeline-step__time">{{ step.time }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatDate } from '@/utils/format'

interface TimelineStep {
  title: string
  desc?: string
  time: string
  done: boolean
  current: boolean
}

const props = defineProps<{
  status: number
  payTime?: string
  dealTime?: string
}>()

const steps = computed<TimelineStep[]>(() => {
  const payTime = props.payTime ? formatDate(props.payTime) : '—'
  const dealTime = props.dealTime ? formatDate(props.dealTime) : '—'

  const allSteps: TimelineStep[] = [
    { title: '已下单', desc: '订单已提交', time: payTime, done: true, current: false },
    { title: '已支付', desc: '买家已付款', time: payTime, done: false, current: false },
    { title: '拣货中', desc: '仓库正在拣货', time: '', done: false, current: false },
    { title: '已发货', desc: '商品已出库，等待揽收', time: '', done: false, current: false },
    { title: '运输中', desc: '快递正在途中', time: '', done: false, current: false },
    { title: '派送中', desc: '快递员正在派送', time: '', done: false, current: false },
    { title: '已签收', desc: '买家已签收', time: dealTime, done: false, current: false },
  ]

  // status: 0=待支付, 1=待发货(已支付), 2=待收货(已发货), 3=已完成
  if (props.status === 1) {
    // Paid but not shipped
    allSteps[0].done = true
    allSteps[1].done = true
    allSteps[1].current = true
  } else if (props.status === 2) {
    // Shipped
    allSteps[0].done = true
    allSteps[1].done = true
    allSteps[2].done = true
    allSteps[3].done = true
    allSteps[4].current = true
    allSteps[4].desc = '快递正在途中'
  } else if (props.status === 3) {
    // Completed — all steps done
    allSteps.forEach(s => { s.done = true })
    allSteps[6].desc = '买家已签收'
  }

  return allSteps
})
</script>

<style lang="scss" scoped>
.logistics-tracker {
  background: var(--bg1);
  border-radius: var(--radius-sm);
  border: 1px solid var(--line-light);
  padding: 24px 28px;

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text1);
    margin-bottom: 20px;
  }

  &__timeline {
    display: flex;
    flex-direction: column;
  }
}

.timeline-step {
  display: flex;
  gap: 14px;
  position: relative;
  min-height: 52px;

  &:not(:last-child) {
    padding-bottom: 4px;

    .timeline-step__dot::after {
      content: '';
      position: absolute;
      left: 8px;
      top: 16px;
      width: 2px;
      height: calc(100% + 4px);
      background: var(--line-regular);
    }
  }

  &--done {
    .timeline-step__dot {
      background: var(--color-success);
      border-color: var(--color-success);
    }

    .timeline-step__dot::after {
      background: var(--color-success);
    }
  }

  &--current {
    .timeline-step__dot {
      background: transparent;
      border-color: var(--color-success);

      &-inner {
        display: block;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: var(--color-success);
      }
    }

    .timeline-step__title {
      color: var(--color-success);
      font-weight: 700;
    }
  }

  &--pending {
    .timeline-step__dot {
      background: transparent;
      border-color: var(--line-regular);
    }

    .timeline-step__title {
      color: var(--text3);
    }

    .timeline-step__time {
      opacity: 0;
    }
  }

  &__dot {
    flex-shrink: 0;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    border: 2px solid var(--line-regular);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    z-index: 1;
    margin-top: 2px;
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text1);
    margin-bottom: 2px;
  }

  &__desc {
    font-size: 12px;
    color: var(--text2);
    margin-bottom: 2px;
  }

  &__time {
    font-size: 12px;
    color: var(--text4);
  }
}
</style>

<template>
  <div class="coupon-form-page">
    <div class="page-header">
      <h1 class="page-title">{{ isEdit ? '编辑优惠券' : '新增优惠券' }}</h1>
    </div>

    <el-card shadow="never">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="coupon-form">
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入优惠券名称" maxlength="50" />
        </el-form-item>

        <el-form-item label="优惠类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button :value="1">满减券</el-radio-button>
            <el-radio-button :value="2">折扣券</el-radio-button>
            <el-radio-button :value="3">免邮券</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item v-if="form.type === 1" label="优惠金额" prop="discount">
              <el-input-number v-model="form.discount" :min="0" :precision="2" controls-position="right" />
              <span class="unit-text">元</span>
            </el-form-item>
            <el-form-item v-if="form.type === 2" label="折扣率" prop="discount">
              <el-input-number v-model="form.discount" :min="0" :max="1" :precision="2" :step="0.05" controls-position="right" />
              <span class="unit-text">（0.95 = 9.5折）</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="使用门槛">
              <el-input-number v-model="form.minAmount" :min="0" :precision="2" controls-position="right" />
              <span class="unit-text">元（0=无门槛）</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发行总量" prop="totalQty">
              <el-input-number v-model="form.totalQty" :min="1" controls-position="right" />
              <span class="unit-text">张</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="有效期" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 220px"
          />
          <span style="margin: 0 12px">至</span>
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 220px"
          />
        </el-form-item>

        <el-divider content-position="left">抢购设置（大额券）</el-divider>

        <el-form-item label="启用抢购">
          <el-switch v-model="isLarge" />
          <span class="unit-text" style="margin-left: 8px">开启后标记为大额券，需在指定时间段内抢购</span>
        </el-form-item>
        <el-form-item v-if="isLarge" label="抢购时间">
          <el-date-picker
            v-model="grabStartTime"
            type="datetime"
            placeholder="开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 220px"
          />
          <span style="margin: 0 12px">至</span>
          <el-date-picker
            v-model="grabEndTime"
            type="datetime"
            placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 220px"
          />
        </el-form-item>

        <el-divider content-position="left">适用范围</el-divider>

        <el-form-item label="适用范围">
          <el-radio-group v-model="form.scopeType">
            <el-radio :value="1">通用</el-radio>
            <el-radio :value="2">指定分类</el-radio>
            <el-radio :value="3">指定商品</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scopeType !== 1" label="范围ID">
          <el-input v-model="form.scopeIds" placeholder="JSON数组格式，如 [1,6,7]" />
        </el-form-item>

        <el-divider content-position="left">状态</el-divider>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">保存</el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createCoupon, updateCoupon, getCouponById } from '@/api/admin'
import { requiredRule } from '@shared/validators'
import type { CouponForm } from '@shared/types/coupon'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const isEdit = computed(() => !!route.params.id)
const submitting = ref(false)

const form = reactive<CouponForm>({
  name: '',
  type: 1,
  discount: 0,
  minAmount: 0,
  totalQty: 100,
  startTime: '',
  endTime: '',
  grabStartTime: '',
  grabEndTime: '',
  scopeType: 1,
  scopeIds: '',
  isLarge: 0,
  status: 1,
})

const isLarge = ref(false)
const grabStartTime = ref('')
const grabEndTime = ref('')

const rules: FormRules = {
  name: [requiredRule('券名称')],
  type: [requiredRule('优惠类型')],
  discount: [requiredRule('优惠值')],
  totalQty: [requiredRule('发行总量')],
  startTime: [requiredRule('开始时间')],
}

watch(isLarge, (val) => {
  form.isLarge = val ? 1 : 0
  if (!val) {
    grabStartTime.value = ''
    grabEndTime.value = ''
  }
})

watch([grabStartTime, grabEndTime], () => {
  form.grabStartTime = grabStartTime.value
  form.grabEndTime = grabEndTime.value
})

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCoupon(Number(route.params.id), { ...form })
      ElMessage.success('优惠券已更新')
    } else {
      await createCoupon({ ...form })
      ElMessage.success('优惠券已创建')
    }
    router.push('/coupons')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (isEdit.value) {
    const res = await getCouponById(Number(route.params.id))
    const c = res.data
    form.name = c.name
    form.type = c.type
    form.discount = c.discount
    form.minAmount = c.minAmount
    form.totalQty = c.totalQty
    form.startTime = c.startTime || ''
    form.endTime = c.endTime || ''
    form.scopeType = c.scopeType || 1
    form.scopeIds = c.scopeIds || ''
    form.status = c.status
    if (c.isLarge) {
      isLarge.value = true
      grabStartTime.value = c.grabStartTime || ''
      grabEndTime.value = c.grabEndTime || ''
    }
  }
})
</script>

<style lang="scss" scoped>
.coupon-form-page {
  max-width: 800px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
}

.coupon-form {
  :deep(.el-input-number) { width: 160px; }
}

.unit-text {
  margin-left: 8px;
  font-size: 12px;
  color: #999;
}
</style>

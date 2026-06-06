<template>
  <div class="blank-layout">
    <router-view />
  </div>
</template>

<style lang="scss" scoped>
.blank-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg2);

  @media (max-width: 768px) {
    align-items: flex-start;
    padding-top: 40px;
  }
}
</style>

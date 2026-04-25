<template>
  <router-view v-slot="{ Component }">
   <transition name="mri-blur" mode="out-in">
  <component :is="Component" />
</transition>
  </router-view>
</template>

<style>
/* ==================================================
   Radiology Dark - 根容器终极修正
   ================================================== 
*/
html, body, #app {
  height: 100%;
  width: 100%;
  margin: 0;
  padding: 0;
  background-color: var(--medical-deep-bg, #0B0E14) !important; /* 统一底色为极暗深渊色 */
  color: var(--medical-text, #F0F6FC);
  font-family: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  overflow: hidden; 
}

.el-overlay {
  background-color: rgba(0, 0, 0, 0.7) !important;
  backdrop-filter: blur(4px);
}

/* ==================================================
   特效 B: 核磁模糊扫描 (MRI-Blur)
   ================================================== 
*/
.mri-blur-enter-active,
.mri-blur-leave-active {
  transition: all 0.35s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.mri-blur-enter-from {
  opacity: 0;
  transform: translateY(20px);
  filter: blur(10px); /* 初始状态带有严重的医疗光学模糊 */
}

.mri-blur-leave-to {
  opacity: 0;
  transform: translateY(-20px);
  filter: blur(10px); /* 离开时迅速模糊消散 */
}
</style>
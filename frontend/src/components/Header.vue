<script setup>
import { ref } from 'vue'
import profileIcon from '@/assets/profile-icon.svg'
import logoImage from '@/assets/MECHU.svg'

const activeTab = ref('내 근처 추천')

const tabs = [
  { id: 'ai', label: 'AI 추천' },
  { id: 'near', label: '내 근처 추천' },
  { id: 'favorites', label: '즐겨찾기' }
]

const setActiveTab = (tabId) => {
  activeTab.value = tabs.find(tab => tab.id === tabId)?.label || '내 근처 추천'
}
</script>

<template>
  <header class="header">
    <div class="header-container">
      <div class="header-left">
        <div class="logo">
          <img :src="logoImage" alt="MECHU" class="logo-image" />
        </div>
        <nav class="tabs">
          <div
            v-for="tab in tabs"
            :key="tab.id"
            class="tab"
            :class="{ active: activeTab === tab.label }"
            @click="setActiveTab(tab.id)"
          >
            <div class="tab-content">
              <p
                class="tab-text"
                :class="{ 'tab-text-active': activeTab === tab.label }"
              >
                {{ tab.label }}
              </p>
            </div>
          </div>
        </nav>
      </div>
      <div class="header-right">
        <button class="user-button">
          <p class="user-button-text">알림</p>
        </button>
        <button class="user-button">
          <img :src="profileIcon" alt="프로필" class="profile-icon" />
          <p class="user-button-text">마이</p>
        </button>
        <button class="user-button">
          <p class="user-button-text">로그아웃</p>
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  height: 80px;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
  padding: 0 380px;
  width: 100%;
  background-color: white;
}

.header-container {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: space-between;
  width: 1200px;
}

.header-left {
  display: flex;
  gap: 32px;
  height: 100%;
  align-items: center;
}

.logo {
  height: 35px;
  width: 92px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.logo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.tabs {
  display: flex;
  gap: 24px;
  height: 100%;
  align-items: center;
}

.tab {
  display: flex;
  height: 56px;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  border-radius: 12px;
  flex-shrink: 0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.tab:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.tab-content {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.tab.active .tab-content {
  border-bottom: 2px solid #ff5531;
}

.tab-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 22px;
  line-height: 1.35;
  color: #5f6368;
  white-space: pre;
  margin: 0;
}

.tab-text-active {
  font-weight: 700;
  color: #ff5531;
}

.header-right {
  display: flex;
  gap: 20px;
  height: 100%;
  align-items: center;
}

.user-button {
  display: flex;
  gap: 8px;
  height: 48px;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  border-radius: 14px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-button:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.profile-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: block;
}

.user-button-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  line-height: 1.35;
  color: #5f6368;
  white-space: pre;
  margin: 0;
}
</style>

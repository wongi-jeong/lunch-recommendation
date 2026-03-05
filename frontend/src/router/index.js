import { createRouter, createWebHistory } from 'vue-router'
import MainPage from '@/components/MainPage.vue'
import NearbyRecommendation from '@/components/NearbyRecommendation.vue'
import AiRecommendation from '@/components/AiRecommendation.vue'
import SignUpPage from '@/components/SignUpPage.vue'
import LoginPage from '@/components/LoginPage.vue'
import RouletteSharePage from '@/components/RouletteSharePage.vue'
import VoteCreatePage from '@/components/VoteCreatePage.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: MainPage,
    meta: { title: 'MECHU - 디두개' }
  },
  {
    path: '/nearby',
    name: 'nearby',
    component: NearbyRecommendation,
    meta: { title: '내 근처 추천 - MECHU' }
  },
  {
    path: '/ai',
    name: 'ai',
    component: AiRecommendation,
    meta: { title: 'AI 추천 - MECHU' }
  },
  {
    path: '/login',
    name: 'login',
    component: LoginPage,
    meta: { title: '로그인 - MECHU' }
  },
  {
    path: '/signup',
    name: 'signup',
    component: SignUpPage,
    meta: { title: '회원가입 - MECHU' }
  },
  {
    path: '/roulette/share',
    name: 'rouletteShare',
    component: RouletteSharePage,
    meta: { title: '룰렛 결과 공유 - MECHU' }
  },
  {
    path: '/vote/create',
    name: 'voteCreate',
    component: VoteCreatePage,
    meta: { title: '투표 생성 - MECHU' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = to.meta.title || 'MECHU - 디두개'
})

export default router

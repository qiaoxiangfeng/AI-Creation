import { createRouter, createWebHistory } from 'vue-router';
import type { RouteLocationNormalized } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import Layout from '@/views/Layout.vue';
import Login from '@/views/Login.vue';
import Users from '@/views/Users.vue';
import Articles from '@/views/Articles.vue';
import ArticleDetail from '@/views/ArticleDetail.vue';
import ArticleGenerationConfigs from '@/views/ArticleGenerationConfigs.vue';
import Dictionaries from '@/views/Dictionaries.vue';
import Dashboard from '@/views/Dashboard.vue';
import MembershipPricingAdmin from '@/views/MembershipPricingAdmin.vue';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: Layout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: { name: 'dashboard' }
      },
      // 子路由必须用相对路径；以 / 开头会被当作根路径，不挂在 Layout 下，meta 合并异常 → 管理员路由/侧栏判断失效
      {
        path: 'users',
        name: 'users',
        component: Users,
        meta: { requiresAdmin: true }
      },
      {
        path: 'membership-pricing',
        name: 'membership-pricing',
        component: MembershipPricingAdmin,
        meta: { requiresAdmin: true }
      },
      {
        path: 'articles',
        name: 'articles',
        component: Articles
      },
      {
        path: 'articles/:id',
        name: 'article-detail',
        component: ArticleDetail
      },
      {
        path: 'article-generation-configs',
        name: 'article-generation-configs',
        component: ArticleGenerationConfigs
      },
      {
        path: 'dictionaries',
        name: 'dictionaries',
        component: Dictionaries
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: Dashboard
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

function routeRequiresAuth(to: RouteLocationNormalized) {
  return to.matched.some((r) => r.meta.requiresAuth === true);
}

function routeRequiresAdmin(to: RouteLocationNormalized) {
  return to.matched.some((r) => r.meta.requiresAdmin === true);
}

router.beforeEach(async (to) => {
  const auth = useAuthStore();

  // 登录页始终可见，避免守卫重定向导致登录页不可见
  if (to.name === 'login' || to.path === '/login') {
    return true;
  }

  if (routeRequiresAuth(to) && !auth.initialized) {
    await auth.initializeAuth();
  }
  if (routeRequiresAuth(to) && !auth.isAuthenticated) {
    return '/login';
  }
  if (routeRequiresAdmin(to) && !auth.isAdmin) {
    return '/dashboard';
  }
  return true;
});

export default router;



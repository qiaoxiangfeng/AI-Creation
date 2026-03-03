import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import Layout from '@/views/Layout.vue';
import Login from '@/views/Login.vue';
import Users from '@/views/Users.vue';
import Articles from '@/views/Articles.vue';
import ArticleDetail from '@/views/ArticleDetail.vue';
import ArticleTypes from '@/views/ArticleTypes.vue';
import ArticleGenerationConfigs from '@/views/ArticleGenerationConfigs.vue';
import Dictionaries from '@/views/Dictionaries.vue';

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
        redirect: '/users'
      },
      {
        path: '/users',
        name: 'users',
        component: Users
      },
      {
        path: '/articles',
        name: 'articles',
        component: Articles
      },
      {
        path: '/articles/:id',
        name: 'article-detail',
        component: ArticleDetail
      },
      {
        path: '/article-types',
        name: 'article-types',
        component: ArticleTypes
      },
      {
        path: '/article-generation-configs',
        name: 'article-generation-configs',
        component: ArticleGenerationConfigs
      },
      {
        path: '/dictionaries',
        name: 'dictionaries',
        component: Dictionaries
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const auth = useAuthStore();
  
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    next('/login');
  } else if (to.path === '/login' && auth.isAuthenticated) {
    next('/');
  } else {
    next();
  }
});

export default router;



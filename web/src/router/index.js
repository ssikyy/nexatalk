import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 懒加载各页面组件（减少首屏包体积）
const LoginPage = () => import('@/views/auth/LoginPage.vue')
const RegisterPage = () => import('@/views/auth/RegisterPage.vue')
const MainLayout = () => import('@/layouts/MainLayout.vue')
const AdminPage = () => import('@/views/admin/AdminPage.vue')
const CreateNotificationPage = () => import('@/views/admin/CreateNotificationPage.vue')
const HomePage = () => import('@/views/home/HomePage.vue')
const PostDetailPage = () => import('@/views/post/PostDetailPage.vue')
const CreatePostPage = () => import('@/views/post/CreatePostPage.vue')
const EditPostPage = () => import('@/views/post/EditPostPage.vue')
const ProfilePage = () => import('@/views/user/ProfilePage.vue')
const UserListPage = () => import('@/views/user/UserListPage.vue')
const UserPostsPage = () => import('@/views/user/UserPostsPage.vue')
const SettingsPage = () => import('@/views/user/SettingsPage.vue')
const EditProfilePage = () => import('@/views/user/EditProfilePage.vue')
const NotificationsPage = () => import('@/views/notifications/NotificationsPage.vue')
const NotificationDetailPage = () => import('@/views/notifications/NotificationDetailPage.vue')
const MessagesPage = () => import('@/views/messages/MessagesPage.vue')
const SearchPage = () => import('@/views/search/SearchPage.vue')
const NotFoundPage = () => import('@/views/error/NotFoundPage.vue')

const routes = [
  // ---- 无需登录的公开页面 ----
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
    meta: { guest: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterPage,
    meta: { guest: true, title: '注册' }
  },

  // ---- 主站布局（社区）----
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: HomePage,
        meta: { title: '首页' }
      },
      {
        path: 'posts/:id',
        name: 'PostDetail',
        component: PostDetailPage,
        meta: { title: '帖子详情' }
      },
      {
        path: 'posts/create',
        name: 'CreatePost',
        component: CreatePostPage,
        meta: { requiresAuth: true, title: '发帖' }
      },
      {
        path: 'posts/:id/edit',
        name: 'EditPost',
        component: EditPostPage,
        meta: { requiresAuth: true, title: '编辑帖子' }
      },
      {
        path: 'user/:id',
        name: 'Profile',
        component: ProfilePage,
        meta: { title: '个人主页' }
      },
      {
        path: 'user/:id/following',
        name: 'UserFollowing',
        component: UserListPage,
        meta: { title: '关注' }
      },
      {
        path: 'user/:id/followers',
        name: 'UserFollowers',
        component: UserListPage,
        meta: { title: '粉丝' }
      },
      {
        path: 'user/:id/posts',
        name: 'UserPosts',
        component: UserPostsPage,
        meta: { title: '帖子' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: SettingsPage,
        meta: { requiresAuth: true, title: '设置' }
      },
      {
        path: 'edit-profile',
        name: 'EditProfile',
        component: EditProfilePage,
        meta: { requiresAuth: true, title: '编辑资料' }
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: NotificationsPage,
        meta: { requiresAuth: true, title: '通知' }
      },
      {
        path: 'notification/:id',
        name: 'NotificationDetail',
        component: NotificationDetailPage,
        meta: { requiresAuth: true, title: '通知详情' }
      },
      {
        path: 'messages',
        name: 'Messages',
        component: MessagesPage,
        meta: { requiresAuth: true, title: '私信' }
      },
      {
        path: 'search',
        name: 'Search',
        component: SearchPage,
        meta: { title: '搜索' }
      }
    ]
  },

  // ---- 管理后台（独立全屏布局）----
  {
    path: '/admin',
    name: 'Admin',
    component: AdminPage,
    meta: { requiresAuth: true, requiresAdmin: true, title: '管理后台' }
  },
  {
    path: '/admin/notification/create',
    name: 'CreateNotification',
    component: CreateNotificationPage,
    meta: { requiresAuth: true, requiresAdmin: true, title: '发布通知' }
  },
  {
    path: '/admin/notification/:id/edit',
    name: 'EditNotification',
    component: CreateNotificationPage,
    meta: { requiresAuth: true, requiresAdmin: true, title: '编辑通知' }
  },

  // ---- 404 兜底 ----
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFoundPage,
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 每次跳转页面滚动回顶部
  scrollBehavior: () => ({ top: 0 })
})

// 全局前置守卫：鉴权 + 修改页面标题
router.beforeEach((to, from, next) => {
  // 更新浏览器标签标题
  document.title = to.meta.title ? `${to.meta.title} - NexaTalk` : 'NexaTalk'

  const userStore = useUserStore()
  const isLoggedIn = userStore.isLoggedIn

  // 已登录用户不允许访问登录/注册页，跳到首页
  if (to.meta.guest && isLoggedIn) {
    return next({ name: 'Home' })
  }

  // 需要登录的页面：未登录则跳转到登录页，并记录来源路径方便登录后跳回
  if (to.meta.requiresAuth && !isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  // 仅管理员可访问的页面
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    ElMessage.error('仅管理员可访问该页面')
    return next({ name: 'Home' })
  }

  next()
})

export default router

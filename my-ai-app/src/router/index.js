import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue')
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/Register.vue')
    },
    {
        path: '/chat',
        component: () => import('../views/ChatLayout.vue'), // 父组件是带侧边栏的布局
        children: [
            // 【核心修改】: 拔除 ChatHome，直接在路由层进行虫洞跳跃
            { path: '', redirect: '/chat/0' },
            // 访问 /chat/123 时，显示ID为123的聊天页（ID为0时触发空状态核心舱）
            { path: ':id', name: 'ChatConversation', component: () => import('../views/Chat.vue'), props: true }
        ]
    },
    // 管理员模块路由
    {
        path: '/admin',
        component: () => import('../views/AdminLayout.vue'),
        children: [
            // ★ 修改这里：默认重定向到炫酷的大屏
            { path: '', redirect: '/admin/knowledge' },
            // ★ 新增这行：注册大屏组件
            { path: 'dashboard', name: 'SystemDashboard', component: () => import('../views/SystemDashboard.vue') },
            { path: 'knowledge', name: 'KnowledgeManagement', component: () => import('../views/KnowledgeManagement.vue') },
            { path: 'history', name: 'HistoryDashboard', component: () => import('../views/HistoryDashboard.vue') },
            // 👇 新增这一行：诊疗报告生成工作站
            { path: 'reports', name: 'MedicalReports', component: () => import('../views/MedicalReports.vue') }
        ]
    }
]

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

export default router
import axios from 'axios'

const apiClient = axios.create({
    baseURL: 'http://localhost:8888/api',
    timeout: 60000,
});

apiClient.interceptors.request.use(config => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, error => {
    return Promise.reject(error);
});

export default {
    login(username, password) {
        return apiClient.post('/user/login', { username, password });
    },
    register(username, password) {
        return apiClient.post('/user/register', { username, password });
    },
    getAllKnowledge() {
        return apiClient.get('/knowledge');
    },
    // 【修改】
    addKnowledge(knowledge) {
        return apiClient.post('/knowledge/add', knowledge);
    },
    deleteKnowledge(id) {
        return apiClient.delete(`/knowledge/${id}`);
    },
    // 【修改】
    updateKnowledge(knowledge) {
        return apiClient.put('/knowledge', knowledge);
    },

    /**
     * 【修改】: 获取所有历史记录，支持筛选
     * @param {object} params {userId, startDate, endDate}
     */
    getAllHistory(params) {
        return apiClient.get('/admin/history', { params });
    },

    // 【新增】: 会话管理API
    getConversations() {
        return apiClient.get('/chat/conversations');
    },
    getHistory(conversationId) {
        return apiClient.get(`/chat/history/${conversationId}`);
    },
    deleteConversation(conversationId) {
        return apiClient.delete(`/chat/conversation/${conversationId}`);
    },

    // 【修改】sendMessage现在需要conversationId
    sendMessage(conversationId, question) {
        return apiClient.post('/chat/send', { conversationId, question });
    },
    // 【新增】: 请求AI为对话生成标题
    getConversationTitle(conversationId) {
        return apiClient.post(`/chat/title`, { conversationId });
    }
};
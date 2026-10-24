import { request } from '../client';
import type { AiAvatar, ChatMessage, PageResult, PageRequest } from '@smartclass/shared';

/**
 * AI 智能服务 API
 */
export const intelligenceApi = {
  /**
   * 获取 AI 分身列表
   */
  listAvatars: (params: PageRequest & {
    category?: string;
    status?: number;
    isPublic?: number;
  }) =>
    request.get<PageResult<AiAvatar>>('/intelligence/avatar/page', { params }),
  
  /**
   * 获取 AI 分身详情
   */
  getAvatarById: (id: number) =>
    request.get<AiAvatar>(`/intelligence/avatar/get/${id}`),
  
  /**
   * 发送聊天消息 (非流式)
   */
  sendChatMessage: (data: {
    aiAvatarId: number;
    content: string;
    sessionId?: string;
    conversationId?: string;
  }) =>
    request.post<{
      messageId: string;
      content: string;
      tokens: number;
      sessionId: string;
    }>('/intelligence/chat/send', data),
  
  /**
   * 发送聊天消息 (流式 SSE)
   */
  sendChatMessageStream: (data: {
    aiAvatarId: number;
    content: string;
    sessionId?: string;
    conversationId?: string;
  }, onMessage: (chunk: {
    content: string;
    done: boolean;
    messageId?: string;
    tokens?: number;
  }) => void) => {
    return new Promise<void>((resolve, reject) => {
      const eventSource = new EventSource(
        `${import.meta.env.VITE_API_BASE_URL || '/api'}/intelligence/chat/stream`,
        { withCredentials: true }
      );
      
      // 发送请求数据
      fetch(`${import.meta.env.VITE_API_BASE_URL || '/api'}/intelligence/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('smartclass_token')}`,
          'X-Tenant-Id': localStorage.getItem('smartclass_tenant_id') || 'default'
        },
        body: JSON.stringify(data)
      }).catch(reject);
      
      eventSource.onmessage = (event) => {
        try {
          const chunk = JSON.parse(event.data);
          onMessage(chunk);
          if (chunk.done) {
            eventSource.close();
            resolve();
          }
        } catch (e) {
          console.error('Parse SSE error:', e);
        }
      };
      
      eventSource.onerror = () => {
        eventSource.close();
        reject(new Error('SSE connection error'));
      };
    });
  },

  /**
   * 获取聊天历史
   */
  getChatHistory: (params: {
    aiAvatarId: number;
    sessionId?: string;
    pageCurrent?: number;
    pageSize?: number;
  }) =>
    request.get<PageResult<ChatMessage>>('/intelligence/chat/history', { params }),
  
  /**
   * 删除聊天会话
   */
  deleteChatSession: (sessionId: string) =>
    request.delete(`/intelligence/chat/session/${sessionId}`),
  
  /**
   * 创建 AI 分身 (管理员)
   */
  createAvatar: (data: {
    name: string;
    description?: string;
    avatarImgUrl?: string;
    category?: string;
    modelType?: string;
    personality?: string;
    abilities?: string;
    tags?: string;
    baseUrl?: string;
    avatarAuth?: string;
    isPublic?: number;
  }) =>
    request.post<AiAvatar>('/intelligence/avatar/add', data),
  
  /**
   * 更新 AI 分身
   */
  updateAvatar: (id: number, data: Partial<AiAvatar>) =>
    request.put<AiAvatar>(`/intelligence/avatar/update/${id}`, data),
  
  /**
   * 删除 AI 分身
   */
  deleteAvatar: (id: number) =>
    request.delete<boolean>(`/intelligence/avatar/delete/${id}`),
  
  /**
   * 获取推荐 AI 分身
   */
  getRecommendedAvatars: (limit = 6) =>
    request.get<AiAvatar[]>(`/intelligence/avatar/recommend?limit=${limit}`),
  
  /**
   * 获取分类列表
   */
  getCategories: () =>
    request.get<Array<{ value: string; label: string; count: number }>>('/intelligence/avatar/categories')
};

/**
 * AI 习题生成 API
 */
export const exerciseApi = {
  /**
   * 生成习题
   */
  generate: (data: {
    courseId: number;
    chapterId?: number;
    lessonId?: number;
    questionType?: 'single_choice' | 'multiple_choice' | 'fill_blank' | 'short_answer';
    difficulty?: 'easy' | 'medium' | 'hard';
    count?: number;
    knowledgePoints?: string[];
  }) =>
    request.post<Array<{
      id: string;
      type: string;
      question: string;
      options?: string[];
      answer: string;
      explanation?: string;
      difficulty: string;
      knowledgePoint: string;
    }>>('/intelligence/exercise/generate', data),
  
  /**
   * 获取生成历史
   */
  getHistory: (params: PageRequest & { courseId?: number }) =>
    request.get<PageResult<any>>('/intelligence/exercise/history', { params }),
  
  /**
   * 提交练习答案
   */
  submitAnswer: (data: {
    exerciseId: string;
    userAnswer: string;
    timeSpent: number;
  }) =>
    request.post<{
      correct: boolean;
      score: number;
      explanation: string;
      nextQuestion?: any;
    }>('/intelligence/exercise/submit', data)
};

/**
 * RAG 知识检索 API
 */
export const ragApi = {
  /**
   * 知识库搜索
   */
  search: (data: {
    query: string;
    courseId?: number;
    chapterId?: number;
    topK?: number;
    threshold?: number;
  }) =>
    request.post<Array<{
      id: string;
      content: string;
      score: number;
      source: string;
      metadata: Record<string, any>;
    }>>('/intelligence/rag/search', data),
  
  /**
   * 获取知识库文档列表
   */
  getDocuments: (params: PageRequest & { courseId?: number }) =>
    request.get<PageResult<any>>('/intelligence/rag/documents', { params }),
  
  /**
   * 上传文档到知识库
   */
  uploadDocument: (file: File, _courseId: number, onProgress?: (p: number) => void) => {
    return request.upload<{ documentId: string }>('/intelligence/rag/upload', file, onProgress);
  },
  
  /**
   * 删除文档
   */
  deleteDocument: (documentId: string) =>
    request.delete(`/intelligence/rag/document/${documentId}`)
};
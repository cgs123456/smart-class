import { request } from '../client';
import type { AiAvatar, ChatMessage, PageResult, PageRequest } from '@smartclass/shared';

/**
 * AI 智能服务 API —— 与单体后端实际契约对齐：
 * - AI 分身：AiAvatarController(/ai-avatars) + UserAiAvatarController(/user_ai_avatar)
 * - 聊天：AiAvatarChatController(/chat)，流式为 POST /chat/message/stream 的 SSE 响应
 * - 习题：ExerciseGenerationController（注意其后端路径自带 /api 前缀，叠加 context-path
 *   后完整路径为 /api/api/exercise/**，此处按真实路径书写）
 * - RAG 检索后端暂无 HTTP 端点（仅有服务层实现），原 ragApi 为虚构契约已删除
 */
export const intelligenceApi = {
  /**
   * 分页获取公开 AI 分身列表
   */
  listAvatars: (params: PageRequest & {
    category?: string;
    status?: number;
    isPublic?: number;
    sortField?: string;
    sortOrder?: string;
  }) =>
    request.get<PageResult<AiAvatar>>('/ai-avatars/page', { params }),

  /**
   * 获取 AI 分身详情
   */
  getAvatarById: (id: number) =>
    request.get<AiAvatar>(`/ai-avatars/${id}`),

  /**
   * 创建 AI 分身（管理员）
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
    request.post<AiAvatar>('/ai-avatars', data),

  /**
   * 删除 AI 分身（管理员）
   */
  deleteAvatar: (id: number) =>
    request.delete<boolean>(`/ai-avatars/${id}`),

  /**
   * 我的 AI 分身列表
   */
  myAvatars: () =>
    request.get<AiAvatar[]>('/user_ai_avatar/my/list'),

  /**
   * 创建我的 AI 分身
   */
  createMyAvatar: (data: Record<string, unknown>) =>
    request.post<number>('/user_ai_avatar/add', data),

  /**
   * 更新我的 AI 分身
   */
  updateMyAvatar: (data: Record<string, unknown>) =>
    request.post<boolean>('/user_ai_avatar/update', data),

  /**
   * 删除我的 AI 分身
   */
  deleteMyAvatar: (id: number) =>
    request.post<boolean>('/user_ai_avatar/delete', { id }),

  /**
   * 创建聊天会话
   */
  createChatSession: (aiAvatarId: number) =>
    request.post<string>('/chat/session/create', { aiAvatarId }),

  /**
   * 发送聊天消息（非流式）
   */
  sendChatMessage: (data: {
    aiAvatarId: number;
    sessionId?: string;
    content: string;
    fileIds?: string[];
  }) =>
    request.post<ChatMessage>('/chat/message/send', data),

  /**
   * 获取聊天历史
   */
  getChatHistory: (sessionId: string) =>
    request.get<ChatMessage[]>('/chat/history', { params: { sessionId } }),

  /**
   * 会话列表
   */
  listChatSessions: () =>
    request.get('/chat/sessions')
};

/**
 * 流式聊天：POST /chat/message/stream 返回 text/event-stream（SSE over POST，
 * 浏览器 EventSource 不支持 POST，改用 fetch 读流解析，事件为 Dify 原生格式：
 * {event:'message', answer} 增量 / {event:'message_end', conversation_id} 结束）
 */
export async function sendChatMessageStream(
  data: { aiAvatarId: number; sessionId?: string; content: string },
  onChunk: (chunk: { content: string; done: boolean; conversationId?: string }) => void,
  signal?: AbortSignal
): Promise<void> {
  const base = (import.meta.env.VITE_API_BASE_URL || '/api') as string;
  const response = await fetch(`${base}/chat/message/stream`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(data),
    signal
  });
  if (!response.ok || !response.body) {
    throw new Error(`流式请求失败：HTTP ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let conversationId: string | undefined;

  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    // 按行解析 SSE：事件以空行分隔，数据行以 "data:" 开头
    let sep: number;
    while ((sep = buffer.indexOf('\n\n')) >= 0) {
      const rawEvent = buffer.slice(0, sep);
      buffer = buffer.slice(sep + 2);
      for (const line of rawEvent.split('\n')) {
        if (!line.startsWith('data:')) continue;
        const payload = line.slice(5).trim();
        if (!payload) continue;
        try {
          const evt = JSON.parse(payload);
          if (evt.event === 'message_end') {
            conversationId = evt.conversation_id ?? conversationId;
            onChunk({ content: '', done: true, conversationId });
            return;
          }
          if (evt.event === 'message' && typeof evt.answer === 'string') {
            onChunk({ content: evt.answer, done: false });
          }
        } catch {
          // 非 JSON 数据帧（如注释/心跳）忽略
        }
      }
    }
  }
  // 流结束但未收到 message_end
  onChunk({ content: '', done: true, conversationId });
}

/**
 * AI 习题生成 API（注意后端真实前缀为 /api/exercise，叠加 context-path 后为 /api/api/exercise）
 */
export const exerciseApi = {
  /**
   * 生成课程习题（管理员，单次上限 20 题，返回生成数量）
   */
  generate: (data: { courseId: number; chapterId?: number; questionCount?: number }) =>
    request.post<number>('/api/exercise/generate', data),

  /**
   * 获取课程习题列表
   */
  list: (params: { courseId: number; current?: number; pageSize?: number }) =>
    request.get<PageResult<unknown>>('/api/exercise/list', { params })
};

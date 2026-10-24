# P2-3: 实时协作 (Yjs/CRDT) 设计文档

## 1. 业务场景

| 场景 | 当前痛点 | 目标体验 |
|------|----------|----------|
| **协同笔记/文档** | 无实时同步，覆盖冲突丢失内容 | Google Docs 级多光标、实时同步、离线优先 |
| **代码/Markdown 协作** | 无法多人同时编辑代码块 | 语法高亮保持、光标位置同步、冲突自动合并 |
| **白板/思维导图** | 单用户操作，无法团队头脑风暴 | 图形拖拽实时同步、撤销/重做共享 |
| **课程协作编辑** | 教师/助教串行编辑，版本管理混乱 | 实时协同编辑大纲/章节/习题、权限分级 |

---

## 2. 技术选型：为什么选 Yjs + CRDT

### 2.1 方案对比

| 维度 | **Yjs (CRDT)** | **Automerge** | **ShareDB (OT)** | **FireStore/Replicache** |
|------|----------------|---------------|------------------|--------------------------|
| **算法** | CRDT (YATA) | CRDT (RGA) | OT (Transform) | CRDT + 服务端权威 |
| **离线支持** | 原生优秀 | 原生优秀 | 差 (需服务端) | 原生优秀 |
| **冲突解决** | 自动、无中心 | 自动、无中心 | 需中心服务器转换 | 自动、最终一致 |
| **数据结构** | Y.Text, Y.Map, Y.Array, Y.Xml | JSON-like | JSON0/OTText | 自定义 |
| **编辑器绑定** | **极其丰富** (ProseMirror, Monaco, CodeMirror, Quill, Slate) | 较少 | 仅 ProseMirror/CodeMirror | 需适配 |
| **网络拓扑** | P2P / 中心化 / 混合 | 中心化 | 中心化 | 中心化 (Firebase) |
| **性能 (大文档)** | 优秀 (增量更新) | 中等 (全量状态) | 依赖服务端 | 优秀 |
| **生态成熟度** | **最高** (Figma, Notion, 许多开源) | 较好 | 老牌但维护慢 | 绑定 Firebase |
| **中文社区** | 活跃 | 一般 | 一般 | 依赖 Google |

### 2.2 结论：选择 **Yjs**

- **编辑器生态最完善**：Monaco (代码)、ProseMirror (富文本)、CodeMirror 6 (通用) 官方绑定
- **CRDT 理论成熟**：YATA 算法经过大规模生产验证 (Figma, Notion, 许多 SaaS)
- **灵活部署**：支持 WebRTC P2P、WebSocket 中心化、HTTP 长轮询、IndexedDB 持久化
- **TypeScript 原生**：类型安全，前端集成体验佳

---

## 3. 架构设计

### 3.1 总体拓扑

```
┌─────────────────────────────────────────────────────────────────┐
│                        客户端 (浏览器/移动端)                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  Editor A   │  │  Editor B   │  │  Editor C   │   ...        │
│  │ (Monaco)    │  │ (ProseMirror)│  │ (Custom)    │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
│         │                │                │                      │
│         ▼                ▼                ▼                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Yjs Doc (CRDT State)                  │   │
│  │  - Y.Text (正文)  - Y.Map (元数据)  - Y.Array (评论)     │   │
│  └────────────────────────┬────────────────────────────────┘   │
│                           │                                     │
│         ┌─────────────────┼─────────────────┐                  │
│         ▼                 ▼                 ▼                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ y-websocket │  │ y-webrtc    │  │ y-indexeddb │            │
│  │ (中心化)    │  │ (P2P 信令)  │  │ (离线持久化) │            │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘            │
└─────────┼────────────────┼────────────────┼────────────────────┘
          │                │                │
          ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         服务端                                    │
│  ┌──────────────────┐  ┌──────────────────┐                    │
│  │  y-websocket     │  │  持久化 Worker   │                    │
│  │  Server (Hocuspocus│  │  (Y.Snapshot +   │                    │
│  │  / 自研)         │  │   PostgreSQL)    │                    │
│  └──────────────────┘  └──────────────────┘                    │
│           │                       │                             │
│           ▼                       ▼                             │
│  ┌──────────────────┐  ┌──────────────────┐                    │
│  │  权限/房间管理   │  │  导出/快照/审计   │                    │
│  │  (JWT + RBAC)    │  │  (PDF/MD/HTML)   │                    │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 核心组件

| 组件 | 技术 | 说明 |
|------|------|------|
| **协同引擎** | `yjs` + `y-prosemirror` / `y-monaco` / `y-codemirror.next` | 核心 CRDT 状态 |
| **网络传输** | `y-websocket` (Hocuspocus) + `y-webrtc` (可选 P2P) | WebSocket 主通道，P2P 降低服务端压力 |
| **持久化** | `y-sweet` (SQLite) / PostgreSQL + `yjs` 编码 | 增量更新存储，支持时间旅行 |
| **权限控制** | 自定义 `Awareness` + 服务端中间件 | 只读/评论/编辑/管理员、租户隔离 |
| **冲突可视化** | Yjs 内置 `UndoManager` + 自定义冲突标记 | 显示冲突区域、接受/拒绝 |

---

## 4. 数据模型

### 4.1 文档结构 (Yjs Types 映射)

```typescript
// 文档根结构
interface CollabDoc {
  // 正文内容 (富文本/Markdown/代码)
  content: Y.Text;           // 或 Y.XmlFragment (ProseMirror)
  
  // 元数据
  meta: Y.Map<{
    title: string;
    language: string;        // markdown, typescript, python...
    version: number;
    lastEditedBy: string;
    lastEditedAt: number;
  }>;
  
  // 评论/批注 (锚定到位置)
  comments: Y.Array<CommentThread>;
  
  // 协作光标/选区
  awareness: Awareness;      // y-awareness 内置
  
  // 版本历史 (服务端维护)
  // snapshots: Y.Map<Snapshot>;
}

interface CommentThread {
  id: string;                    // UUID
  anchor: { start: number; end: number }; // 相对 content 位置
  authorId: string;
  authorName: string;
  authorAvatar: string;
  createdAt: number;
  updatedAt: number;
  resolved: boolean;
  replies: CommentReply[];
}

interface CommentReply {
  id: string;
  authorId: string;
  content: string;               // 支持 @mention
  createdAt: number;
}
```

### 4.2 房间与权限

```typescript
interface RoomConfig {
  docId: string;
  tenantId: string;              // 多租户隔离 (P1-3)
  
  // 权限矩阵
  permissions: {
    [role: string]: {
      read: boolean;
      comment: boolean;
      edit: boolean;
      manage: boolean;           // 邀请/踢人/设置权限
      export: boolean;
    }
  };
  
  // 默认角色: owner > admin > editor > commenter > viewer
  defaultRole: 'viewer';
  
  // 协作设置
  settings: {
    maxUsers: 50;                // 单文档最大协作人数
    enableP2P: boolean;          // 启用 WebRTC
    autoSaveInterval: 2000;      // ms
    snapshotInterval: 300000;    // 5 min 服务端快照
    retentionDays: 90;           // 历史保留
  };
}
```

---

## 5. 服务端实现：Hocuspocus (推荐)

### 5.1 为什么选 Hocuspocus
- **Yjs 官方推荐**的 WebSocket 服务端实现
- **TypeScript 原生**，扩展机制完善 (Hooks/Extensions)
- **内置**：权限、持久化、文档加载、统计、监控
- **可水平扩展**：Redis PubSub 多实例同步

### 5.2 核心配置

```typescript
// server/hocuspocus-server.ts
import { Server } from '@hocuspocus/server';
import { Database } from '@hocuspocus/extension-database';
import { Logger } from '@hocuspocus/extension-logger';
import { Redis } from '@hocuspocus/extension-redis';
import { Tiptap } from '@hocuspocus/extension-tiptap';

const server = Server.configure({
  port: 12346,
  token: process.env.HOCUSPOCUS_TOKEN, // JWT 验证
  
  // 文档加载钩子：权限校验 + 租户隔离
  async onLoadDocument(data) {
    const { documentName, token } = data;
    const payload = verifyToken(token);
    
    // 校验租户权限
    const hasAccess = await checkTenantAccess(payload.tenantId, documentName);
    if (!hasAccess) throw new Error('Forbidden');
    
    // 返回文档初始内容 (从 DB 加载)
    return await loadDocumentFromDB(documentName);
  },
  
  // 文档存储钩子：增量持久化
  async onStoreDocument(data) {
    await saveDocumentToDB(data.documentName, data.documentState);
  },
  
  // 连接认证
  async onAuthenticate(data) {
    const { token } = data;
    const payload = verifyToken(token);
    return {
      user: { id: payload.sub, name: payload.name, avatar: payload.avatar },
      permissions: getPermissions(payload.role, payload.tenantId, data.documentName)
    };
  },
  
  extensions: [
    new Database({ /* PostgreSQL 配置 */ }),
    new Redis({ /* Redis PubSub 配置 */ }),
    new Logger(),
    new Tiptap({ /* ProseMirror 扩展 */ })
  ]
});

server.listen();
```

### 5.3 权限扩展

```typescript
// extensions/permission.ts
import { Extension } from '@hocuspocus/extension';

export class PermissionExtension extends Extension {
  onConnect(data) {
    const { user, permissions } = data;
    // 将权限注入 Awareness，客户端可读取
    data.document.awareness.setLocalStateField('permissions', permissions);
  }
  
  onAwarenessUpdate(data) {
    const { awareness, changedClients } = data;
    // 只允许有 edit 权限的用户广播编辑操作
    changedClients.forEach(clientId => {
      const state = awareness.getStates().get(clientId);
      if (state?.permissions?.edit === false) {
        // 撤销该客户端的编辑操作 (发送 revert)
        this.revertClientEdits(clientId);
      }
    });
  }
}
```

---

## 6. 前端集成方案

### 6.1 通用 Hook 封装

```typescript
// hooks/useCollabDoc.ts
import { useEffect, useRef, useState } from 'react';
import * as Y from 'yjs';
import { WebsocketProvider } from 'y-websocket';
import { IndexeddbPersistence } from 'y-indexeddb';
import { MonacoBinding } from 'y-monaco';
import { ProsemirrorBinding } from 'y-prosemirror';

interface UseCollabDocOptions {
  docId: string;
  token: string;
  editorType: 'monaco' | 'prosemirror' | 'codemirror';
  editorInstance: any; // MonacoEditor / ProseMirrorView / CodeMirrorView
  onSynced?: (isSynced: boolean) => void;
  onAwarenessChange?: (states: Map<number, any>) => void;
}

export function useCollabDoc({
  docId,
  token,
  editorType,
  editorInstance,
  onSynced,
  onAwarenessChange
}: UseCollabDocOptions) {
  const ydocRef = useRef<Y.Doc>(new Y.Doc());
  const providerRef = useRef<WebsocketProvider>();
  const bindingRef = useRef<any>();
  const [isConnected, setIsConnected] = useState(false);
  const [users, setUsers] = useState<Map<number, any>>(new Map());

  useEffect(() => {
    const ydoc = ydocRef.current;
    
    // 1. 离线持久化
    const indexeddb = new IndexeddbPersistence(docId, ydoc);
    indexeddb.on('synced', () => console.log('IndexedDB synced'));
    
    // 2. WebSocket 连接
    const provider = new WebsocketProvider(
      `${import.meta.env.VITE_WS_URL}?token=${encodeURIComponent(token)}`,
      docId,
      ydoc,
      { connect: true, maxBackoffTime: 30000 }
    );
    providerRef.current = provider;
    
    // 连接状态
    provider.on('status', ({ status }) => {
      setIsConnected(status === 'connected');
      onSynced?.(status === 'synced');
    });
    
    // 感知状态 (光标/选区/用户信息)
    provider.awareness.on('change', () => {
      const states = provider.awareness.getStates();
      setUsers(new Map(states));
      onAwarenessChange?.(states);
    });
    
    // 3. 编辑器绑定
    switch (editorType) {
      case 'monaco':
        bindingRef.current = new MonacoBinding(
          ydoc.getText('content'),
          editorInstance.getModel(),
          new Set([editorInstance]),
          provider.awareness
        );
        break;
      case 'prosemirror':
        bindingRef.current = new ProsemirrorBinding(
          ydoc.getXmlFragment('content'),
          editorInstance,
          provider.awareness
        );
        break;
      case 'codemirror':
        // import { CodemirrorBinding } from 'y-codemirror.next'
        break;
    }
    
    // 4. 撤销管理器 (跨用户隔离)
    const undoManager = new Y.UndoManager(ydoc.getText('content'), {
      trackedOrigins: new Set([null]), // 只跟踪本地操作
      captureTimeout: 500
    });
    
    // 快捷键绑定
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'z') {
        e.preventDefault();
        e.shiftKey ? undoManager.redo() : undoManager.undo();
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'y') {
        e.preventDefault();
        undoManager.redo();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      bindingRef.current?.destroy();
      provider.destroy();
      indexeddb.destroy();
      ydoc.destroy();
    };
  }, [docId, token, editorType, editorInstance]);
  
  return { ydoc: ydocRef.current, provider: providerRef.current, isConnected, users };
}
```

### 6.2 组件示例：协作代码编辑器

```tsx
// components/CollabCodeEditor.tsx
import { useRef, useEffect } from 'react';
import * as monaco from 'monaco-editor';
import { useCollabDoc } from '@/hooks/useCollabDoc';
import { UserCursor } from '@/components/UserCursor';

interface CollabCodeEditorProps {
  docId: string;
  language: 'typescript' | 'python' | 'java' | 'sql' | 'markdown';
  token: string;
  readOnly?: boolean;
}

export function CollabCodeEditor({ docId, language, token, readOnly }: CollabCodeEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor>();
  
  const { users, isConnected } = useCollabDoc({
    docId,
    token,
    editorType: 'monaco',
    editorInstance: editorRef.current!,
    onAwarenessChange: (states) => {
      // 更新远程光标
      updateRemoteCursors(editorRef.current!, states);
    }
  });
  
  useEffect(() => {
    if (!containerRef.current) return;
    
    editorRef.current = monaco.editor.create(containerRef.current, {
      value: '', // 初始值由 Yjs 加载
      language,
      theme: 'vs-dark',
      automaticLayout: true,
      readOnly,
      minimap: { enabled: true },
      fontSize: 14,
      lineNumbers: 'on',
      cursorBlinking: 'phase',
      cursorSmoothCaretAnimation: 'on'
    });
    
    return () => editorRef.current?.dispose();
  }, []);
  
  // 远程光标渲染
  const updateRemoteCursors = (editor: monaco.editor.IStandaloneCodeEditor, states: Map<number, any>) => {
    states.forEach((state, clientId) => {
      if (clientId === states.get(providerRef.current?.awareness.clientID)?.clientID) return;
      const selection = state.selection;
      if (selection) {
        // 使用 monaco.editor.setDecorations 渲染远程光标/选区
        // 显示用户名、颜色区分
      }
    });
  };
  
  return (
    <div className="collab-editor-wrapper">
      <div className="collab-toolbar">
        <ConnectionStatus connected={isConnected} />
        <UserAvatars users={Array.from(users.values())} />
        {readOnly ? <Badge>只读模式</Badge> : <Badge>可编辑</Badge>}
      </div>
      <div ref={containerRef} className="editor-container" />
    </div>
  );
}
```

---

## 7. 部署与运维

### 7.1 部署架构

```yaml
# docker-compose.collab.yml
services:
  hocuspocus:
    image: registry.smartclass.cn/hocuspocus:latest
    deploy:
      replicas: 3
      resources:
        limits: { cpus: '2', memory: '2G' }
    environment:
      - PORT=12346
      - HOCUSPOCUS_TOKEN=${JWT_SECRET}
      - DATABASE_URL=postgresql://...
      - REDIS_URL=redis://redis:6379
    ports:
      - "12346:12346"
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:12346/health"]
      interval: 10s

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes: [redis-data:/data]

  # 可选：WebRTC 信令服务
  signal-server:
    image: registry.smartclass.cn/y-webrtc-signaling:latest
    ports: ["4443:4443"]
```

### 7.2 扩展性设计

| 指标 | 单实例承载 | 水平扩展策略 |
|------|------------|--------------|
| **连接数** | ~5,000 并发 | Redis PubSub 多实例 + Nginx 负载均衡 |
| **文档数** | ~10,000 活跃 | 分片：docId Hash 取模路由 |
| **消息吞吐** | ~50k ops/s | 批量合并发送、二进制编码 (lib0/encoding) |
| **存储** | PostgreSQL 分区表 | 按 tenant_id + docId 分区、冷数据归档 |

### 7.3 监控指标

```prometheus
# 关键指标
hocuspocus_connections_total{status="connected"}
hocuspocus_documents_loaded_total
hocuspocus_operations_per_second
hocuspocus_document_size_bytes
hocuspocus_persistence_latency_seconds
hocuspocus_awareness_updates_per_second
hocuspocus_conflicts_resolved_total

# 告警规则
- alert: HocuspocusHighLatency
  expr: histogram_quantile(0.99, hocuspocus_persistence_latency_seconds) > 1
- alert: HocuspocusConnectionDrop
  expr: rate(hocuspocus_connections_total{status="disconnected"}[5m]) > 10
```

---

## 8. 迁移与兼容

### 8.1 现有内容迁移
```typescript
// 迁移脚本: 现有文档 -> Yjs 格式
async function migrateLegacyDoc(legacyDoc: LegacyDoc): Promise<Uint8Array> {
  const ydoc = new Y.Doc();
  const content = ydoc.getText('content');
  const meta = ydoc.getMap('meta');
  
  // 富文本 HTML -> ProseMirror JSON -> Y.XmlFragment
  // 或 Markdown -> Y.Text
  content.insert(0, legacyDoc.content);
  
  meta.set('title', legacyDoc.title);
  meta.set('language', detectLanguage(legacyDoc.content));
  meta.set('version', 1);
  
  return Y.encodeStateAsUpdate(ydoc); // 二进制存入 DB
}
```

### 8.2 导出/互操作
```typescript
// 支持导出格式
async function exportDoc(docId: string, format: 'pdf' | 'md' | 'html' | 'docx') {
  const ydoc = await loadDoc(docId);
  const content = ydoc.getText('content').toString();
  
  switch (format) {
    case 'md': return content; // Markdown 直接是纯文本
    case 'html': return markdownToHtml(content);
    case 'pdf': return htmlToPdf(await exportDoc(docId, 'html'));
    case 'docx': return htmlToDocx(await exportDoc(docId, 'html'));
  }
}
```

---

## 9. 计划与里程碑

| 阶段 | 时间 | 交付物 |
|------|------|--------|
| **Phase 1** | Week 1 | Hocuspocus 服务端部署、基础 WebSocket 连接、认证/权限 |
| **Phase 2** | Week 2 | Monaco 代码协作编辑器、光标/选区同步、撤销管理 |
| **Phase 3** | Week 3 | ProseMirror 富文本协作、评论/批注系统、@mention |
| **Phase 4** | Week 4 | IndexedDB 离线支持、冲突可视化、导出 (PDF/MD/HTML) |
| **Phase 5** | Week 5 | P2P (y-webrtc) 可选、大文档性能优化、压测 |
| **Phase 6** | Week 6 | 管理后台 (文档管理/权限/审计)、监控大盘、文档 |

---

## 10. 工作量预估

| 模块 | 人天 |
|------|------|
| Hocuspocus 服务端部署 + 权限扩展 | 5 |
| Monaco 协作编辑器集成 | 5 |
| ProseMirror 富文本协作集成 | 8 |
| 评论/批注/提及系统 | 5 |
| 离线支持 + 冲突 UI | 5 |
| 导出/快照/审计 | 3 |
| 压测 + 监控 + 运维文档 | 4 |
| **合计** | **~35 人天 (约 5-6 周, 2 人)** |

---

## 11. 依赖关系

| 依赖 | 说明 |
|------|------|
| **P1-1 Dubbo3** | 协作服务可作为微服务注册，文档元数据同步 |
| **P1-2 Gateway** | WebSocket 升级鉴权、租户透传、限流 |
| **P1-3 多租户** | 文档强制租户隔离、跨租户协作禁止 |
| **P1-4 Monorepo** | 协作编辑器组件发布为 `@smartclass/ui-collab` 包 |
| **P0-2 Observability** | 全链路 Trace: 编辑操作 → CRDT 合并 → 持久化 → 推送 |

---

## 12. 替代方案备选

| 场景 | 备选方案 |
|------|----------|
| **无服务端运维能力** | **PartyKit** / **Liveblocks** / **SuperViz** (托管协作后端) |
| **仅需简单文本同步** | **Socket.io + 简单 OT** (自研，维护成本高) |
| **强一致性要求 (金融/法律)** | **ShareDB (OT) + MongoDB** (中心化权威) |
| **移动端原生优先** | **Yjs + y-webrtc (P2P)** + **Automerge-Repo (同步层)** |

---

## 13. 关键决策点 (需团队确认)

1. **编辑器选型**：代码场景用 Monaco，富文本用 ProseMirror，还是统一 CodeMirror 6？
2. **P2P 需求**：是否需要局域网/弱网离线协作？(决定是否引入 y-webrtc + 信令服务器)
3. **权限粒度**：是否需要段落/行级权限？(Yjs 原生不支持，需应用层实现)
4. **历史版本**：是否需要完整时间旅行 (任意时间点回滚)？(存储成本高，建议定期快照 + 增量)
5. **导出格式**：PDF/Word 是否需要服务端渲染 (Puppeteer/Headless Chrome)？
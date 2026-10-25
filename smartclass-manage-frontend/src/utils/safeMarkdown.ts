import { defaultSchema } from 'rehype-sanitize';
import rehypeRaw from 'rehype-raw';
import rehypeSanitize from 'rehype-sanitize';
import rehypeKatex from 'rehype-katex';
import type { PluggableList } from 'unified';

/**
 * 帖子/文章内容来自普通用户，服务端不清洗富文本，因此渲染端必须消毒：
 * rehypeRaw 解析内嵌 HTML → rehypeSanitize 白名单过滤 → rehypeKatex 最后执行
 * （KaTeX 生成的标记是本地生成的，不经消毒，math 节点靠 code/span 上的 className 存活）。
 */
const sanitizeSchema = {
  ...defaultSchema,
  attributes: {
    ...defaultSchema.attributes,
    '*': [...(defaultSchema.attributes?.['*'] ?? []), 'className'],
    code: [...(defaultSchema.attributes?.code ?? []), 'className'],
    span: ['className'],
    div: ['className'],
    img: ['src', 'alt', 'title', 'width', 'height', 'loading'],
    a: ['href', 'title', 'target', 'rel'],
  },
  protocols: {
    ...defaultSchema.protocols,
    href: ['http', 'https', 'mailto'],
    src: ['http', 'https', 'data'],
  },
};

/** 统一的安全 rehype 插件管线：Raw → Sanitize → KaTeX。
 *  显式 PluggableList（unified 11 起拒绝 readonly 元组），不得写 as const。 */
export const safeRehypePlugins: PluggableList = [
  rehypeRaw,
  [rehypeSanitize, { schema: sanitizeSchema }],
  rehypeKatex,
];

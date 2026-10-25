// 统一 Markdown+KaTeX 渲染管线：先把 LaTeX 公式预渲染为 KaTeX HTML，再交给 marked 解析，
// 最后经 DOMPurify 白名单清理。替代此前 markdown-it 与 marked 两套并行实现。
import { marked } from 'marked';
import katex from 'katex';
import DOMPurify from 'dompurify';

marked.setOptions({ gfm: true, breaks: true });

// KaTeX 输出含 MathML 与 SVG，需要 html+svg+mathMl 三个 profile 同时放行
const PURIFY_CONFIG = {
  USE_PROFILES: { html: true, svg: true, mathMl: true },
  ADD_ATTR: ['target', 'class', 'style', 'aria-hidden', 'role'],
};

// KaTeX 生成节点打标，便于上层样式或测试识别
DOMPurify.addHook('afterSanitizeAttributes', function (node) {
  if (
    node.classList &&
    (node.classList.contains('katex') ||
      node.classList.contains('katex-html') ||
      node.classList.contains('katex-mathml'))
  ) {
    node.setAttribute('data-katex-processed', 'true');
  }
});

function renderFormulas(content: string): string {
  // 块级公式 $$...$$
  let processed = content.replace(/\$\$([\s\S]+?)\$\$/g, (match, formula: string) => {
    try {
      return `<div class="katex-block">${katex.renderToString(formula.trim(), {
        displayMode: true,
        throwOnError: false,
      })}</div>`;
    } catch {
      return match;
    }
  });

  // 行内公式 $...$，排除货币写法（$10、10$）
  processed = processed.replace(/\$([^$\n]+?)\$/g, (match, formula: string) => {
    if (/^\$\d/.test(match) || /\d\$$/.test(match)) {
      return match;
    }
    try {
      return katex.renderToString(formula.trim(), {
        displayMode: false,
        throwOnError: false,
      });
    } catch {
      return match;
    }
  });

  return processed;
}

/**
 * 渲染 Markdown 内容（含 LaTeX 公式）为安全 HTML。
 * 解析失败时返回原始文本，由调用方决定降级展示方式。
 */
export function renderMarkdown(content: string): string {
  if (!content) {
    return '';
  }
  try {
    const html = marked.parse(renderFormulas(content), { async: false });
    return DOMPurify.sanitize(html, PURIFY_CONFIG);
  } catch (error) {
    console.error('Markdown渲染失败:', error);
    return content;
  }
}

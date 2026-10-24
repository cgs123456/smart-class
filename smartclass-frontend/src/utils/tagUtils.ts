/**
 * 文章标签样式 - 共享工具函数
 * 用于避免 ArticleContent.vue 和 ArticleList.vue 中的代码复制
 */

// 根据文章类别返回不同的样式
export const getTagStyle = (category: string): Record<string, string> => {
  const styles: Record<string, string> = {};

  switch (category) {
    case '励志':
      styles.background = 'linear-gradient(135deg, #ff9a9e 0%, #fad0c4 99%)';
      break;
    case '历史':
      styles.background = 'linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%)';
      break;
    case '科技':
      styles.background = 'linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%)';
      break;
    case '文化':
      styles.background = 'linear-gradient(135deg, #fbc2eb 0%, #a6c1ee 100%)';
      break;
    case '艺术':
      styles.background = 'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)';
      break;
    case '旅行':
      styles.background = 'linear-gradient(135deg, #f6d365 0%, #fda085 100%)';
      break;
    case '数学':
      styles.background = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
      break;
    case '应用':
      styles.background = 'linear-gradient(135deg, #2af598 0%, #009efd 100%)';
      break;
    default:
      styles.background = 'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)';
  }

  return styles;
};

// 将难度等级转换为文本
export const convertDifficultyToText = (difficulty: number | string): string => {
  if (typeof difficulty === 'string') {
    return difficulty;
  }

  const difficultyMap: Record<number, string> = {
    1: '初级',
    2: '中级',
    3: '高级',
  };

  return difficultyMap[difficulty] || '未知';
};
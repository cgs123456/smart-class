<template>
  <div>
    <!-- 精美文章模块 -->
    <van-cell-group class="article-module">
      <van-cell title="每日美文">
        <template #icon>
          <svg class="icon svg-icon article-icon" aria-hidden="true">
            <use xlink:href="#icon-wenzhang"></use>
          </svg>
        </template>
        <template #right-icon>
          <span class="more-link" @click="$emit('more')">更多</span>
        </template>
      </van-cell>

      <div class="article-list">
        <div
          v-for="article in articles"
          :key="article.id"
          class="article-item"
          @click="showArticleDetail(article)"
        >
          <div class="article-cover">
            <van-image :src="article.cover" fit="cover" radius="4" />
            <span class="article-tag" :style="getTagStyle(article.category)">{{
              article.category
            }}</span>
          </div>
          <div class="article-info">
            <h3 class="article-title">{{ article.title }}</h3>
            <p class="article-brief">{{ article.brief }}</p>
            <!-- 显示前两个标签（如果有） -->
            <div
              class="list-tags-container"
              v-if="article.tags && article.tags.length > 0"
            >
              <van-tag
                v-for="tag in article.tags.slice(0, 2)"
                :key="tag"
                type="primary"
                plain
                class="list-tag-item"
              >
                {{ tag }}
              </van-tag>
              <span v-if="article.tags.length > 2" class="more-tags"
                >+{{ article.tags.length - 2 }}</span
              >
            </div>
            <div class="article-meta">
              <span>{{ article.readTime }}分钟</span>
              <span>{{ convertDifficultyToText(article.difficulty) }}</span>
              <span v-if="article.author">作者: {{ article.author }}</span>
            </div>
          </div>
        </div>
      </div>
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { getTagStyle, convertDifficultyToText } from '../../utils/tagUtils';

interface Article {
  id: number;
  title: string;
  brief: string;
  cover: string;
  category: string;
  readTime: number;
  difficulty: number | string;
  content: string;
  tags?: string[];
  author?: string;
  source?: string;
  publishDate?: string;
  viewCount?: number;
  likeCount?: number;
}

// 定义props
defineProps<{
  articles: Article[];
}>();

// 定义事件
defineEmits<{
  (e: 'more'): void;
}>();

const router = useRouter();

// 显示文章详情
const showArticleDetail = (article: Article): void => {
  if (article && article.id) {
    router.push(`/daily/article/${article.id}`);
  }
};

// 根据文章类别返回不同的样式（从共享工具导入）
import { getTagStyle, convertDifficultyToText } from '../../utils/tagUtils';
</script>

<style scoped>
.article-module {
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.article-icon {
  margin-right: 6px;
  color: #1989fa;
  font-size: 18px;
}

.more-link {
  color: #1989fa;
  font-size: 14px;
}

.article-list {
  padding: 0;
}

.article-item {
  display: flex;
  padding: 16px;
  position: relative;
  border-bottom: 0.5px solid #f5f5f5;
  cursor: pointer;
}

.article-item:last-child {
  border-bottom: none;
}

.article-item:active {
  background-color: #f9f9f9;
}

.article-cover {
  width: 120px;
  height: 80px;
  position: relative;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}

.article-cover .van-image {
  width: 100%;
  height: 100%;
}

.article-tag {
  position: absolute;
  top: 0;
  left: 0;
  padding: 2px 6px;
  font-size: 10px;
  color: white;
  border-bottom-right-radius: 4px;
}

.article-info {
  flex: 1;
  padding-left: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}

.article-title {
  font-size: 15px;
  font-weight: 500;
  line-height: 1.4;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-brief {
  font-size: 12px;
  color: #666;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
}

.article-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #999;
}

.list-tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.list-tag-item {
  font-size: 10px !important;
  padding: 0 4px !important;
  height: 16px !important;
  line-height: 16px !important;
}

.more-tags {
  font-size: 10px;
  color: #1989fa;
  line-height: 16px;
}
</style>

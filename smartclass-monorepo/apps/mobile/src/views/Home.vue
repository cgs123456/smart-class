<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button, Card, Avatar, Badge, Tag } from '@smartclass/ui';
import { useUserStore } from '@smartclass/stores';
import { courseApi } from '@smartclass/api';
import type { Course } from '@smartclass/shared';

const userStore = useUserStore();

const recommendCourses = ref<Course[]>([]);
const loading = ref(false);

async function loadRecommendCourses() {
  loading.value = true;
  try {
    const response = await courseApi.getRecommend(6);
    recommendCourses.value = response ?? [];
  } catch (error) {
    console.error('加载推荐课程失败:', error);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadRecommendCourses();
});

function navigateToCourse(course: Course) {
  window.location.href = `/course/${course.id}`;
}
</script>

<template>
  <div class="home-page">
    <!-- 顶部欢迎区 -->
    <div class="home-header">
      <div class="home-header__content">
        <div class="home-header__welcome">
          <h1 class="home-header__title">欢迎回来，{{ userStore.userName }}</h1>
          <p class="home-header__subtitle">继续你的学习之旅，每天进步一点点</p>
        </div>
        <Avatar :src="userStore.userAvatar" size="large" />
      </div>
    </div>
    
    <!-- 快速操作 -->
    <div class="home-actions">
      <Button type="primary" size="large" block @click="navigateToCourse({ id: 1 } as any)">
        继续学习
      </Button>
      <div class="home-actions__row">
        <Button type="default" @click="$router.push('/intelligence')">
          AI 助手
        </Button>
        <Button type="default" @click="$router.push('/circle')">
          圈子
        </Button>
      </div>
    </div>
    
    <!-- 推荐课程 -->
    <section class="home-section">
      <div class="home-section__header">
        <h2>为你推荐</h2>
        <Button type="text" size="small" @click="$router.push('/course')">查看全部</Button>
      </div>
      
      <div v-if="loading" class="home-courses__loading">
        <div class="skeleton" v-for="i in 3" :key="i"></div>
      </div>
      
      <div v-else class="home-courses">
        <Card
          v-for="course in recommendCourses"
          :key="course.id"
          :hoverable="true"
          @click="navigateToCourse(course)"
          class="home-course-card"
        >
          <template #header>
            <div class="course-card__header">
              <img :src="course.coverImgUrl" alt="" class="course-card__cover" />
              <div class="course-card__info">
                <h3 class="course-card__title">{{ course.title }}</h3>
                <p class="course-card__meta">
                  <Tag :type="course.level === 'beginner' ? 'success' : course.level === 'intermediate' ? 'warning' : 'error'" size="small">
                    {{ course.level }}
                  </Tag>
                  <span class="course-card__stats">
                    <Badge :value="course.studyCount" type="info" /> 学习
                  </span>
                </p>
              </div>
            </div>
          </template>
          
          <p class="course-card__desc">{{ course.summary }}</p>
          
          <template #footer>
            <div class="course-card__footer">
              <span v-if="course.isFree" class="course-card__free">免费</span>
              <span v-else class="course-card__price">¥{{ course.price }}</span>
              <Button size="small" type="primary">开始学习</Button>
            </div>
          </template>
        </Card>
      </div>
    </section>
  </div>
</template>


<style scoped>
.home-page {
  padding: 16px;
  padding-bottom: 80px;
  background-color: var(--color-bg-secondary);
  min-height: 100vh;
}

.home-header {
  margin-bottom: 24px;
}

.home-header__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
  border-radius: var(--radius-xl);
  color: var(--color-text-inverse);
}

.home-header__title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  margin: 0 0 8px 0;
}

.home-header__subtitle {
  font-size: var(--font-size-sm);
  opacity: 0.9;
  margin: 0;
}

.home-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.home-actions__row {
  display: flex;
  gap: 12px;
}

.home-actions__row > * {
  flex: 1;
}

.home-section {
  margin-bottom: 24px;
}

.home-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.home-section__header h2 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  margin: 0;
}

.home-courses {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.home-course-card {
  cursor: pointer;
  transition: transform var(--transition-fast);
}

.home-course-card:hover {
  transform: translateY(-4px);
}

.course-card__header {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.course-card__cover {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-md);
  object-fit: cover;
  flex-shrink: 0;
}

.course-card__info {
  flex: 1;
  min-width: 0;
}

.course-card__title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.course-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin: 0;
}

.course-card__desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0 0 12px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.course-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.course-card__free {
  color: var(--color-success);
  font-weight: var(--font-weight-semibold);
}

.course-card__price {
  color: var(--color-primary);
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-lg);
}

.skeleton {
  height: 300px;
  background: linear-gradient(90deg, var(--color-bg-tertiary) 25%, var(--color-border-primary) 50%, var(--color-bg-tertiary) 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: var(--radius-lg);
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
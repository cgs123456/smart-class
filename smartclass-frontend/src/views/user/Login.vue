<template>
  <div class="login">
    <!-- 替换为全局返回按钮 -->
    <back-button title="登录" />

    <div class="login-header">
      <van-image class="logo" width="80" height="80" src="/logo.svg" />
      <h2>欢迎来到智云星课</h2>
      <p class="subtitle">登录后即刻开启您的学习之旅</p>
    </div>

    <!-- 登录方式导航 -->
    <van-tabs v-model:active="loginType" class="login-tabs">
      <van-tab name="account" title="用户名登录" />
      <van-tab name="phone" title="手机号登录" />
    </van-tabs>

    <van-form @submit="onSubmit" class="login-form">
      <van-cell-group inset>
        <template v-if="loginType === 'account'">
          <van-field
            v-model="formData.userAccount"
            name="username"
            label="用户名"
            placeholder="请输入用户名"
            :rules="[{ required: true, message: '请填写用户名' }]"
            label-width="60"
          />
          <van-field
            v-model="formData.password"
            type="password"
            name="password"
            label="密码"
            placeholder="请输入密码"
            :rules="[{ required: true, message: '请填写密码' }]"
            label-width="60"
          />
        </template>
        <template v-else>
          <van-field
            v-model="formData.userPhone"
            type="tel"
            name="phone"
            label="手机号"
            placeholder="请输入手机号"
            :rules="[{ required: true, message: '请填写手机号' }]"
            label-width="60"
          />
          <van-field
            v-model="formData.password"
            type="password"
            name="password"
            label="密码"
            placeholder="请输入密码"
            :rules="[{ required: true, message: '请填写密码' }]"
            label-width="60"
          />
        </template>
        <van-field
          v-if="captchaEnabled"
          v-model="captchaCode"
          name="captcha"
          label="验证码"
          placeholder="请输入验证码"
          :rules="[{ required: true, message: '请填写验证码' }]"
          label-width="60"
        >
          <template #button>
            <img
              :src="captchaImage"
              alt="验证码"
              class="captcha-image"
              @click="refreshCaptcha"
            />
          </template>
        </van-field>
      </van-cell-group>

      <div class="form-actions">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
        >
          登录
        </van-button>
      </div>

      <div class="additional-links">
        <span class="link-text" @click="router.push('/forgot-password')"
          >忘记密码？</span
        >
        <span class="link-text" @click="router.push('/register')"
          >注册新账号</span
        >
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import { useUserStore } from '../../stores/userStore';
import { BackButton } from '../../components/Common';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const loginType = ref('account'); // 登录方式：account - 用户名登录，phone - 手机号登录

const formData = ref({
  userAccount: '',
  userPhone: '',
  password: '',
});

// 图形验证码（后端 /api/captcha，一次一用，失败或提交后需刷新）
const captchaEnabled = ref(true);
const captchaImage = ref('');
const captchaUuid = ref('');
const captchaCode = ref('');

const refreshCaptcha = async () => {
  const captcha = await userStore.fetchCaptcha();
  captchaEnabled.value = captcha.enabled;
  captchaImage.value = captcha.image;
  captchaUuid.value = captcha.uuid;
  captchaCode.value = '';
};

onMounted(refreshCaptcha);

const onSubmit = async () => {
  loading.value = true;
  try {
    let result;

    if (loginType.value === 'account') {
      // 用户名登录
      result = await userStore.login(
        formData.value.userAccount,
        formData.value.password,
        captchaUuid.value,
        captchaCode.value,
      );
    } else {
      // 手机号登录
      result = await userStore.loginByPhone(
        formData.value.userPhone,
        formData.value.password,
        captchaUuid.value,
        captchaCode.value,
      );
    }

    if (result.success) {
      showToast({
        type: 'success',
        message: '登录成功',
        onClose: () => {
          // 获取重定向URL
          const redirectUrl = router.currentRoute.value.query.redirect || '/';
          router.push(redirectUrl.toString());
        },
      });
    } else {
      showToast({
        type: 'fail',
        message: result.message || '登录失败',
      });
      // 验证码一次一用，失败后刷新以便重试
      refreshCaptcha();
    }
  } catch (error) {
    showToast({
      type: 'fail',
      message: error.message || '登录失败，请检查网络连接',
    });
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login {
  min-height: 100vh;
  background-color: #fff;
  display: flex;
  flex-direction: column;
}

.login-header {
  text-align: center;
  margin: 32px 0;
}

.logo {
  margin-bottom: 16px;
}

.login-header h2 {
  margin: 0;
  font-size: 24px;
  color: #323233;
  font-weight: 600;
}

.subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #969799;
}

.login-form {
  padding: 0 16px;
}

.form-actions {
  margin: 24px 0;
}

.additional-links {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  margin-top: 8px;
}

.link-text {
  font-size: 14px;
  color: #1989fa;
  cursor: pointer;
}

:deep(.van-field__label) {
  color: #323233;
}

:deep(.van-nav-bar) {
  background-color: transparent;
}

:deep(.van-nav-bar__title) {
  color: #323233;
}

:deep(.van-nav-bar .van-icon) {
  color: #323233;
}

.login-tabs {
  margin-bottom: 16px;
}

.captcha-image {
  height: 32px;
  border-radius: 4px;
  cursor: pointer;
}

:deep(.van-tabs__line) {
  background-color: #1989fa;
}

:deep(.van-tab--active) {
  color: #1989fa;
  font-weight: 500;
}
</style>

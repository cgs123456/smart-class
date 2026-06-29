import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.smartclass.app',
  appName: '智云星课',
  webDir: 'dist',
  bundledWebRuntime: false,
  server: {
    androidScheme: 'https',
    // 禁止明文 HTTP 流量，强制所有请求走 HTTPS
    // 避免会话 Cookie / Authorization 头被中间人窃听
    cleartext: false,
  },
  android: {
    buildOptions: {
      keystorePath: process.env.CAPACITOR_KEYSTORE_PATH || '',
      keystorePassword: process.env.CAPACITOR_KEYSTORE_PASSWORD || '',
      keystoreAlias: process.env.CAPACITOR_KEYSTORE_ALIAS || 'smartclass',
      keystoreAliasPassword: process.env.CAPACITOR_KEYSTORE_ALIAS_PASSWORD || '',
      releaseType: 'AAB',
    },
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 3000,
      launchAutoHide: true,
      backgroundColor: '#E8F2FC',
      splashImmersive: true,
      splashFullScreen: true,
    },
    Camera: {
      permissions: ['camera', 'photos'],
    },
    CapacitorHttp: {
      enabled: true,
    },
  },
};

export default config;

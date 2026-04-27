# WatchChat / 腕聊

WatchChat 是一个面向 Android 手机和 Wear OS 手表的简易聊天 App。

第一版目标：

- 手机端注册 / 登录
- 手机端一对一文字聊天
- 手表端查看消息
- 手表端快捷回复
- 手表端语音输入转文字
- 后续增加直接发送语音消息
- 使用 Supabase 保存用户、会话、消息和语音文件

## 项目结构

```text
watchchat-app
├── mobile-app      Android 手机端 App
├── wear-app        Wear OS 手表端 App
├── shared          公共数据模型与工具代码
├── backend-docs    Supabase 配置说明
└── .github         GitHub Actions 自动打包 APK
```

## 第一版功能范围

### 手机端

- 邮箱 + 密码登录
- 邮箱 + 密码注册
- 聊天列表
- 聊天详情页
- 发送文字消息
- 接收文字消息
- 云端保存聊天记录

### 手表端

- 查看最近聊天
- 查看消息内容
- 快捷回复：好的、收到、等一下、马上、在忙
- 语音输入转文字
- 收到消息震动提醒

### 云端

- Supabase Auth：账号登录
- Supabase Database：保存用户、会话、消息
- Supabase Realtime：实时消息同步
- Supabase Storage：后续保存语音文件

## 暂不包含

第一版暂时不做：

- 群聊
- 图片消息
- 视频消息
- 朋友圈
- 红包
- 复杂好友系统
- 正式应用商店上架

## 开发路线

1. 创建项目骨架
2. 配置 GitHub Actions 自动打包 APK
3. 创建 Supabase 数据库表
4. 手机端登录 / 注册
5. 手机端聊天列表和聊天窗口
6. 接入 Supabase 实时消息
7. 手表端查看消息和快捷回复
8. 手表端语音输入
9. 增加语音消息录制、上传和播放

## 当前状态

当前仓库处于第 1 步：项目骨架初始化。

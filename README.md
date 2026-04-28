# WatchChat 聊天 App

[![Android 调试构建](https://github.com/temple3fky-hue/watchchat-app/actions/workflows/android-debug-build.yml/badge.svg)](https://github.com/temple3fky-hue/watchchat-app/actions/workflows/android-debug-build.yml)

WatchChat 是一个面向 **Android 手机** 和 **Wear OS 手表** 的轻量聊天 App。项目目标是先完成可运行的聊天基础版本，再逐步接入 Supabase 实时同步、语音消息和手表联动。

## 当前状态

```text
手机端：登录 / 注册 / 聊天列表 / 新建聊天 / 文字消息
手表端：最近聊天 / 消息查看 / 快捷回复 / 语音输入 / 震动提醒
后端：Supabase 表结构、RLS 策略和基础仓库代码
构建：GitHub Actions Debug APK 自动构建
```

最近已整理：

- 登录页文案改为“登录你的聊天账号”。
- 本地假登录支持持久化，杀后台后不会自动掉登录。
- Supabase Auth 会等待本地 Session 恢复完成后再判断登录状态。

## 项目结构

```text
watchchat-app/
├── mobile-app/       Android 手机端 App
├── wear-app/         Wear OS 手表端 App
├── shared/           手机端和手表端共用数据模型
├── backend-docs/     Supabase 数据库和配置文档
├── .github/          GitHub Actions 自动构建配置
├── local.properties.example
├── settings.gradle.kts
└── README.md
```

## 功能清单

### 手机端 mobile-app

- Compose UI
- 登录 / 注册页面
- Supabase Auth 登录 / 注册入口
- 未配置 Supabase 时使用本地假登录
- 本地假登录状态持久化
- App 启动时自动检查登录状态
- 退出登录
- 最近聊天列表
- 新建聊天
- 新建聊天时可填写对方邮箱
- 聊天详情页
- 发送文字消息
- 本地消息状态更新
- 聊天列表最后一条消息预览更新
- Supabase 聊天数据读取 / 写入雏形
- 消息自动刷新，目前使用 3 秒轮询

### 手表端 wear-app

- Wear OS 最近聊天列表
- 未读角标
- 消息详情页
- 快捷回复：好的、收到、等一下、马上、在忙
- 系统语音输入 Intent
- 语音识别结果作为文字回复发送
- 语音输入状态提示
- 快捷回复成功震动
- 语音输入点击震动
- 语音失败 / 取消错误震动
- 模拟收到新消息
- 收到新消息震动提醒
- 进入聊天后标记已读

手表端 Supabase 聊天列表（第一阶段）限制：

- 仅实现“最近聊天列表”读取，依赖 wear-app 自身已有 Supabase Auth Session。
- 如果未配置 Supabase、未登录或读取失败，手表端会自动回退显示本地假聊天数据。
- 发送消息、语音上传、语音播放暂未纳入该阶段。

### shared

共用数据模型：

```text
WatchChatUser
Chat
Message
MessageType
MessageStatus
```

### backend-docs

后端文档包含：

```text
backend-docs/supabase-schema.sql
backend-docs/supabase-setup.md
```

主要内容：

- `profiles` 用户资料表
- `chats` 聊天表
- `chat_members` 聊天成员表
- `messages` 消息表
- RLS 行级安全策略
- Realtime 开启说明
- `voice-messages` Storage bucket 规划

## 快速运行

### 1. 克隆项目

```bash
git clone https://github.com/temple3fky-hue/watchchat-app.git
cd watchchat-app
```

### 2. 用 Android Studio 打开

用 Android Studio 打开项目根目录，等待 Gradle Sync 完成。

### 3. 运行手机端

选择运行配置：

```text
mobile-app
```

未配置 Supabase 时也能运行，会自动使用本地假登录和假聊天数据。

### 4. 运行手表端

选择运行配置：

```text
wear-app
```

手表端目前主要用于验证 Wear OS UI、快捷回复、语音输入和震动提醒。

## Supabase 配置

### 1. 创建 Supabase 项目

在 Supabase 控制台创建项目，记录：

```text
Project URL
anon public key
```

### 2. 执行数据库 SQL

进入 Supabase：

```text
SQL Editor → New query
```

执行：

```text
backend-docs/supabase-schema.sql
```

### 3. 配置 Android 本地密钥

复制示例文件：

```bash
cp local.properties.example local.properties
```

填入你的 Supabase 信息：

```properties
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your-anon-public-key
```

注意：不要把 `service_role` key 放进 Android App，也不要提交真实的 `local.properties`。

### 4. 本地假登录模式

如果暂时不配置 Supabase，可以在本地 Gradle 属性里开启：

```properties
WATCHCHAT_ALLOW_DEMO_MODE=true
```

开启后，登录页会使用本地假登录，方便先调试 UI 和聊天流程。

## GitHub Actions 自动打包

工作流文件：

```text
.github/workflows/android-debug-build.yml
```

触发方式：

```text
push 到 main
pull request 到 main
手动 workflow_dispatch
```

构建任务：

```text
:mobile-app:assembleDebug
:wear-app:assembleDebug
```

构建成功后会上传：

```text
mobile-app-debug-apk
wear-app-debug-apk
```

## 测试重点

每次改登录相关代码后，建议测试：

```text
1. 打开 App
2. 登录账号
3. 进入聊天列表
4. 从后台划掉 App
5. 重新打开 App
6. 确认可以直接进入聊天列表，不会回到登录页
```

每次改聊天相关代码后，建议测试：

```text
1. 新建聊天
2. 进入聊天详情
3. 发送一条文字消息
4. 返回聊天列表
5. 确认最后一条消息预览更新
```

## 下一步计划

1. 补齐 Gradle Wrapper，让本地和 CI 使用同一套 Gradle 版本
2. 把手机端 3 秒轮询替换成 Supabase Realtime channel
3. 手表端接入手机端同步数据
4. 增加真正的语音消息录制
5. 上传语音文件到 Supabase Storage
6. 聊天详情页支持播放语音消息
7. 优化聊天列表刷新和错误提示
8. 增加基础测试和构建检查

## 第一版暂不包含

- 群聊
- 图片消息
- 视频消息
- 朋友圈
- 红包
- 复杂好友系统
- 正式应用商店上架

## 开发提醒

- 真实 Supabase 密钥只放在 `local.properties`。
- 不要提交 `local.properties`。
- 不要把 `service_role` key 放进 Android App。
- 修改代码后先运行 `mobile-app`，再检查 GitHub Actions 是否通过。

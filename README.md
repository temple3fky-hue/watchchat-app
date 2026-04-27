# WatchChat / 腕聊

WatchChat 是一个面向 Android 手机和 Wear OS 手表的简易聊天 App。

第一版目标：手机端完成账号登录、聊天列表、文字聊天；手表端完成最近聊天、消息查看、快捷回复、语音输入转文字和震动提醒。

## 项目结构

```text
watchchat-app
├── mobile-app      Android 手机端 App
├── wear-app        Wear OS 手表端 App
├── shared          公共数据模型
├── backend-docs    Supabase 配置说明和 SQL
└── .github         GitHub Actions 自动打包 APK
```

## 当前已完成功能

### 手机端 mobile-app

- Compose 启动页面
- 登录 / 注册 UI
- Supabase Auth 登录 / 注册入口
- 未配置 Supabase 时自动走本地假登录
- 注册成功后写入 `profiles` 用户资料表
- App 启动时自动检查已有登录会话
- 退出登录
- 最近聊天列表
- 新建聊天
- 新建聊天时可填写对方邮箱
- 聊天详情页
- 发送文字消息
- 本地消息状态更新
- 聊天列表最后一条消息预览更新
- Supabase 聊天仓库雏形
- Supabase `chats` / `chat_members` / `messages` 读取和写入雏形
- 消息自动刷新，目前使用 3 秒轮询，后续可替换为真正 Realtime channel

### 手表端 wear-app

- Wear OS 最近聊天列表
- 未读角标
- 消息详情页
- 快捷回复：好的、收到、等一下、马上、在忙
- 系统语音输入 Intent
- 语音识别文字自动作为回复发送
- 语音输入状态提示
- 快捷回复成功震动
- 语音输入点击震动
- 语音失败 / 取消错误震动
- 模拟收到新消息
- 收到新消息震动提醒
- 进入聊天后标记已读

### shared

公共数据模型：

```text
WatchChatUser
Chat
Message
MessageType
MessageStatus
```

### Supabase 后端文档

已添加：

```text
backend-docs/supabase-schema.sql
backend-docs/supabase-setup.md
```

包含：

- `profiles` 用户资料表
- `chats` 聊天表
- `chat_members` 聊天成员表
- `messages` 消息表
- RLS 行级安全策略
- Realtime 开启说明
- `voice-messages` Storage bucket 规划

## 运行方式

### 1. 克隆项目

```bash
git clone https://github.com/temple3fky-hue/watchchat-app.git
cd watchchat-app
```

### 2. 用 Android Studio 打开

使用 Android Studio 打开项目根目录，然后等待 Gradle 同步完成。

### 3. 运行手机端

选择运行配置：

```text
mobile-app
```

手机端未配置 Supabase 时也能运行，会自动使用本地假登录和假聊天数据。

### 4. 运行手表端

选择运行配置：

```text
wear-app
```

手表端目前使用本地假数据，主要用于验证 Wear OS UI、快捷回复、语音输入和震动提醒。

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

复制：

```text
local.properties.example
```

改名为：

```text
local.properties
```

填入：

```properties
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your-anon-public-key
```

注意：不要把 `service_role` key 放进 Android App。

## 当前开发状态

当前项目已经从“项目骨架”推进到：

```text
手机端：可登录 / 注册 / 新建聊天 / 发送文字消息
手表端：可查看聊天 / 快捷回复 / 语音输入 / 震动提醒
后端：Supabase 表结构和基础仓库代码已搭建
```

仍有部分功能是开发版实现：

- 手机端消息同步目前是 3 秒轮询，不是真正 Realtime channel
- 手表端数据目前是本地假数据，尚未和手机端或 Supabase 同步
- 语音消息目前是语音转文字回复，尚未做音频录制、上传和播放

## 下一步计划

1. 把手机端 3 秒轮询替换成 Supabase Realtime channel
2. 手表端接入手机端同步数据
3. 增加真正的语音消息录制
4. 上传语音文件到 Supabase Storage
5. 聊天详情页支持播放语音消息
6. 优化聊天列表刷新和错误提示
7. 增加基础测试和 GitHub Actions 打包检查

## 暂不包含

第一版暂时不做：

- 群聊
- 图片消息
- 视频消息
- 朋友圈
- 红包
- 复杂好友系统
- 正式应用商店上架

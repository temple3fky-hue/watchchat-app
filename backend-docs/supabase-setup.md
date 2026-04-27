# WatchChat Supabase 配置说明

本文档记录 WatchChat 第一版需要的 Supabase 后端配置。

## 1. 创建 Supabase 项目

1. 打开 Supabase 控制台。
2. 新建项目。
3. 记录项目的 `Project URL` 和 `anon public key`。
4. 后续 Android App 会使用这两个值连接 Supabase。

## 2. 开启邮箱登录

在 Supabase 控制台中进入：

```text
Authentication → Providers → Email
```

确认 Email 登录已开启。

开发阶段可以先关闭邮箱验证，方便测试：

```text
Authentication → Providers → Email → Confirm email
```

正式上线前建议重新开启邮箱验证。

## 3. 创建数据库表

进入：

```text
SQL Editor → New query
```

复制并执行：

```text
backend-docs/supabase-schema.sql
```

这个 SQL 会创建：

```text
profiles       用户资料
chats          聊天会话
chat_members   会话成员
messages       聊天消息
```

并开启 Row Level Security，确保用户只能访问自己参与的聊天和消息。

## 4. Realtime 实时消息

进入：

```text
Database → Replication
```

打开 `messages` 表的 Realtime。

第一版手机端接入时，需要监听当前聊天的 `messages` 新增事件。

## 5. 语音消息存储

后续做语音消息时，进入：

```text
Storage → New bucket
```

创建 bucket：

```text
voice-messages
```

建议设置为 private bucket，后续通过登录用户权限读取。

## 6. Android 端需要配置的值

后续在 Android 项目中需要加入：

```text
SUPABASE_URL=你的 Project URL
SUPABASE_ANON_KEY=你的 anon public key
```

不要把 service_role key 放进 Android App。

## 7. 当前开发状态

当前项目还没有真正接入 Supabase SDK。

目前 App 使用：

```text
FakeChatRepository
```

提供本地假聊天数据。

下一步可以做：

```text
1. 添加 Supabase Android 依赖
2. 新建 SupabaseClientProvider
3. 把假登录替换成 Supabase Auth 登录 / 注册
4. 把 FakeChatRepository 替换成 SupabaseChatRepository
```

-- WatchChat Supabase schema
-- 在 Supabase SQL Editor 中执行本文件。
-- 目标：账号资料、一对一聊天、文字消息、后续语音消息。

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null,
  display_name text not null default 'WatchChat 用户',
  avatar_url text,
  created_at timestamptz not null default now()
);

create table if not exists public.chats (
  id uuid primary key default gen_random_uuid(),
  title text not null default '',
  created_by uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.chat_members (
  chat_id uuid not null references public.chats(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  joined_at timestamptz not null default now(),
  primary key (chat_id, user_id)
);

create table if not exists public.messages (
  id uuid primary key default gen_random_uuid(),
  chat_id uuid not null references public.chats(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  content text not null default '',
  message_type text not null default 'text' check (message_type in ('text', 'voice')),
  status text not null default 'sent' check (status in ('sending', 'sent', 'failed', 'read')),
  voice_url text,
  voice_duration_seconds integer,
  created_at timestamptz not null default now(),
  read_at timestamptz
);

create index if not exists idx_chat_members_user_id on public.chat_members(user_id);
create index if not exists idx_messages_chat_id_created_at on public.messages(chat_id, created_at);
create index if not exists idx_messages_sender_id on public.messages(sender_id);

create or replace function public.touch_chat_updated_at()
returns trigger
language plpgsql
as $$
begin
  update public.chats
  set updated_at = now()
  where id = new.chat_id;
  return new;
end;
$$;

drop trigger if exists trg_messages_touch_chat_updated_at on public.messages;
create trigger trg_messages_touch_chat_updated_at
after insert on public.messages
for each row execute function public.touch_chat_updated_at();

alter table public.profiles enable row level security;
alter table public.chats enable row level security;
alter table public.chat_members enable row level security;
alter table public.messages enable row level security;

-- profiles

drop policy if exists "profiles_select_public" on public.profiles;
create policy "profiles_select_public"
on public.profiles for select
using (true);

drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own"
on public.profiles for insert
with check (auth.uid() = id);

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
on public.profiles for update
using (auth.uid() = id)
with check (auth.uid() = id);

-- chats

drop policy if exists "chats_select_member" on public.chats;
create policy "chats_select_member"
on public.chats for select
using (
  exists (
    select 1 from public.chat_members cm
    where cm.chat_id = chats.id
      and cm.user_id = auth.uid()
  )
);

drop policy if exists "chats_insert_creator" on public.chats;
create policy "chats_insert_creator"
on public.chats for insert
with check (created_by = auth.uid());

drop policy if exists "chats_update_member" on public.chats;
create policy "chats_update_member"
on public.chats for update
using (
  exists (
    select 1 from public.chat_members cm
    where cm.chat_id = chats.id
      and cm.user_id = auth.uid()
  )
);

-- chat_members

drop policy if exists "chat_members_select_member" on public.chat_members;
create policy "chat_members_select_member"
on public.chat_members for select
using (
  exists (
    select 1 from public.chat_members me
    where me.chat_id = chat_members.chat_id
      and me.user_id = auth.uid()
  )
);

drop policy if exists "chat_members_insert_creator_or_self" on public.chat_members;
create policy "chat_members_insert_creator_or_self"
on public.chat_members for insert
with check (
  user_id = auth.uid()
  or exists (
    select 1 from public.chats c
    where c.id = chat_members.chat_id
      and c.created_by = auth.uid()
  )
);

-- messages

drop policy if exists "messages_select_chat_member" on public.messages;
create policy "messages_select_chat_member"
on public.messages for select
using (
  exists (
    select 1 from public.chat_members cm
    where cm.chat_id = messages.chat_id
      and cm.user_id = auth.uid()
  )
);

drop policy if exists "messages_insert_sender_is_member" on public.messages;
create policy "messages_insert_sender_is_member"
on public.messages for insert
with check (
  sender_id = auth.uid()
  and exists (
    select 1 from public.chat_members cm
    where cm.chat_id = messages.chat_id
      and cm.user_id = auth.uid()
  )
);

drop policy if exists "messages_update_chat_member" on public.messages;
create policy "messages_update_chat_member"
on public.messages for update
using (
  exists (
    select 1 from public.chat_members cm
    where cm.chat_id = messages.chat_id
      and cm.user_id = auth.uid()
  )
);

-- 后续语音消息使用 Supabase Storage。
-- 建议创建私有 bucket：voice-messages

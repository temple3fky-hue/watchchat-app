-- WatchChat 好友系统迁移 SQL
-- 已经执行过基础 supabase-schema.sql 的项目，只需要再执行本文件。
-- 作用：创建好友表、好友 RLS、自动补齐 profiles 用户资料。

create extension if not exists pgcrypto;

-- 确保 profiles 表存在。
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null,
  display_name text not null default 'WatchChat 用户',
  avatar_url text,
  created_at timestamptz not null default now()
);

-- 给新注册用户自动创建资料，避免“没有找到这个邮箱”。
create or replace function public.handle_new_user_profile()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, display_name)
  values (
    new.id,
    coalesce(new.email, ''),
    coalesce(new.raw_user_meta_data ->> 'display_name', split_part(coalesce(new.email, 'WatchChat 用户'), '@', 1), 'WatchChat 用户')
  )
  on conflict (id) do update
  set email = excluded.email,
      display_name = coalesce(nullif(public.profiles.display_name, ''), excluded.display_name);

  return new;
end;
$$;

drop trigger if exists on_auth_user_created_profile on auth.users;
create trigger on_auth_user_created_profile
after insert on auth.users
for each row execute function public.handle_new_user_profile();

-- 补齐已经注册过但 profiles 缺失的用户。
insert into public.profiles (id, email, display_name)
select
  u.id,
  coalesce(u.email, ''),
  coalesce(split_part(coalesce(u.email, 'WatchChat 用户'), '@', 1), 'WatchChat 用户')
from auth.users u
on conflict (id) do update
set email = excluded.email,
    display_name = coalesce(nullif(public.profiles.display_name, ''), excluded.display_name);

create table if not exists public.friendships (
  id uuid primary key default gen_random_uuid(),
  requester_id uuid not null references auth.users(id) on delete cascade,
  addressee_id uuid not null references auth.users(id) on delete cascade,
  status text not null default 'pending' check (status in ('pending', 'accepted', 'rejected')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (requester_id <> addressee_id)
);

create unique index if not exists idx_friendships_unique_pair
on public.friendships (
  least(requester_id, addressee_id),
  greatest(requester_id, addressee_id)
);

create index if not exists idx_friendships_requester_id on public.friendships(requester_id);
create index if not exists idx_friendships_addressee_id on public.friendships(addressee_id);
create index if not exists idx_friendships_status on public.friendships(status);

create or replace function public.touch_friendship_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_friendships_touch_updated_at on public.friendships;
create trigger trg_friendships_touch_updated_at
before update on public.friendships
for each row execute function public.touch_friendship_updated_at();

alter table public.profiles enable row level security;
alter table public.friendships enable row level security;

-- profiles：允许登录用户按邮箱查找用户。
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

-- friendships：只能看到和自己有关的好友关系。
drop policy if exists "friendships_select_own" on public.friendships;
create policy "friendships_select_own"
on public.friendships for select
using (
  requester_id = auth.uid()
  or addressee_id = auth.uid()
);

drop policy if exists "friendships_insert_requester" on public.friendships;
create policy "friendships_insert_requester"
on public.friendships for insert
with check (
  requester_id = auth.uid()
  and requester_id <> addressee_id
  and status = 'pending'
);

drop policy if exists "friendships_update_own" on public.friendships;
create policy "friendships_update_own"
on public.friendships for update
using (
  requester_id = auth.uid()
  or addressee_id = auth.uid()
)
with check (
  requester_id = auth.uid()
  or addressee_id = auth.uid()
);

-- 强制刷新 PostgREST schema cache。
notify pgrst, 'reload schema';

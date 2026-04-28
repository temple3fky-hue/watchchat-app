-- WatchChat 好友系统迁移 SQL
-- 已经执行过基础 supabase-schema.sql 的项目，只需要再执行本文件。

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

alter table public.friendships enable row level security;

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

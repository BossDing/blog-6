--6.4
alter table blog_fragment_user add column if not exists is_del  boolean not null default false;
alter table blog_news add column if not exists news_hits  int not null default 0;
--6.6
alter table blog_common_file drop column if exists file_width;
alter table blog_common_file drop column if exists file_height;
alter table blog_news add column if not exists news_lock  varchar(40);
--7.0
alter table blog_page_user add column if not exists is_enable boolean not null default true;
alter table blog_fragment_user add column if not exists is_enable boolean not null default true;
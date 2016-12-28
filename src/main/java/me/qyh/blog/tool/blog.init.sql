/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
drop table if exists `blog_article`;
CREATE TABLE IF NOT EXISTS `blog_article` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `content` mediumtext  NOT NULL,
  `pubDate` datetime DEFAULT NULL,
  `lastModifyDate` datetime DEFAULT NULL,
  `title` varchar(200)  NOT NULL,
  `isPrivate` tinyint(1) NOT NULL DEFAULT '0',
  `hits` int(11) NOT NULL DEFAULT '0',
  `summary` varchar(3000)  NOT NULL,
  `art_level` int(11) DEFAULT NULL,
  `art_status` int(11) NOT NULL DEFAULT '0',
  `art_from` int(11) NOT NULL DEFAULT '0',
  `editor` int(11) NOT NULL DEFAULT '0',
  `space_id` int(11) NOT NULL,
  `art_lock` varchar(40)  DEFAULT NULL,
  `art_alias` varchar(60)  DEFAULT NULL,
  `allowComment` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
);
drop table if exists `blog_article_tag`;
CREATE TABLE IF NOT EXISTS `blog_article_tag` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `article_id` int(11) NOT NULL,
  `tag_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
);

drop table if exists `blog_comment`;
CREATE TABLE IF NOT EXISTS `blog_comment` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parent_id` int(11) DEFAULT NULL,
  `parent_path` varchar(255)  NOT NULL,
  `comment_nickname` varchar(20)  DEFAULT NULL,
  `comment_email` varchar(255)  DEFAULT NULL,
  `comment_ip` varchar(255)  NOT NULL,
   `comment_admin` tinyint(1)  NOT NULL DEFAULT '0',
  `content` varchar(2000)  NOT NULL,
  `article_id` int(11) NOT NULL,
  `comment_date` datetime NOT NULL,
  `comment_status` int(11) NOT NULL,
  comment_website varchar(255),
  COMMENT_GRAVATAR varchar(255),
  PRIMARY KEY (`id`)
) ;

drop table if exists `blog_comment_module`;
CREATE TABLE IF NOT EXISTS `blog_comment_module` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parent_id` int(11) DEFAULT NULL,
  `parent_path` varchar(255)  NOT NULL,
  `comment_nickname` varchar(20)  DEFAULT NULL,
  `comment_email` varchar(255)  DEFAULT NULL,
  `comment_ip` varchar(255)  NOT NULL,
   `comment_admin` tinyint(1)  NOT NULL DEFAULT '0',
  `content` varchar(2000)  NOT NULL,
  `module_id` int(11) NOT NULL,
  `comment_date` datetime NOT NULL,
  `comment_status` int(11) NOT NULL,
  comment_website varchar(255),
  COMMENT_GRAVATAR varchar(255),
  PRIMARY KEY (`id`)
) ;

drop table if exists `blog_common_file`;
CREATE TABLE IF NOT EXISTS `blog_common_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `file_key` varchar(2000)  DEFAULT NULL,
  `file_extension` varchar(500)  NOT NULL,
  `file_size` int(11) NOT NULL,
  `file_store` int(11) NOT NULL,
  `file_originalname` varchar(500)  NOT NULL,
  `file_width` int(11) DEFAULT NULL,
  `file_height` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

drop table if exists `blog_file`;
CREATE TABLE IF NOT EXISTS `blog_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `file_name` varchar(500)  NOT NULL,
  `file_parent` int(11) DEFAULT NULL,
  `file_type` int(11) NOT NULL DEFAULT '0',
  `file_createDate` datetime NOT NULL,
  `common_file` int(11) DEFAULT NULL,
  `file_lft` int(11) NOT NULL,
  `file_rgt` int(11) NOT NULL,
  `file_lastmodifydate` datetime DEFAULT NULL,
  `file_path` varchar(500)  NOT NULL,
  PRIMARY KEY (`id`)
) ;

drop table if exists `blog_file_delete`;
CREATE TABLE IF NOT EXISTS `blog_file_delete` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `file_store` int(11) DEFAULT NULL,
  `file_key` varchar(2000)  NOT NULL,
  `file_type` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ;

drop table if exists `blog_fragment_user`;
CREATE TABLE IF NOT EXISTS `blog_fragment_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fragment_name` varchar(20)  NOT NULL,
  `fragment_description` varchar(500)  NOT NULL,
  `fragment_tpl` text  NOT NULL,
  `fragment_create_date` datetime NOT NULL,
  `space_id` int(11) DEFAULT NULL,
  `is_global` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ;

drop table if exists `blog_lock`;
CREATE TABLE IF NOT EXISTS `blog_lock` (
  `id` varchar(40)  NOT NULL,
  `lock_type` int(11) NOT NULL DEFAULT '0',
  `lock_password` varchar(100)  DEFAULT NULL,
  `lock_name` varchar(20)  NOT NULL,
  `createDate` datetime NOT NULL,
  `lock_question` text ,
  `lock_answers` varchar(500)  DEFAULT NULL,
  PRIMARY KEY (`id`)
) ;

drop table if exists `blog_page_error`;
CREATE TABLE IF NOT EXISTS `blog_page_error` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `space_id` int(11) DEFAULT NULL,
  `error_code` int(11) NOT NULL,
  `page_tpl` mediumtext  NOT NULL,
  PRIMARY KEY (`id`)
);

drop table if exists `blog_page_expanded`;
CREATE TABLE IF NOT EXISTS `blog_page_expanded` (
  `id` int(11) NOT NULL,
  `page_name` varchar(20)  NOT NULL,
  `page_tpl` mediumtext  NOT NULL,
  PRIMARY KEY (`id`)
) ;

drop table if exists `blog_page_lock`;
CREATE TABLE IF NOT EXISTS `blog_page_lock` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `space_id` int(11) DEFAULT NULL,
  `page_tpl` mediumtext  NOT NULL,
  `page_locktype` varchar(20)  NOT NULL,
  PRIMARY KEY (`id`)
) ;

drop table if exists `blog_page_sys`;
CREATE TABLE  IF NOT EXISTS `blog_page_sys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `space_id` int(11) DEFAULT NULL,
  `page_tpl` mediumtext  NOT NULL,
  `page_target` int(11) NOT NULL,
  PRIMARY KEY (`id`)
);

drop table if exists `blog_page_user`;
CREATE TABLE IF NOT EXISTS `blog_page_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `page_tpl` mediumtext  NOT NULL,
  `page_name` varchar(20)  NOT NULL,
  `page_description` varchar(500)  NOT NULL,
  `space_id` int(11) DEFAULT NULL,
  `page_create_date` datetime NOT NULL,
  `page_alias` varchar(40)  DEFAULT NULL,
  PRIMARY KEY (`id`)
) ;

drop table if exists `blog_space`;
CREATE TABLE IF NOT EXISTS `blog_space` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `space_alias` varchar(20)  NOT NULL,
  `createDate` datetime NOT NULL,
  `space_name` varchar(20)  NOT NULL,
  `space_lock` varchar(40)  DEFAULT NULL,
  `is_private` tinyint(1) NOT NULL DEFAULT '0',
  `is_default` tinyint(1) NOT NULL DEFAULT '0',
  `art_pagesize` int(11) NOT NULL DEFAULT 10,
  PRIMARY KEY (`id`),
  UNIQUE KEY `space_alias` (`space_alias`),
  UNIQUE KEY `space_name` (`space_name`)
) ;

drop table if exists `blog_tag`;
CREATE TABLE IF NOT EXISTS `blog_tag` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(20)  NOT NULL,
  `create_date` datetime NOT NULL,
  PRIMARY KEY (`id`)
);
ALTER TABLE blog_article ADD COLUMN comment_check BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE blog_comment ADD COLUMN comment_status INT NOT NULL DEFAULT 0;
DROP TABLE blog_widget_user;
DROP TABLE blog_widget_tpl;
CREATE TABLE `blog_fragement_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fragement_name` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fragement_description` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fragement_tpl` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `fragement_create_date` datetime NOT NULL,
  `space_id` int(11) DEFAULT NULL,
  `is_global` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `fragement_name` (`fragement_name`),
  KEY `sfk` (`space_id`),
  CONSTRAINT `sfk` FOREIGN KEY (`space_id`) REFERENCES `blog_space` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE blog_article ADD COLUMN art_alias VARCHAR(60);

DROP TABLE blog_fragement_user`;

CREATE TABLE `blog_fragment_user` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `fragment_name` VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fragment_description` VARCHAR(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fragment_tpl` TEXT COLLATE utf8mb4_unicode_ci NOT NULL,
  `fragment_create_date` DATETIME NOT NULL,
  `space_id` INT(11) DEFAULT NULL,
  `is_global` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `space_id` (`space_id`),
  CONSTRAINT `blog_fragment_user_ibfk_1` FOREIGN KEY (`space_id`) REFERENCES `blog_space` (`id`)
) ENGINE=INNODB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci

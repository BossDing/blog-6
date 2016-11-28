# blog5
钱宇豪的个人博客

## 采用 spring4&mybaits&thymeleaf开发

# www.qyh.me

一个**单用户**博客，特性如下：
* 放弃了博客文章的分类属性，采用了space替代，一个space代表了一种分类，space可以被独立的子域名所访问
* 可覆盖的页面模板和挂件模板、自定义页面、拓展页面、自定义挂件以及错误码页面(这些需要对thymeleaf有一定的了解)，这意味着可以对每一个space都设置一套模板(一些公用的页面除外)
* 文章置顶、定时发布以及草稿箱
* lucene搜索
* CKEDITOR|markdown
* 空间和文章锁保护
* 基于oauth的评论系统，评论|回复邮件通知
* RSS订阅
* 文件管理，七牛云存储，实时缩略图服务
* metaweblog api支持

### doc: http://doc.qyh.me


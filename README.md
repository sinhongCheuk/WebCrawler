# WebCrawler 项目说明文档

本项目基于 Java 实现了对多个网站的定向爬虫，包括豆瓣 Top250、Bilibili 排行榜、Missav 视频站点等。实现了静态解析（Jsoup）、动态渲染（Selenium）、反爬机制绕过、多标签页提取、Redis 排行榜、MySQL 数据持久化等功能。

---

## 项目结构说明

WebCrawler/
├── Bilibili/
│   ├── BilibiliMovieSelenium.java       # B站电影榜爬虫（Selenium）
│   └── MovieDao.java                    # 数据写入 MySQL
├── Douban/
│   ├── DoubanMovieCrawler.java          # 豆瓣 Top250 爬虫（Jsoup）
│   └── DoubanRankService.java           # Redis 排行榜服务
├── Missav/
│   ├── MissavCrawler.java               # 核心爬虫，多标签页提取 + 验证绕过
│   ├── MissavDao.java                   # MySQL 写入 DAO
│   ├── MissavMovie.java                 # 实体类映射数据库结构
│   ├── MissavQueryService.java          # MySQL 查询逻辑封装
│   └── MissavDetailExtractor.java       # 影片详情页解析器
├── org.example/
│   ├── GraphCrawler.java                # 多线程网页结构图构建器
│   ├── JsoupCrawl.java                  # Jsoup 入门爬虫
│   └── ImageCrawler.java                # 多线程图片下载器
├── pom.xml                              # Maven 依赖管理
└── README.md                            # 项目说明文档

---

## 核心功能概览

### 1. Jsoup 静态页面爬虫（豆瓣 Top250）

- Jsoup 获取页面 DOM 并提取评分
- 使用 .absUrl() 补全链接
- 将评分数据写入 Redis 的 ZSet，实现排行榜

### 2. Selenium 动态页面爬虫（Bilibili）

- 页面为前后端分离，内容通过 JS 异步加载
- 使用 Selenium 等待渲染后提取视频标题、播放量、链接等信息
- 数据持久化到本地 MySQL 数据库

### 3. Missav 爬虫：动态渲染 + Cloudflare 验证绕过

- Cloudflare 使用 JavaScript Challenge 检测自动化行为
- 使用主标签页手动通过验证，子标签页复用登录态
- 多标签页并发提取详情页中的番号、演员、描述、封面等内容
- 每页 12 条数据，分页抓取 5 页即提取 60 个视频详情
- 所有字段写入 MySQL 的 missav_movies 表中

---

## 技术实现要点

### 反爬机制绕过

| 目标站点 | 反爬策略           | 解决方法                     |
|----------|--------------------|------------------------------|
| Missav   | Cloudflare JS 验证 | 主页面验证 + 标签页复用状态 |
| Bilibili | 动态渲染           | 使用 Selenium 模拟浏览器    |
| 豆瓣     | User-Agent 检测     | 设置浏览器 UA 模拟请求       |

---

## 数据库结构

sql
CREATE TABLE IF NOT EXISTS missav_movies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50),
    title VARCHAR(512),
    actors TEXT,
    duration VARCHAR(50),
    release_date DATE,
    cover_url VARCHAR(255),
    video_url VARCHAR(255),
    description TEXT,
    page_number INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

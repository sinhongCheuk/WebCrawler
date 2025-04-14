package org.example;

import org.apache.commons.io.FileUtils;                // 用于快速文件下载和保存
import org.jsoup.Jsoup;                                // Jsoup 是用于 HTML 解析的库
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageCrawler {

    // 最大递归深度，防止爬虫无限扩散
    private static final int MAX_DEPTH = 1;

    // 图片保存目录（请确保目录已存在）
    private static final String downloadDir = "/Users/xh/Desktop/temp/";

    // 创建线程池：线程数量为 CPU 核数的两倍，提高并发性能
    private static final ExecutorService executor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    // 已访问的网页链接，避免重复抓取（线程安全）
    private static final ConcurrentMap<String, Boolean> visited = new ConcurrentHashMap<>();

    // 活动任务计数器，用于动态跟踪任务总数（线程安全的计数）
    private static final AtomicInteger activeTasks = new AtomicInteger(0);

    // 主线程的等待锁，当所有任务完成时会触发解除阻塞
    private static final CountDownLatch allDoneLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        // 启动第一个爬虫任务（入口页面，深度为0）
        submitCrawl("https://www.szu.edu.cn/", 0);

        try {
            // 主线程阻塞，直到 activeTasks = 0 时 countDown 被触发
            allDoneLatch.await();
            System.out.println("所有页面和图片已处理完毕！");
        } catch (InterruptedException e) {
            System.out.println("主线程等待被中断");
        } finally {
            // 所有任务完成后关闭线程池
            executor.shutdown();
        }
    }

    /**
     * 提交一个新的页面爬取任务到线程池
     */
    private static void submitCrawl(String url, int depth) {
        activeTasks.incrementAndGet(); // 新增一个任务
        executor.submit(() -> crawl(url, depth));
    }

    /**
     * 核心方法：抓取页面内容 + 递归爬取链接 + 提交图片下载任务
     */
    private static void crawl(String url, int depth) {
        try {
            // 超过最大递归深度，或已经访问过的链接则直接跳过
            if (depth > MAX_DEPTH || visited.putIfAbsent(url, true) != null) return;

            System.out.println("Visiting: " + url);

            // 用 Jsoup 请求并解析 HTML 页面
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36") // 模拟浏览器
                    .timeout(8000)  // 设置超时时间（毫秒）
                    .get();

            // 解析所有图片标签 <img>，并提交下载任务
            Elements imgs = doc.select("img");
            for (Element img : imgs) {
                String src = img.absUrl("src");  // 获取图片的完整 URL
                if ((src.endsWith(".png") || src.endsWith(".jpg")) && src.contains("/images/")) {
                    executor.submit(() -> downloadImage(src));
                }
            }

            // 解析页面中的链接并递归爬取子页面
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String href = link.attr("href").trim();                         // 相对/绝对路径
                String fullLink = formatLink(href, getHost(url));              // 规范化为完整URL
                if (fullLink != null &&
                        fullLink.startsWith(getHost(url)) &&
                        !visited.containsKey(fullLink)) {
                    submitCrawl(fullLink, depth + 1);  // 递归提交新页面爬取
                }
            }

        } catch (Exception e) {
            System.out.println("访问失败: " + url);
        } finally {
            // 当前任务结束，计数器减1
            if (activeTasks.decrementAndGet() == 0) {
                allDoneLatch.countDown();  // 如果没有活跃任务了，唤醒主线程
            }
        }
    }

    /**
     * 下载图片并保存到本地目录
     */
    private static void downloadImage(String src) {
        try {
            // 提取文件名（如：on_03.png）
            String fileName = src.substring(src.lastIndexOf("/") + 1);
            File outputFile = new File(downloadDir + fileName);

            // 如果文件已存在，跳过下载
            if (outputFile.exists()) return;

            // 使用 commons-io 进行下载
            FileUtils.copyURLToFile(new URL(src), outputFile);
            System.out.println("下载成功: " + outputFile.getName());
        } catch (IOException e) {
            System.out.println("下载失败: " + src);
        }
    }

    /**
     * 从 URL 中提取 host 部分（协议 + 域名）
     * 例：输入 https://www.szu.edu.cn/news/abc.htm -> 输出 https://www.szu.edu.cn
     */
    private static String getHost(String url) {
        try {
            URL u = new URL(url);
            return u.getProtocol() + "://" + u.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将相对路径、特殊链接（如 /xxx 或 abc.html）转换成绝对链接
     */
    private static String formatLink(String link, String hostPrefix) {
        if (link.startsWith("http")) return link;                  // 绝对路径，直接返回
        if (link.startsWith("/")) return hostPrefix + link;        // 站内根目录路径
        if (!link.startsWith("#") && !link.startsWith("javascript")) return hostPrefix + "/" + link;
        return null; // 忽略锚点链接、JS脚本链接
    }
}

package org.example;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

/**
 * 多线程网页爬虫 + 图构建器
 * - 功能：递归爬取网页链接，构建网页之间的有向图
 * - 输出：
 *   1. edges.txt：记录顶点之间的有向边（编号\t编号）
 *   2. labels.txt：记录顶点编号与 URL 的映射（编号\tURL）
 */
public class GraphCrawler {

    // 最大爬取深度（从初始页面开始最多递归几层）
    private static final int MAX_DEPTH = 1;

    // 可选：图片保存目录（当前已注释图片功能）
    private static final String downloadDir = "/Users/xh/Desktop/temp/";

    // 图边输出文件
    private static final String edgeFile = "/Users/xh/Desktop/edges.txt";

    // 顶点编号 → URL 映射文件
    private static final String labelFile = "/Users/xh/Desktop/labels.txt";

    // 多线程线程池，线程数 = CPU核心数 * 2
    private static final ExecutorService executor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    // 记录已访问过的 URL，避免重复访问（线程安全）
    private static final ConcurrentMap<String, Boolean> visited = new ConcurrentHashMap<>();

    // 活动任务计数器，用于控制主线程何时退出
    private static final AtomicInteger activeTasks = new AtomicInteger(0);

    // 所有任务完成的标志（主线程通过它来阻塞/唤醒）
    private static final CountDownLatch allDoneLatch = new CountDownLatch(1);

    // URL 到顶点编号的映射表，如 https://xxx.com → 1、2、3...
    private static final ConcurrentMap<String, String> urlToId = new ConcurrentHashMap<>();

    // 顶点编号生成器，从 1 开始递增
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    // 写入边/标签文件的输出流（线程安全通过 synchronized 控制）
    private static BufferedWriter edgeWriter;
    private static BufferedWriter labelWriter;

    public static void main(String[] args) throws IOException {
        // 初始化文件写入器
        edgeWriter = new BufferedWriter(new FileWriter(edgeFile));
        labelWriter = new BufferedWriter(new FileWriter(labelFile));

        // 启动第一个网页爬取任务（初始深度为0）
        submitCrawl("https://www.szu.edu.cn/", 0);

        try {
            // 主线程阻塞，直到所有递归任务全部完成
            allDoneLatch.await();
            System.out.println("所有页面和链接图构建完毕！");
        } catch (InterruptedException e) {
            System.out.println("主线程被中断");
        } finally {
            // 所有任务完成后关闭资源
            executor.shutdown();
            edgeWriter.close();
            labelWriter.close();
        }
    }

    /**
     * 提交一个网页爬取任务
     */
    private static void submitCrawl(String url, int depth) {
        activeTasks.incrementAndGet(); // 活动任务数 +1
        executor.submit(() -> crawl(url, depth)); // 提交任务到线程池
    }

    /**
     * 爬取一个网页：
     * - 提取所有链接
     * - 为当前 URL 分配编号
     * - 写入边关系
     * - 递归子链接
     */
    private static void crawl(String url, int depth) {
        try {
            // 递归超过最大深度 或 已访问过该页面，直接返回
            // 等价于if (depth > MAX_DEPTH) return;
            // if (visited.containsKey(url)) return;
            // visited.put(url, true);
            if (depth > MAX_DEPTH || visited.putIfAbsent(url, true) != null) return;

            // 为当前 URL 分配顶点编号
            String fromId = getOrAssignId(url);

            System.out.println("Visiting: " + fromId + " -> " + url);

            // 使用 Jsoup 获取页面内容
            // Document 就是整个 <html>...</html> 的对象化形式
            // doc 是一个 org.jsoup.nodes.Document 对象
            // 它代表着从该 url 下载并解析后的 完整 HTML 文档结构
            // 类似于浏览器中按下 F12 → 右键“查看页面源代码”中的内容
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36")
                    .timeout(8000)
                    .get();

            // ↓↓↓ 如果你希望恢复图片爬取功能，可取消注释以下代码 ↓↓↓
            /*
            Elements imgs = doc.select("img");
            for (Element img : imgs) {
                String src = img.absUrl("src");
                if ((src.endsWith(".png") || src.endsWith(".jpg")) && src.contains("/images/")) {
                    executor.submit(() -> downloadImage(src));
                }
            }
            */

            // 抓取所有链接 <a href=...> 并构建边
            // select方法，获取所有 <a> 标签	doc.select("a")
            // 获取所有 <img> 标签	doc.select("img")
            // 从整个 HTML 文档中选取所有 <a> 标签，且含有 href 属性的标签
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                // link.absUrl("href") 会自动把相对路径（如 /news.html）补全为绝对路径（如 https://www.szu.edu.cn/news.html）
                //.trim() 用于去除多余的空格
                String href = link.absUrl("href").trim();

                String fullLink = formatLink(href, getHost(url));

                // 确保是本站链接（同域名）再爬
                // 安全检查：是否是本站内链
                // 防止爬虫跑到别的域名去
                // getHost(url) 提取的是当前页面所在域名（例如 https://www.szu.edu.cn）
                // 如果 fullLink 不是本站开头，直接跳过，不爬（外链、广告等）
                if (fullLink != null && fullLink.startsWith(getHost(url))) {
                    String toId = getOrAssignId(fullLink);  // 为目标 URL 分配编号
                    writeEdge(fromId, toId);                // 写入边 A → B

                    // 如果该链接还未爬取过，则递归爬取
                    if (!visited.containsKey(fullLink)) {
                        submitCrawl(fullLink, depth + 1);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("访问失败: " + url);
        } finally {
            // 当前任务完成，活动任务数 -1，如果为 0，唤醒主线程
            if (activeTasks.decrementAndGet() == 0) {
                allDoneLatch.countDown();
            }
        }
    }

    /**
     * 下载图片（功能可选，已默认关闭）
     */
    private static void downloadImage(String src) {
        try {
            String fileName = src.substring(src.lastIndexOf("/") + 1);
            File outputFile = new File(downloadDir + fileName);
            if (outputFile.exists()) return;

            FileUtils.copyURLToFile(new URL(src), outputFile);
            System.out.println("下载图片: " + outputFile.getName());
        } catch (IOException e) {
            System.out.println("下载失败: " + src);
        }
    }

    /**
     * 从 URL 中提取域名前缀
     * 例如 https://abc.com/xxx -> https://abc.com
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
     * 标准化链接格式：将相对链接/锚点链接转为完整 URL
     *
     * @param link 原始的链接（可能是相对路径或绝对路径）
     * @param hostPrefix 当前页面的主机前缀，例如：https://www.szu.edu.cn
     * @return 标准化后的完整 URL，如果不合法或是脚本链接则返回 null
     */
    private static String formatLink(String link, String hostPrefix) {
        // 说明链接本身已经是绝对路径
        // 直接返回即可，不做任何拼接处理
        if (link.startsWith("http")) return link;

        // 是以 / 开头的相对路径（相对于网站根目录）
        // 需要加上 host 前缀
        if (link.startsWith("/")) return hostPrefix + link;

        // 相对路径但不以 / 开头
        // 这是一些“相对路径”的形式，如 news.html 或 info/index.jsp
        // 我们补个 / 拼成完整路径
        // 同时过滤了两种不合法或无需跟踪的链接：
        // 锚点链接（#about、#top）
        // JavaScript 伪链接（如 javascript:void(0)）
        if (!link.startsWith("#") && !link.startsWith("javascript")) return hostPrefix + "/" + link;
        return null;
    }

    /**
     * 写入一条图的边（编号编号）到 edges.txt
     */
    private static void writeEdge(String from, String to) {
        try {
            synchronized (edgeWriter) {
                edgeWriter.write(from + "\t" + to + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 为每个唯一 URL 分配一个唯一编号（从1开始）
     * 并写入 labels.txt 文件中
     */
    private static String getOrAssignId(String url) {
        if (urlToId.containsKey(url)) {
            return urlToId.get(url);  // 已经有编号
        } else {
            int numericId = idCounter.incrementAndGet(); // 分配新编号
            String idStr = String.valueOf(numericId);

            try {
                synchronized (labelWriter) {
                    labelWriter.write(idStr + "\t" + url + "\n"); // 写入文件
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            urlToId.put(url, idStr);  // 保存编号
            return idStr;
        }
    }
}

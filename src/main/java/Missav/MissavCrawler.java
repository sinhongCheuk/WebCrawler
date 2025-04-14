package Missav;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
//import org.example.MissavMovieDao;
import java.util.*;

/**
 * MissavCrawler.java
 *
 * 核心功能：爬取 missav.ai 网站的“中文字幕”板块所有影片的信息（标题、番号、演员、时长、封面、上映时间、描述等），并持久化到 MySQL。
 *
 * 技术要点：
 * 1. 利用 Selenium 动态模拟浏览器行为。
 * 2. 使用“多标签页策略”逐个访问详情页，解决 Cloudflare 的人机验证问题。
 * 3. 遇到验证页时等待用户手动验证。
 * 4. 数据提取后通过 DAO 插入数据库。
 *
 * 遇到的核心困难：
 * - Missav 使用 Cloudflare 防护，识别并阻止爬虫行为。
 * - WebDriver 默认特征容易被检测（如 navigator.webdriver）。
 * - 页面加载过程涉及 JavaScript 渲染，Jsoup 无法使用。
 *
 * 解决方案概述：
 * - 设置 ChromeOptions 避免触发 Cloudflare 检测。
 * - 初始页面由用户手动验证后再批量打开子标签页。
 * - 使用主窗口认证结果复用到所有子标签页。
 * - 每个详情页数据提取后关闭该页，避免内存爆炸。
 */

public class MissavCrawler {

    public static void main(String[] args) throws Exception {
        // === 阶段一：配置反检测参数 ===
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation")); // 关闭“自动化控制”提示
        options.setExperimentalOption("useAutomationExtension", false); // 禁用自动化扩展插件
        options.addArguments("--disable-blink-features=AutomationControlled"); // 禁用Blink特征，减少特征暴露

        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();    // 最大化浏览器窗口，可去掉

        // 先进入主页并手动验证一次
        // === 阶段二：手动通过 Cloudflare 验证，避免每个页面都弹出验证 ===
        // 验证机制说明：
        // Cloudflare 使用 JavaScript Challenge 验证用户是否为真人（包括指纹检测、cookie验证、navigator.webdriver）
        // 若访问频繁或被识别为机器人，将弹出“请稍候...”页面并阻止加载数据(navigator.webdriver = true, 想办法变成undefined)

        // 绕过方式：我们让主窗口中通过一次验证，然后在已验证状态下打开多个标签页
        // 所有新标签页将继承主窗口 Cookie，因此可复用已验证状态抓取详情页
        // 注意到一进去https://missav.ai不会触发验证，随便点一个链接才会触发验证
        driver.get("https://missav.ai");
        spoof(driver); // 删除 navigator.webdriver，伪装成人类用户
        System.out.println("请在首页中手动通过验证后，按下回车继续...");
        System.in.read(); // 等待按回车

        // 控制要爬取的页数
        int totalPages = 5;

        for (int page = 1; page <= totalPages; page++) {
            // === 构造当前页 URL ===
            String pageUrl = "https://missav.ai/dm265/chinese-subtitle?sort=today_views";
            if (page > 1) pageUrl += "&page=" + page;

            driver.get(pageUrl);    // 访问"中文字幕"的第page页
            Thread.sleep(5000); // 手动让他"加载"一会
            System.out.println("====== 正在抓取第 " + page + " 页 ======");

            // === 获取该页中的视频缩略图组件 ===
            List<WebElement> videos = driver.findElements(By.cssSelector(".thumbnail"));
            System.out.println("第 " + page + " 页找到 " + videos.size() + " 个视频");

            // 提取当前页的每个视频预提取 link、封面、时长等基础信息
            List<MissavDetailExtractor.MissavVideo> videoList = new ArrayList<>();
            for (WebElement video : videos) {
                try {
                    String link = video.findElement(By.cssSelector("a")).getAttribute("href");
                    String duration = video.findElement(By.cssSelector("span.absolute.bottom-1.right-1")).getText();
                    String cover = video.findElement(By.cssSelector("img")).getAttribute("data-src");

                    MissavDetailExtractor.MissavVideo mv = new MissavDetailExtractor.MissavVideo();
                    mv.link = link;
                    mv.cover = cover;
                    mv.duration = duration;
                    videoList.add(mv);

                } catch (Exception e) {
                    System.out.println("提取封面/链接/时长失败，跳过");
                }
            }

            // === 阶段三：打开所有视频的详情页（多标签页方式）===
            // 遍历每个视频链接，使用 JS 在新标签页中打开
            JavascriptExecutor js = (JavascriptExecutor) driver;
            for (MissavDetailExtractor.MissavVideo mv : videoList) {
                js.executeScript("window.open(arguments[0])", mv.link);
                Thread.sleep(800);  // 控制标签页打开频率，防止卡死浏览器
            }

            // === 阶段四：提取每个详情页信息 ===
            // 获取所有窗口句柄，注意第一个是主窗口
            List<String> tabs = new ArrayList<>(driver.getWindowHandles());
            // 从第二个开始，依次切换标签页提取详情页数据
            for (int i = 1; i < tabs.size(); i++) {
                driver.switchTo().window(tabs.get(i));
                spoof(driver);  // 防止 Cloudflare 再次识别出 webdriver
                Thread.sleep(3000); // 页面加载等待

                // === 检查是否仍然处于 Cloudflare 验证页 ===
                String title = driver.getTitle();
                if (title.contains("請稍候") || title.contains("Just a moment")) {
                    System.out.println("第 " + i + " 个标签页触发验证，请手动完成并按下回车...");
                    System.in.read();
                    Thread.sleep(3000);
                }

                try {
                    // 取回之前记录的影片信息（如链接、封面、时长）
//                    System.out.println("----------------------------------");
                    MissavDetailExtractor.MissavVideo mv = videoList.get(i - 1);

                    // 提取详情页的 meta 信息
                    MissavDetailExtractor.MissavVideo filled = MissavDetailExtractor.extractDetail(driver, mv.duration, mv.cover, mv.link);
                    System.out.println(filled);

                    // 赋值，把变量filled的值赋给movie，封装为数据库实体类
                    // === 阶段五：构造实体并持久化 ===
                    MissavMovie movie = new MissavMovie();
                    movie.setCode(filled.code);
                    movie.setTitle(filled.title);
                    movie.setActors(filled.actors);
                    movie.setDuration(filled.duration);
                    movie.setReleaseDate(filled.date);
                    movie.setCoverUrl(filled.cover);
                    movie.setVideoUrl(filled.link);
                    movie.setDescription(filled.description);
                    movie.setPageNumber(page);  // 当前页码

                    // 插入数据库
                    MissavDao.insert(movie);

                    System.out.println("----------------------------------");
                } catch (Exception e) {
                    System.out.println("提取失败，跳过 tab " + i);
                }

                // === 关闭该详情页，释放资源 ===
                driver.close(); // 处理完后关闭当前标签
            }

            // 回到列表页
            // === 回到第一页标签 ===
            driver.switchTo().window(tabs.get(0));
        }

        // === 全部完成后关闭浏览器 ===
        driver.quit();
    }

    /**
     * 绕过检测的关键：
     * Cloudflare 会检测 navigator.webdriver 为 true（Selenium 的默认行为）。
     * 此处手动设置为 undefined，模拟真实用户环境。
     */
    private static void spoof(WebDriver driver) {
        ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
        );
    }
}


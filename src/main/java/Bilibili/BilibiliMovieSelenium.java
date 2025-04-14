package Bilibili;

import Bilibili.MovieDao;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;

/**
 *   Java 程序          Chrome 浏览器             B站前端                  B站后端 API
 *      │                    │                        │                         │
 *      │ new ChromeDriver() │ ← 启动浏览器           │                         │
 *      │ ──────────────────▶│                        │                         │
 *      │ driver.get(...)    │← 加载空的 HTML 页面    │                         │
 *      │                    │                        │← <script src="main.js"> │
 *      │                    │                        │ 执行 JS（Vue/React）    │
 *      │                    │                        │ 发起 AJAX 请求         │
 *      │                    │                        │────────────────────────▶│
 *      │                    │                        │                         │ 返回 JSON（电影列表）
 *      │                    │                        │← 构建 DOM 元素（.rank-item）
 *      │ Thread.sleep(5s)   │← JS 渲染完成           │                         │
 *      │                    │                        │                         │
 *      │ findElements(...)  │← 查找 DOM 元素         │                         │
 *      │ 提取文本、链接     │← 拿到电影信息          │                         │
 *      │ 插入数据库         │                         │                         │
 *      │ driver.quit()      │← 关闭浏览器            │                         │
 * */

/**
 * 1. 前后端分离
 * 页面 HTML 中没有真实数据
 *
 * 数据是 JS 脚本动态从接口请求回来的（前端和后端是两个系统）
 *
 * 后端只提供 JSON 接口，比如：
 * */

/**
 * 2. 动态加载
 * 页面结构由 Vue/React 动态生成（不是服务端返回的）
 *
 * 所有 .rank-item 都是通过 JS 渲染出来的
 *
 * Jsoup 这种工具爬不到这些 DOM 元素，只有 Selenium 能模拟浏览器完整加载、执行 JS、构建 DOM
 * */

public class BilibiliMovieSelenium {
    public static void main(String[] args) throws InterruptedException {

        // 启动 Chrome 浏览器驱动（Selenium 启动了一个“真实”的浏览器进程）
        WebDriver driver = new ChromeDriver();

        // 浏览器访问 B 站电影排行榜页面
        // 此时浏览器开始加载 HTML 框架、CSS、JavaScript 文件
        driver.get("https://www.bilibili.com/v/popular/rank/movie");

        // 等待 5 秒钟，让 JS 脚本有时间完成渲染
        // 页面结构是“前端空壳 HTML + JavaScript 异步请求数据”
        // JS 需要时间调用接口、构建 DOM 结构
        Thread.sleep(5000);

        // Selenium 查询当前页面上所有 class="rank-item" 的 DOM 元素
        // 注意：这些元素是前端 JS 渲染出来的（不是 HTML 源码自带的）
        List<WebElement> items = driver.findElements(By.cssSelector(".rank-item"));

        // 遍历所有电影条目
        int rank = 1;
        for (WebElement item : items) {

            // 获取电影标题：前端 JS 把 JSON 数据插入到了 .info .title 中
            String title = item.findElement(By.cssSelector(".info .title")).getText();

            // 获取状态（如“2024-12-28上映”或“敬请期待”）
            String status = item.findElement(By.cssSelector(".detail .data-box")).getText();

            // 获取点击跳转链接（来自 <a href="..."> 的 href 属性）
            String link = item.findElement(By.cssSelector("a")).getAttribute("href");

            // 打印输出
            System.out.printf(" %-25s  %s %s\n", title, status, link);

            // 写入数据库（调用你写好的 DAO 类）
            MovieDao.insert(title, link, status, rank);

            // 排序编号 +1
            rank++;
        }

        // 关闭浏览器（释放资源）
        driver.quit();
    }
}

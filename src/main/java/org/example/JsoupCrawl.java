package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class JsoupCrawl {
    private static final String baseUrl = "https://www.szu.edu.cn/index/sdxw.htm";
    private static final String downloadDir = "/Users/xh/Desktop/temp/";

    public static void main(String[] args) throws IOException {
        // 模拟浏览器访问行为
        Document document = Jsoup.connect(baseUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/89.0.4389.82 Safari/537.36")
                .timeout(5000)
                .get();

        // 获取所有 <img> 标签
        Elements imgs = document.select("img");

        System.out.println("共找到 " + imgs.size() + " 张图片：");
        for (Element img : imgs) {
            // 使用 absUrl 自动转换为绝对路径（相对 ../images/on_03.png 会补全）
            String src = img.absUrl("src");

            // 进一步筛选：只抓 .png 的图片，并且路径中包含 /images/
            // 只下载 .png 格式，并且属于 /images/ 路径的图片
            if (src.endsWith(".jpg") && src.contains("/images/")) {
                System.out.println("下载图片: " + src);

                // 从 src 路径中提取图片文件名
                String fileName = src.substring(src.lastIndexOf("/") + 1);

                // 构造保存文件的完整路径
                File outputFile = new File(downloadDir + fileName);

                // 下载并保存图片
                try {
                    FileUtils.copyURLToFile(new URL(src), outputFile);
                    System.out.println("已保存到: " + outputFile.getAbsolutePath());
                } catch (IOException e) {
                    System.out.println("下载失败: " + src);
                    e.printStackTrace();
                }
            }
        }
    }
}

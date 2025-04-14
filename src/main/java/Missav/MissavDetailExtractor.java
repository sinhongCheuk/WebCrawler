package Missav;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class MissavDetailExtractor {

    public static class MissavVideo {
        public String code;
        public String title;
        public String actors;
        public String duration;
        public String date;
        public String cover;
        public String link;
        public String description;

        @Override
        public String toString() {
            return "番号: " + code + "\n"
                    + "标题: " + title + "\n"
                    + "演员: " + actors + "\n"
                    + "时长: " + duration + "\n"
                    + "上映: " + date + "\n"
                    + "封面: " + cover + "\n"
                    + "链接: " + link + "\n"
                    + "描述: " + description;
        }
    }

    public static MissavVideo extractDetail(WebDriver driver, String duration, String cover, String link) {
        MissavVideo video = new MissavVideo();
        video.duration = duration;   // 从主列表页传入的影片时长
        video.cover = cover;         // 从主列表页传入的封面图链接
        video.link = link;           // 当前详情页链接

        try {
            // 从 meta 标签中提取标题（含番号+标题+演员）
            String fullTitle = getMeta(driver, "twitter:title");
            video.title = fullTitle;

            // 通过空格切分提取“番号”（标题开头部分，如 WAAA-497）
            if (fullTitle != null && fullTitle.contains(" ")) {
                video.code = fullTitle.split(" ")[0].trim();
            }

            // 提取“演员名”：从标题中最后一个 “-” 后截取
            if (fullTitle != null && fullTitle.contains("-")) {
                video.actors = fullTitle.substring(fullTitle.lastIndexOf("-") + 1).trim();
            }

            // 从 meta 中获取发布日期和影片简介
            video.date = getMeta(driver, "og:video:release_date");
            video.description = getMeta(driver, "twitter:description");

        } catch (Exception e) {
            System.out.println("extractDetail 解析失败: " + link);
        }

        return video;
    }


    private static String getMeta(WebDriver driver, String key) {
        try {
            // 优先按 meta[name=xxx] 获取内容
            return driver.findElement(By.cssSelector("meta[name='" + key + "']")).getAttribute("content");
        } catch (Exception e1) {
            try {
                // 如果没有，再尝试通过 property 属性
                return driver.findElement(By.cssSelector("meta[property='" + key + "']")).getAttribute("content");
            } catch (Exception e2) {
                return null;
            }
        }
    }
}


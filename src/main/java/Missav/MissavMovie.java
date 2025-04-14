package Missav;

/**
 * 封装每一部 Missav 影片的结构
 * 用于存储和传输影片的详细信息
 * 最终会被持久化到 MySQL 数据库中
 */
public class MissavMovie {
    private String code;           // 番号，例如 "WAAA-497"
    private String title;          // 影片完整标题
    private String actors;         // 演员名称列表（字符串）
    private String duration;       // 时长（如 "1:58:34"）
    private String releaseDate;    // 上映日期（格式：yyyy-MM-dd）
    private String coverUrl;       // 封面图链接
    private String videoUrl;       // 视频播放链接
    private String description;    // 简介 / 剧情描述
    private int pageNumber;        // 所在列表页码

    // Getter / Setter 方法（IDE 可自动生成）

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getActors() { return actors; }
    public void setActors(String actors) { this.actors = actors; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
}

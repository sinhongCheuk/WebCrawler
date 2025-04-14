package Missav;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MissavQueryService {

    private static final String URL = "jdbc:mysql://localhost:3306/missav?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "******";

    /**
     * 获取所有影片信息
     */
    public static List<MissavMovie> getAllMovies() {
        List<MissavMovie> movies = new ArrayList<>();
        String sql = "SELECT * FROM missav_movies ORDER BY release_date DESC";
        // SELECT * FROM missav_movies WHERE actors LIKE '%三上悠亞%'

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                MissavMovie movie = new MissavMovie();
                //movie.setId(rs.getInt("id"));
                movie.setCode(rs.getString("code"));
                movie.setTitle(rs.getString("title"));
                movie.setActors(rs.getString("actors"));
                movie.setDuration(rs.getString("duration"));
                movie.setReleaseDate(String.valueOf(rs.getDate("release_date")));
                movie.setCoverUrl(rs.getString("cover_url"));
                movie.setVideoUrl(rs.getString("video_url"));
                movie.setDescription(rs.getString("description"));
                movie.setPageNumber(rs.getInt("page_number"));

                movies.add(movie);
            }

        } catch (SQLException e) {
            System.out.println("查询失败: " + e.getMessage());
        }

        return movies;
    }

    /**
     * 示例 main 函数：打印所有电影名称和番号
     */
    public static void main(String[] args) {
        List<MissavMovie> movies = getAllMovies();
        for (MissavMovie mv : movies) {
            System.out.printf("[%s] %s\n", mv.getCode(), mv.getTitle());
        }
    }
}

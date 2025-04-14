package Missav;

import java.sql.*;

public class MissavDao {
    private static final String URL = "jdbc:mysql://localhost:3306/missav?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true";
    private static final String USER = "root";
    private static final String PASSWORD = "******";

    public static void insert(MissavMovie movie) {
        String sql = "INSERT INTO missav_movies (code, title, actors, duration, release_date, cover_url, video_url, description, page_number) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, movie.getCode());
            stmt.setString(2, movie.getTitle());
            stmt.setString(3, movie.getActors());
            stmt.setString(4, movie.getDuration());
            stmt.setDate(5, movie.getReleaseDate() != null ? Date.valueOf(movie.getReleaseDate()) : null);
            stmt.setString(6, movie.getCoverUrl());
            stmt.setString(7, movie.getVideoUrl());
            stmt.setString(8, movie.getDescription());
            stmt.setInt(9, movie.getPageNumber());

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("插入失败: " + movie.getTitle());
            e.printStackTrace();
        }
    }
}

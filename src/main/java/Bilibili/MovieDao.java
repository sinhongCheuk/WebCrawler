package Bilibili;

import java.sql.*;


/**
 * 在 Java 中将数据写入 MySQL 的过程，本质上是：
 *
 * 加载 JDBC 驱动（在新版中由 DriverManager 自动完成）；
 *
 * 建立数据库连接；
 *
 * 构造 SQL 语句（建议使用预编译 PreparedStatement）；
 *
 * 绑定参数；
 *
 * 执行 SQL（executeUpdate() 表示执行 insert/update/delete）；
 *
 * 数据被写入到数据库。
 * */
public class MovieDao {

    // 数据库配置
    private static final String URL = "jdbc:mysql://localhost:3306/bilibili?useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
    private static final String USER = "root";
    private static final String PASSWORD = "@Xumizi666";

    /**
     * 插入一条电影数据到 bilibili_movies 表
     * @param title       电影标题
     * @param link        视频链接
     * @param status      显示的状态（如上映时间/敬请期待）
     * @param rankIndex   当前电影在排行榜上的名次（从1开始）
     */
    public static void insert(String title, String link, String status, int rankIndex) {
        // 准备的 SQL 插入语句，用了 ? 作为占位符，这是预处理语句（PreparedStatement）的写法，可以防止 SQL 注入，提升执行效率。
        String sql = "INSERT INTO bilibili_movies (title, link, status, rank_index) VALUES (?, ?, ?, ?)";

        try (
                // 通过 JDBC 驱动建立与 MySQL 数据库的连接，其中：
                // URL 形如 jdbc:mysql://localhost:3306/bilibili?... 指明了要连接的数据库；
                // USER 和 PASSWORD 是你的数据库用户名和密码；
                // 成功连接后会返回一个 Connection 对象。
                Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

                // conn.prepareStatement(sql) 是 JDBC 中最推荐、最安全、最现代的写法，避免拼接 SQL 字符串的麻烦和风险。
                // 告诉数据库“我要执行一条 SQL 语句”，并且先把这条 SQL 编译好、准备好，然后再往里填数据。
                // 它把 ? 当做“空槽”，提前发给数据库 预编译，之后只需要填进去真实的值。
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            // 设置占位符参数
            // 这四行就是将 Java 中的变量 绑定到 SQL 中的 ? 占位符上
            // 绑定完成后，SQL 就变成了一条完整的 INSERT INTO ... VALUES(...) 的语句。
            ps.setString(1, title);
            ps.setString(2, link);
            ps.setString(3, status);
            ps.setInt(4, rankIndex);

            // 调用 executeUpdate()，表示执行 insert 操作，它会返回影响的行数（比如成功插入 1 条就返回 1）。
            // 数据库此时就会真正地将数据写入硬盘中的表结构中了！
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("插入失败: " + title);
            e.printStackTrace();
        }
    }
}

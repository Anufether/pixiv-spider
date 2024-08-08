package top.anufether.pixiv;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;

/**
 * @Project: pixiv-spider
 * @Package: top.anufether.pixiv
 * @Author: anufether
 * @Create: 2024/8/8 9:04
 * @Description: 数据库管理类，用于创建和管理SQLite数据库。
 * @History: modify
 * * 1900-01-01 12:00:00 modified by xxx
 **/
@Slf4j
@Getter
@Setter
public class DatabaseManager {

    /**
     * jar 包路径
     */
    private String jarPath;

    /**
     * 链接对象
     */
    private Connection conn;

    /**
     * 加载 SQLite 数据库驱动并连接到数据库。如果数据库不存在，则创建一个新的数据库文件。
     */
    public void load() {
        String driver = "org.sqlite.JDBC";
        String url = "jdbc:sqlite:" + jarPath + "pixiv-spider.db";
        try {
            // 加载 SQLite 驱动
            Class.forName(driver);
            // 连接到数据库
            this.conn = DriverManager.getConnection(url);
            log.info("数据库连接成功");
        } catch (ClassNotFoundException | SQLException e) {
            // 连接数据库失败时，记录错误信息并退出程序
            log.error("连接数据库时失败: {}", e.getMessage());
            System.exit(Constants.EXIT_ERROR);
        }

        // 创建 crawled_artworks 表的 SQL 语句
        String sql = "CREATE TABLE IF NOT EXISTS `crawled_artworks` (" +
                "`id` INT NOT NULL, " +
                "`amount` SMALLINT NOT NULL, " +
                "PRIMARY KEY(`id`)" +
                ");";
        try {
            // 执行创建表的 SQL 语句
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            log.info("数据表创建成功");
        } catch (SQLException e) {
            // 创建表失败时，记录错误信息并退出程序
            log.error("创建数据表时失败: {}", e.getMessage());
            System.exit(Constants.EXIT_ERROR);
        }
    }

    /**
     * 根据作品ID检查作品的数量。
     *
     * @param id 作品ID
     * @return 作品数量，如果作品不存在则返回0
     */
    public int checkArtworks(int id) {
        String sql = "SELECT `amount` FROM `crawled_artworks` WHERE `id`=?;";
        int amount = 0;
        try {
            // 准备 SQL 查询语句
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            // 执行查询并获取结果集
            ResultSet rs = pstmt.executeQuery();
            // 检查结果集是否为空
            if (rs.next()) {
                amount = rs.getInt("amount");
            }
        } catch (SQLException e) {
            // 查询失败时，记录错误信息
            log.error("查询作品时失败: {}", e.getMessage());
        }
        return amount;
    }

    /**
     * 向数据库中添加作品信息。
     *
     * @param id     作品ID
     * @param amount 作品数量
     */
    public void addArtworks(int id, int amount) {
        String sql = "INSERT INTO `crawled_artworks` (`id`, `amount`) VALUES (?, ?);";
        try {
            // 准备 SQL 插入语句
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.setInt(2, amount);
            // 执行插入操作
            pstmt.executeUpdate();
            log.info("作品添加成功");
        } catch (SQLException e) {
            // 插入失败时，记录错误信息
            log.error("添加作品时失败: {}", e.getMessage());
        }
    }
}

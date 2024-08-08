package top.anufether.pixiv;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * @Project: pixiv-spider
 * @Package: top.anufether.pixiv
 * @Author: anufether
 * @Create: 2024/8/8 10:20
 * @Description: this file is used for ...
 * @History: modify
 * * 1900-01-01 12:00:00 modified by xxx
 **/
@Slf4j
public class PixivSpider {
    // jar 包路径
    public static String jarPath = Objects.requireNonNull(PixivSpider.class.getClassLoader().getResource("")).getPath();

    // 读取配置
    public static Config config = new Config("config.yml");

    // 数据库链接
    public static DatabaseManager databaseManager = new DatabaseManager();

    public static void main(String[] args) {

        // 设置配置文件路径
        config.setJarPath(jarPath);
        config.saveDefaultConfig("config.yml");

        // 尝试上锁配置文件
        File cfgFile = new File(jarPath + "config.yml");
        try (FileChannel ignored = FileChannel.open(cfgFile.toPath(), StandardOpenOption.READ)) {
            log.info("配置文件成功上锁.");
        } catch (IOException e) {
            log.warn("上锁配置文件失败, 请在接下来的过程中勿编辑配置文件.", e);
        }

        config.load("config.yml");

        // 设置代理
        String proxyHost = config.getString("proxy.host");
        String proxyPort = config.getString("proxy.port");
        if (!proxyHost.isEmpty()) {
            System.setProperty("proxyHost", proxyHost);
        }
        if (!proxyPort.isEmpty()) {
            System.setProperty("proxyPort", proxyPort);
        }

        // 设置数据库
        databaseManager.setJarPath(jarPath);
        databaseManager.load();

        // 设置爬虫
        PageResolver crawler = new PageResolver(config, databaseManager);
        crawler.setJarPath(jarPath);
        String cookie = config.getString("cookie");
        crawler.addCookie("PHPSESSID", cookie);

        // 开始爬取
        String url = config.getString("startpage");
        while (true) {
            try {
                url = crawler.resolveListPage(url);
                log.info("已完成当前列表并成功获取到下一页: {}", url);
            } catch (Exception e) {
                log.error("处理列表页面时发生错误", e);
                break; // 发生错误后退出循环
            }
        }
    }
}

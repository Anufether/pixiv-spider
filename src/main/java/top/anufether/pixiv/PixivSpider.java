package top.anufether.pixiv;

import lombok.extern.slf4j.Slf4j;
import top.anufether.pixiv.config.YamlConfig;
import top.anufether.pixiv.dao.DatabaseManager;
import top.anufether.pixiv.spider.PageResolver;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

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
    public static String jarPath = getJarPath();

    // 读取配置
    public static YamlConfig yamlConfig = new YamlConfig("config.yaml");

    // 数据库链接
    public static DatabaseManager databaseManager = new DatabaseManager();

    public static void main(String[] args) {
        log.info("Starting Pixiv Spider...");

        // 设置配置文件路径
        yamlConfig.setJarPath(jarPath);
        yamlConfig.saveDefaultConfig("config.yaml");

        // 尝试上锁默认配置文件
        File cfgFile = new File(jarPath + "config.yaml");
        try (FileChannel ignored = FileChannel.open(cfgFile.toPath(), StandardOpenOption.READ)) {
            log.info("配置文件成功上锁.");
        } catch (IOException e) {
            log.warn("上锁配置文件失败, 请在接下来的过程中勿编辑配置文件.", e);
        }

        yamlConfig.load("config.yaml");

        // 设置代理
        String proxyHost = yamlConfig.getString("proxy.host");
        String proxyPort = yamlConfig.getString("proxy.port");
        if (!proxyHost.isEmpty() && !proxyPort.isEmpty()) {
            System.setProperty("proxyHost", proxyHost);
            System.setProperty("proxyPort", proxyPort);
            log.info("检测到代理服务器，运行环境已配置代理: {}:{}", proxyHost, proxyPort);
        }

        // 设置数据库
        databaseManager.setJarPath(jarPath);
        databaseManager.load();

        // 设置爬虫
        PageResolver crawler = new PageResolver(yamlConfig, databaseManager);
        crawler.setJarPath(jarPath);
        String cookie = yamlConfig.getString("cookie");
        crawler.addCookie("PHPSESSID", cookie);

        // 开始爬取
        String url = yamlConfig.getString("startPage");
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

    public static String getJarPath() {
        try {
            // 获取 JAR 文件的 URL
            URL url = PixivSpider.class.getProtectionDomain().getCodeSource().getLocation();
            // 将 URL 转换为文件路径
            File jarFile = new File(url.toURI());
            // 返回 JAR 文件所在目录的绝对路径
            return jarFile.getParent() + File.separator;
        } catch (URISyntaxException e) {
            log.error("获取 JAR 包路径失败, {}", e.getMessage());
            return null;
        }
    }
}

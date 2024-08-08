package top.anufether.pixiv;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Project: pixiv-spider
 * @Package: top.anufether.pixiv
 * @Author: anufether
 * @Create: 2024/8/8 9:56
 * @Description: 处理列表页面的爬取和解析
 * @History: modify
 * * 1900-01-01 12:00:00 modified by xxx
 **/
@Slf4j
@Getter
@Setter
public class PageResolver {
    private final Map<String, String> cookies = new HashMap<>();

    /**
     * 配置文件对象
     */
    private Config config;

    /**
     * jar 包路径
     */
    private String jarPath;

    /**
     * 数据连接对象
     */
    private DatabaseManager databaseManager;

    public PageResolver(Config config, DatabaseManager databaseManager) {
        this.config = config;
        this.databaseManager = databaseManager;
    }

    public void addCookie(String key, String value) {
        cookies.put(key, value);
    }

    /**
     * 解析列表页面，并获取下一页 URL
     *
     * @param url 要解析的列表页面 URL
     * @return 下一页的 URL，如果没有则返回 null
     */
    public String resolveListPage(String url) {
        String nextpageurl = null;
        try {
            Connection.Response res;
            while (true) {
                try {
                    res = Jsoup.connect(url).cookies(cookies).method(Connection.Method.GET).execute();
                    break; // 成功获取响应，退出循环
                } catch (SocketTimeoutException e) {
                    log.warn("请求图片列表页面超时, 将重试.", e);
                } catch (SSLHandshakeException e) {
                    log.warn("请求图片列表页面被拒绝, 将重试.", e);
                } catch (SSLException e) {
                    log.warn("请求图片列表页面被关闭, 将重试.", e);
                } catch (ConnectException e) {
                    log.warn("连接超时, 请检查 cookie 是否错误或过期.", e);
                } catch (SocketException e) {
                    log.warn("意外结束, 将重试.", e);
                } catch (HttpStatusException e) {
                    log.warn("HTTP 状态错误 {}，请填写正确的 cookie.", e.getStatusCode(), e);
                }
            }

            Document doc = res.parse();
            Elements pages = doc.select("#wrapper").select("div.layout-body").select("div")
                    .select("div.ui-fixed-container").select("div").select("nav:nth-child(2)").select("ul")
                    .select("li.after").select("a");
            Element nextpage = null;
            try {
                nextpage = pages.get(0);
            } catch (IndexOutOfBoundsException e) {
                log.error("网页格式错误，请检查 cookie 是否已经过期 (或者该榜单已被爬取完毕).", e);
                System.exit(Constants.EXIT_ERROR);
            }

            nextpageurl = nextpage.absUrl("href");
            Elements images = doc.select("#wrapper").select("div.layout-body").select("div")
                    .select("div.ranking-items-container").select("div.ranking-items.adjust")
                    .select("section.ranking-item");

            for (Element image : images) {
                String dataId = image.attr("data-id");
                Element imagePage = image.select("div.ranking-image-item").select("a").get(0);
                String imagePageUrl = imagePage.absUrl("href");
                log.info("==============={}===============", dataId);
                log.info("正在爬取: {}", imagePageUrl);
                resolveImagePage(imagePageUrl, dataId);
            }

            config.setValue("startpage", url);
            config.Save();
        } catch (IOException e) {
            log.error("处理页面时发生错误", e);
            return nextpageurl;
        }

        log.info("下一页 URL: {}", nextpageurl);
        return nextpageurl;
    }

    private void resolveImagePage(String imagePageUrl, String dataId) {
        int amount = databaseManager.checkArtworks(Integer.parseInt(dataId)); // 尝试获取一个值
        if (amount == 0) { // 如果为 0 说明数据库中没有这个数据
            log.info("数据库中未查到此图片页面的信息, 继续下载操作.");
        } else { // 如果不是 0 说明这些图片已经下载过了
            log.info("数据库中已查到此图片页面的信息, 图片数量为 {} , 自动跳过.", amount);
            return;
        }

        try {
            Connection.Response res;
            while (true) {
                try {
                    res = Jsoup.connect(imagePageUrl).cookies(cookies).method(Connection.Method.GET).execute();
                    break; // 成功获取响应，退出循环
                } catch (SocketTimeoutException e) {
                    log.warn("请求图片页面超时, 将重试.", e);
                } catch (SSLHandshakeException e) {
                    log.warn("请求图片页面被拒绝, 将重试.", e);
                } catch (SSLException e) {
                    log.warn("请求图片页面被关闭, 将重试.", e);
                } catch (ConnectException e) {
                    log.warn("连接超时, 请检查 cookie 是否错误或过期.", e);
                } catch (SocketException e) {
                    log.warn("意外结束, 将重试.", e);
                }
            }

            Document doc = res.parse();
            Element meta = doc.select("#meta-preload-data").first();
            assert meta != null;
            String content = meta.attr("content");
            JSONObject obj = JSON.parseObject(content);
            int pageCount = obj.getJSONObject("illust").getJSONObject(dataId).getIntValue("pageCount");
            String p0Url = obj.getJSONObject("illust").getJSONObject(dataId).getJSONObject("urls")
                    .getString("original");

            for (int i = 0; i < pageCount; i++) {
                String imgUrl = (i == 0) ? p0Url : p0Url.replaceAll("p0", "p" + i);
                String filename = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
                // 这里要去掉 jarPath 末尾的 "/"
                String imageSavePath = config.getString("imgSavePath").replace("%HERE%",
                        jarPath.substring(0, jarPath.length() - 1));
                File imgFile = FileUtils.createFile(imageSavePath, filename);

                BufferedInputStream in = null;
                BufferedOutputStream out = null;
                Connection.Response resImg;

                try {
                    while (true) {
                        try {
                            resImg = Jsoup.connect(imgUrl).cookies(cookies).ignoreContentType(true)
                                    .maxBodySize((int) Constants.GIGABYTE).referrer("https://www.pixiv.net/artworks/" + dataId).execute();
                            in = new BufferedInputStream(resImg.bodyStream());
                            out = new BufferedOutputStream(new FileOutputStream(imgFile));
                            byte[] bytes = new byte[1024];
                            int total = 0;
                            int count;
                            while ((count = in.read(bytes)) != -1) {
                                out.write(bytes, 0, count);
                                total += count;
                            }
                            log.info("文件 {} 保存完成, 共收到 {} 字节.", filename, total);
                            break;
                        } catch (SocketTimeoutException e) {
                            log.warn("请求图片超时, 将重试.", e);
                        } catch (SSLHandshakeException e) {
                            log.warn("请求图片被拒绝, 将重试.", e);
                        } catch (SSLException e) {
                            log.warn("请求图片被关闭, 将重试.", e);
                        } catch (ConnectException e) {
                            log.warn("连接超时, 请检查 cookie 是否错误或过期.", e);
                        } catch (SocketException e) {
                            log.warn("意外结束, 将重试.", e);
                        } catch (HttpStatusException e) {
                            if (imgUrl.contains(".jpg")) {
                                imgUrl = imgUrl.replaceAll(".jpg", ".png");
                            } else if (imgUrl.contains(".png")) {
                                imgUrl = imgUrl.replaceAll(".png", ".jpg");
                            }
                            log.warn("HTTP 状态错误: {} 将尝试另一后缀名.", e.getStatusCode());
                        }
                    }
                } catch (IOException e) {
                    log.error("文件操作错误", e);
                } finally {
                    try {
                        if (in != null) in.close();
                        if (out != null) out.close();
                    } catch (IOException e) {
                        log.error("关闭流时发生错误", e);
                    }
                }
            }
            databaseManager.addArtworks(Integer.parseInt(dataId), pageCount);
        } catch (IOException e) {
            log.error("处理图片页面时发生错误", e);
        }
    }
}

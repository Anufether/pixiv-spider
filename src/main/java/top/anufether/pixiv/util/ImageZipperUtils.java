package top.anufether.pixiv.util;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import top.anufether.pixiv.constant.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @Project: pixiv-spider
 * @Package: top.anufether.pixiv
 * @Author: anufether
 * @Create: 2024/8/10 8:58
 * @Description: 图片压缩类
 * @History: modify
 * * 1900-01-01 12:00:00 modified by xxx
 **/
@Slf4j
@Getter
@Setter
public class ImageZipperUtils {

    /**
     * 最大打包图片数量
     */
    private Integer zipNum = Constants.ZIP_MAX_NUM;

    /**
     * 打包文件夹路径
     */
    private String zipPath;

    /**
     * 压缩指定文件夹中的图片文件
     *
     * @param outputZipFilePattern 输出的ZIP文件路径，如 D:/output/images.zip
     * @throws IOException 压缩图片文件失败异常
     */
    public void zipImages(String outputZipFilePattern) throws IOException {
        Path zipDirPath = Paths.get(zipPath);
        File[] imageFiles = zipDirPath.toFile().listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));

        if (imageFiles == null || imageFiles.length == 0) {
            log.warn("没有找到要压缩的图片文件！");
            return;
        }
        int fileCount = 0;
        int zipCount = 0;

        ZipArchiveOutputStream zos = createZipOutputStream(outputZipFilePattern, zipCount);

        try {
            for (File imageFile : imageFiles) {
                if (zipNum != null && fileCount >= zipNum) {
                    log.debug("达到最大打包数量: {}，创建新压缩文件", zipNum);
                    zos.close(); // 关闭当前的ZIP文件
                    zipCount++;
                    zos = createZipOutputStream(outputZipFilePattern, zipCount);
                    fileCount = 0; // 重置文件计数器
                }

                log.debug("即将打包的文件: {}", imageFile.getName());

                try (FileInputStream fis = new FileInputStream(imageFile)) {
                    ZipArchiveEntry entry = new ZipArchiveEntry(imageFile.getName());
                    zos.putArchiveEntry(entry);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeArchiveEntry();
                    fileCount++;
                    log.info("已添加图片文件: {}", imageFile.getName());
                } catch (IOException e) {
                    log.error("添加图片文件失败: {}", imageFile.getName(), e);
                }
            }
        } finally {
            zos.close();
        }

        log.info("图片压缩完成，输出文件模式: {}", outputZipFilePattern);
    }

    private ZipArchiveOutputStream createZipOutputStream(String outputZipFilePattern, int zipCount) throws IOException {
        String outputZipFile = String.format(outputZipFilePattern, zipCount);
        log.debug("创建新的压缩文件: {}", outputZipFile);
        return new ZipArchiveOutputStream(new FileOutputStream(outputZipFile));
    }
}

package top.anufether.pixiv.util;

import java.io.File;
import java.io.IOException;

/**
 * @Project: pixiv-spider
 * @Package: top.anufether.pixiv
 * @Author: anufether
 * @Create: 2024/8/7 17:14
 * @Description: this file is used for ...
 * @History: modify
 * * 1900-01-01 12:00:00 modified by xxx
 **/
public class FileUtils {
    /**
     * 创建指定路径的文件夹。如果文件夹已存在，则不会进行任何操作。
     *
     * @param folderPath 文件夹的路径
     */
    public static void createFolder(String folderPath) {
        File folder = new File(folderPath);

        // 尝试创建文件夹
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (!created) {
                throw new RuntimeException("无法创建文件夹: " + folderPath);
            }
        }

    }

    /**
     * 创建指定路径下的文件。如果文件已存在，则先删除旧文件，然后创建新文件。
     * 如果父文件夹不存在，则会首先创建父文件夹。
     *
     * @param folderPath 文件的父文件夹路径
     * @param fileName 文件名
     * @return 创建的文件对象
     * @throws RuntimeException 如果创建文件或删除现有文件时发生错误
     */
    public static File createFile(String folderPath, String fileName) {
        // 确保文件夹存在
        createFolder(folderPath);

        File file = new File(folderPath, fileName);
        try {
            // 如果文件已存在，尝试删除旧文件
            if (file.exists() && !file.delete()) {
                throw new RuntimeException("无法删除已存在的文件: " + file.getAbsolutePath());
            }

            // 尝试创建新文件
            if (!file.createNewFile()) {
                throw new RuntimeException("无法创建新文件: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("无法创建文件: " + file.getAbsolutePath(), e);
        }

        return file;
    }

    /**
     * 创建指定的文件。如果文件已经存在，则会被删除。
     * 如果文件的父文件夹不存在，则会首先创建父文件夹。
     *
     * @param file 要创建的文件对象
     * @throws RuntimeException 如果创建文件或删除现有文件时发生错误
     */
    public static void createFile(File file) {
        // 确保父文件夹存在
        File parentFolder = file.getParentFile();
        if (parentFolder != null && !parentFolder.exists()) {
            if (!parentFolder.mkdirs()) {
                throw new RuntimeException("无法创建父文件夹: " + parentFolder.getAbsolutePath());
            }
        }

        try {
            // 如果文件已经存在，尝试删除旧文件
            if (file.exists() && !file.delete()) {
                throw new RuntimeException("无法删除已存在的文件: " + file.getAbsolutePath());
            }

            // 尝试创建新文件
            if (!file.createNewFile()) {
                throw new RuntimeException("无法创建新文件: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("无法处理文件: " + file.getAbsolutePath(), e);
        }
    }

}

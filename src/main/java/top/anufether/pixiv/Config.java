package top.anufether.pixiv;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Project: pixiv-reptile
 * @Package: top.anufether.pixiv
 * @Author: anufether
 * @Create: 2024/8/7 16:46
 * @Description: 用于读取 config.yaml 配置文件
 * @History: modify
 * * 1900-01-01 12:00:00 modified by xxx
 **/
@Getter
@Setter
@Slf4j
public class Config {

    public Map<String, Object> map = new LinkedHashMap<>();

    /**
     * config.yaml 文件路径
     */
    private String path;

    /**
     * jar 包路径
     */
    private String jarPath;

    /**
     * 文件
     */
    private File file;

    /**
     * 加载指定路径的文件，并将内容解析为 Map。
     * @param path 文件路径
     * @throws RuntimeException 如果文件未找到或读取过程中发生错误
     */
    public void load(String path) {
        if (file == null) {
            file = new File(jarPath + path);
        }

        try (InputStream in = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            this.map = yaml.load(in);
        } catch (FileNotFoundException e) {
            log.error("File not found: {}", file.getAbsolutePath());
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Error closing InputStream", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将当前的 map 对象保存到指定的文件中。
     * 使用 YAML 格式进行保存，文件以块样式格式化并且具有良好的可读性。
     *
     * @throws RuntimeException 如果文件创建或写入过程中发生错误
     */
    public void save() {
        // 设置 YAML 输出选项
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // 块样式输出
        options.setPrettyFlow(true); // 美化输出

        Yaml yaml = new Yaml(options);

        // 确保文件存在
        try {
            FileUtils.createFile(file);
        } catch (RuntimeException e) {
            log.error("文件创建失败: {}", file.getAbsolutePath(), e);
            throw e; // 重新抛出异常，以便上层处理
        }

        // 写入数据到文件
        try (FileWriter writer = new FileWriter(file)) {
            yaml.dump(this.map, writer);
        } catch (IOException e) {
            log.error("写入文件时发生错误: {}", file.getAbsolutePath(), e);
            throw new RuntimeException("无法写入文件: " + file.getAbsolutePath(), e);
        }
    }

}

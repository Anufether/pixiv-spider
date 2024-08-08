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
 * @Project: pixiv-spider
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

    public Config(String path) {
        this.path = path;
    }

    /**
     * 加载指定路径的文件，并将内容解析为 Map。
     *
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

    public void Save() {
        this.save();
    }

    /**
     * 获取指定路径的值
     *
     * @param path 路径，使用"."分隔的嵌套路径
     * @return 指定路径的值
     */
    @SuppressWarnings("unchecked")
    public Object getValue(String path) {
        if (path.equals(".")) { // 表示根路径
            return this.map;
        }

        if (!path.contains(".")) { // 只有一个key, 直接从map获取值
            return this.map.get(path);
        }

        // 分割路径, "."是转义字符必须加\\
        String[] keys = path.split("\\.");
        Map<String, Object> map = this.map;

        // 循环从第一个key开始, 至倒数第二个key为止
        for (int i = 0; i < keys.length - 1; i++) {
            map = (Map<String, Object>) map.get(keys[i]);
            if (map == null) {
                return null; // 如果中途找不到对应的key，返回null
            }
        }

        return map.get(keys[keys.length - 1]); // 直接取最后一个key的值作为value
    }

    /**
     * 获取指定路径的字符串值
     *
     * @param path 路径，使用"."分隔的嵌套路径
     * @return 指定路径的字符串值
     */
    public String getString(String path) {
        Object value = getValue(path);
        return value != null ? value.toString() : null;
    }

    /**
     * 设置指定路径的值
     *
     * @param path  路径，使用"."分隔的嵌套路径
     * @param value 要设置的值
     */
    @SuppressWarnings("unchecked")
    public void setValue(String path, Object value) {
        if (path.equals(".")) { // 表示根路径
            this.map = (Map<String, Object>) value;
            return;
        }

        // 分割路径, "."是转义字符必须加\\
        String[] keys = path.split("\\.");
        Map<String, Object> cache = this.map;

        for (int i = 0; i < keys.length - 1; i++) {
            cache = (Map<String, Object>) cache.get(keys[i]);
        }

        cache.put(keys[keys.length - 1], value);
    }

    /**
     * 保存默认配置文件
     *
     * @param path 配置文件的路径
     */
    public void saveDefaultConfig(String path) {
        File cfgFile = new File(getJarPath() + path);
        if (cfgFile.exists()) {
            log.info("文件已存在: {}", getJarPath() + path);
        } else {
            // 从 jar 包中获取配置文件
            try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.yaml");
                 FileOutputStream out = new FileOutputStream(cfgFile)) {

                if (in == null) {
                    log.warn("配置文件路径无效，无法找到文件: {}", path);
                    return;
                }

                byte[] bytes = new byte[1024];
                int count;
                while ((count = in.read(bytes)) != -1) {
                    out.write(bytes, 0, count); // 将数据写入插件文件
                }
                log.warn("未找到配置文件，已自动为您生成，请将其配置好后重新运行此程序.");
                System.exit(Constants.EXIT_ERROR);
            } catch (IOException e) {
                log.error("保存默认配置文件失败,{}", file.getAbsolutePath(), e);
            }
        }
    }
}

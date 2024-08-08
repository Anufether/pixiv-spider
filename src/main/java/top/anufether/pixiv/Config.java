package top.anufether.pixiv;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
     * 获取指定路径的列表值
     *
     * @param path 路径，使用"."分隔的嵌套路径
     * @return 指定路径的列表值
     */
    @SuppressWarnings("unchecked")
    public List<String> getList(String path) {
        return (List<String>) getValue(path);
    }

    /**
     * 获取指定路径的所有子键
     *
     * @param path 路径，使用"."分隔的嵌套路径
     * @return 指定路径的所有子键列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getKey(String path) {
        Map<String, Object> map = (Map<String, Object>) getValue(path);
        return map != null ? new ArrayList<>(map.keySet()) : new ArrayList<>();
    }

    /**
     * 通过值查找键
     *
     * @param value 要查找的值
     * @return 具有指定值的键列表
     */
    public List<String> getKeyOfValue(String value) {
        List<String> keyList = new ArrayList<>();
        for (String key : map.keySet()) {
            String strValue = String.valueOf(map.get(key)); // 将类型(Integer)转化为String
            if (strValue.equals(value)) {
                keyList.add(key);
            }
        }
        return keyList;
    }

    /**
     * 获取指定路径的布尔值
     *
     * @param path 路径，使用"."分隔的嵌套路径
     * @return 指定路径的布尔值
     */
    public boolean getBoolean(String path) {
        Object value = getValue(path);
        return value != null && (boolean) value;
    }

    /**
     * 设置指定路径的值
     *
     * @param path 路径，使用"."分隔的嵌套路径
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

}

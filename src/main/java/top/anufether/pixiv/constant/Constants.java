package top.anufether.pixiv.constant;

/**
 * @Project: pixiv-spider
 * @Package: top.anufether.pixiv
 * @Author: anufether
 * @Create: 2024/8/8 9:51
 * @Description: this file is used for ...
 * @History: modify
 * * 1900-01-01 12:00:00 modified by xxx
 **/
public interface Constants {
    int EXIT_ERROR = 1; // 错误退出

    int END_OF_STREAM = -1; // 流结束标志
    int BUFFER_START_INDEX = 0; // 数据写入操作的起始偏移量

    int ZIP_MAX_NUM = 100; // 单次打包最大文件数量

    // 文件大小常量
    long KILOBYTE = 1024L; // 一千字节
    long MEGABYTE = 1024L * KILOBYTE; // 一兆字节
    long GIGABYTE = 1024L * MEGABYTE; // 一千兆字节
}

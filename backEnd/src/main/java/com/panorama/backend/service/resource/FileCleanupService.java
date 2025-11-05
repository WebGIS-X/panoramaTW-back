package com.panorama.backend.service.resource;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-11 21:31:58
 * @version: 1.0
 */
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class FileCleanupService {

    // 定义需要清理的文件夹路径
    @Value("${path.temp}")
    private String temp;
    // 定义文件超过多长时间没有被修改则删除，单位是秒
    private static final long MAX_UNUSED_TIME = Duration.ofDays(1).getSeconds();

    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanUpOldFiles() {
        log.info("定时清理任务执行中...");
        File directory = new File(temp);
        if (directory.exists() && directory.isDirectory()) {
            // 遍历目录中的所有子文件夹和文件
            deleteOldFiles(directory);
        }
        log.info("定时清理任务完成");
    }

    private void deleteOldFiles(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归处理子文件夹
                    deleteOldFiles(file);
                    // 如果子文件夹为空，删除它
                    if (file.list().length == 0) {
                        if (file.delete()) {
                            log.info("Deleted empty directory: {}", file.getAbsolutePath());
                        } else {
                            log.info("Failed to delete directory: {}", file.getAbsolutePath());
                        }
                    }
                } else {
                    try {
                        // 获取文件的最后修改时间
                        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        Instant lastModifiedTime = attrs.lastModifiedTime().toInstant();

                        // 判断文件是否超过指定时间未被使用
                        if (Duration.between(lastModifiedTime, Instant.now()).getSeconds() > MAX_UNUSED_TIME) {
                            // 删除文件
                            if (file.delete()) {
                                log.info("Deleted old file: {}", file.getAbsolutePath());
                            } else {
                                log.info("Failed to delete file: {}", file.getAbsolutePath());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

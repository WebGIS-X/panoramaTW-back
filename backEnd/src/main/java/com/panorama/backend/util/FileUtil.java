package com.panorama.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-24 14:45:17
 * @version: 1.0
 */
@Slf4j
public class FileUtil {

    public static File convertMultipartFileToFile(MultipartFile multipartFile, String temp) throws IOException {

        String originalFilename = multipartFile.getOriginalFilename();
        assert originalFilename != null;
        String path = String.join(
                File.separator,
                temp,
                originalFilename.substring(0, originalFilename.lastIndexOf('.')) + "_" + System.currentTimeMillis()
        );

        // 创建temp路径的File对象
        File tempDir = new File(path);

        // 如果目录不存在，则创建
        if (!tempDir.exists()) {
            boolean isCreated = tempDir.mkdirs(); // 创建多层目录
            if (!isCreated) {
                throw new IOException("Failed to create directory: " + tempDir);
            }
        }

        File file = new File(path + File.separator + originalFilename);

        // 将 multipartFile 的内容转存到 file 中
        // 使用 InputStream 而不是 transferTo，避免临时文件问题
        try (java.io.InputStream inputStream = multipartFile.getInputStream();
             java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return file;
    }

    public static List<String> unZipFiles(File srcFile, String destDirPath) throws RuntimeException {
        List<String> list = new ArrayList<>();
        long start = System.currentTimeMillis();
        // 判断源文件是否存在
        if (!srcFile.exists()) {
            throw new RuntimeException(srcFile.getPath() + "所指文件不存在");
        }
        // 开始解压
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(srcFile, Charset.forName("GBK"));
            Enumeration<?> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                log.info("解压{}", entry.getName());
                // 如果是文件夹，就创建个文件夹
                if (entry.isDirectory()) {
                    String dirPath = destDirPath + File.separator + entry.getName();
                    File dir = new File(dirPath);
                    dir.mkdirs();
                } else {
                    // 如果是文件，就先创建一个文件，然后用io流把内容copy过去
                    File targetFile = new File(destDirPath + File.separator + entry.getName());
                    // 保证这个文件的父文件夹必须要存在
                    log.info("{}{}{}", destDirPath, File.separator, entry.getName());
                    list.add(destDirPath + File.separator + entry.getName());
                    if (!targetFile.getParentFile().exists()) {
                        log.info("父文件不存在");
                    }
                    targetFile.createNewFile();
                    // 将压缩文件内容写入到这个文件中
                    InputStream is = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    int len;
                    byte[] buf = new byte[1024];
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    // 关流顺序，先打开的后关闭
                    fos.close();
                    is.close();
                }
            }
            long end = System.currentTimeMillis();
            log.info("解压完成，耗时：{} ms", end - start);
        } catch (Exception e) {
            throw new RuntimeException("unzip error from ZipUtils", e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    public static String findFileWithExtension(List<String> list, String extension){
        String fileName = "";
        for (String file : list) {
            if (file.endsWith(extension)) {
                fileName = file;
                break;
            }
        }
        return fileName;
    }

    public static String findFileWithExtension(String directoryPath, String extension) {
        Path dirPath = Paths.get(directoryPath);

        try {
            // 使用 Files.walk() 遍历目录中的所有文件
            return Files.walk(dirPath, 1)
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(extension))
                    .map(Path::toString) // 将 Path 转换为 String
                    .findFirst() // 找到第一个匹配的文件
                    .orElse(null); // 如果没有找到，则返回 null
        } catch (IOException e) {
            System.err.println("访问目录时出错: " + e.getMessage());
            return null;
        }
    }

    public static void deleteFilesInDirectory(String directoryPath) throws IOException {
        Path directory = Paths.get(directoryPath);

        // 检查目录是否存在
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file); // 删除文件
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE; // 保留目录本身
                }
            });
        } else {
            throw new IllegalArgumentException("目录不存在或路径不是目录：" + directoryPath);
        }
    }

    public static void deleteDirectory(Path path) {
        try {
            // 检查路径是否是目录
            if (Files.isDirectory(path)) {
                // 遍历目录中的所有文件和子目录
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path entry : stream) {
                        deleteDirectory(entry); // 递归删除
                    }
                }
            }
            // 删除空目录或文件
            Files.delete(path);
            log.info("删除成功: {}", path);
        } catch (IOException e) {
            log.info("删除失败: {} - {}", path, e.getMessage());
        }
    }

    public static void moveFile(String sourcePath, String destinationPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path destination = Paths.get(destinationPath);
            // 如果目标目录不存在，创建目录
            if (Files.notExists(destination.getParent())) {
                Files.createDirectories(destination.getParent());
            }
            // 移动文件（即“剪切”）
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("File moved from {} to {}", sourcePath, destinationPath);
        } catch (Exception e) {
            log.error("failed to move file from {} to {}, {}", sourcePath, destinationPath, e.getMessage());
        }
    }

    public static long countFilesWithPrefix(String directoryPath, String prefix) throws IOException {
        Path dir = Paths.get(directoryPath);

        // 遍历文件并统计匹配的文件名
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(Files::isRegularFile)  // 只过滤出文件（不包括子目录）
                    .filter(path -> path.getFileName().toString().startsWith(prefix)) // 过滤文件名以 prefix 开头的文件
                    .count();  // 统计数量
        }
    }

}

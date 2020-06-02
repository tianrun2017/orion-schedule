package com.orion.schedule.common.util;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class IoUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IoUtils.class);

    private static String getPath() {
        String path = IoUtils.class.getResource("").getPath();
        if (System.getProperty("os.name").contains("dows")) {
            path = path.substring(1, path.length());
        }
        if (path.contains("jar!")) {
            path = path.substring(0, path.indexOf("jar!"));
            return path.substring(0, path.lastIndexOf("/"));
        }
        return path.replace("target/classes/", "");
    }

    public static File saveContentToFile(String content, String prefix, String suffix) {
        File tempFile = null;
        try {
            String path = getPath();
            File jaas = new File("jaas");
            if (!jaas.exists()) {
                jaas.mkdir();
            }
            tempFile = new File(jaas, String.format("%s.%s", prefix, suffix));
            LOGGER.info("dir is {} {}", path, tempFile.getAbsolutePath());
            if (tempFile.exists()) {
                return tempFile;
            }
            boolean newFile = tempFile.createNewFile();
            if (!newFile) {
                throw new RuntimeException("create file exception " + tempFile.getAbsolutePath());
            }
            Files.asCharSink(tempFile, Charsets.UTF_8).write(content);
            return tempFile;
        } catch (IOException e) {
            LOGGER.warn("Can't create temp file {}", tempFile.getAbsolutePath(), e);
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
        }
    }

    public static File saveContentAsTempFile(String content, String prefix, String suffix) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile(prefix, suffix);
            Files.asCharSink(tempFile, Charsets.UTF_8).write(content);
            return tempFile;
        } catch (IOException e) {
            LOGGER.warn("Can't create temp file {}.", tempFile, e);
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                tempFile.deleteOnExit();
            }
        }
    }

    public static File copyResourceAsTempFile(Class<?> relatedClass, String resourceName, String prefix, String suffix) {
        try {
            File tempFile = File.createTempFile(prefix, suffix);
            try (InputStream in = relatedClass.getResourceAsStream(resourceName)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Resource is {}.", relatedClass.getResource(resourceName));
                }
                try (OutputStream out = new FileOutputStream(tempFile)) {
                    if (in != null) {
                        ByteStreams.copy(in, out);
                        return tempFile;
                    } else {
                        throw new IllegalArgumentException(resourceName + " is not exist!");
                    }
                }
            } finally {
                tempFile.deleteOnExit();
            }
        } catch (IOException e) {
            LOGGER.error("Unexpected io error where process {}.", resourceName, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static File copyResourceAsTempFile(String resourcePath, String prefix, String suffix) {
        try {
            File jaas = new File("jaas");
            if (!jaas.exists()) {
                jaas.mkdir();
            }
            File tempFile = new File(jaas, String.format("%s.%s", prefix, suffix));
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Resource is {}.", Thread.currentThread().getContextClassLoader().getResource(resourcePath));
                }
                try (OutputStream out = new FileOutputStream(tempFile)) {
                    if (in != null) {
                        ByteStreams.copy(in, out);
                        return tempFile;
                    } else {
                        throw new IllegalArgumentException(resourcePath + " is not exist!");
                    }
                }
            } finally {
            }
        } catch (IOException e) {
            LOGGER.error("Unexpected io error where process {}.", resourcePath, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}

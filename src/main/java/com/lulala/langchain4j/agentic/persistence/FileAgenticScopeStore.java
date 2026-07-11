package com.lulala.langchain4j.agentic.persistence;

import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.AgenticScopeSerializer;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于文件系统的 AgenticScopeStore 示例实现。
 * @author shenjh
 * @version 1.0
 * @since 2026/7/6 20:30
 */
public class FileAgenticScopeStore implements AgenticScopeStore {

    private static final String SEPARATOR = "~";
    private static final String SUFFIX = ".json";

    private final Path directory;

    public FileAgenticScopeStore(Path directory) {
        this.directory = directory;
        createDirectory();
    }

    @Override
    public boolean save(AgenticScopeKey key, DefaultAgenticScope agenticScope) {
        try {
            Files.writeString(fileFor(key), AgenticScopeSerializer.toJson(agenticScope), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("保存 AgenticScope 失败：" + key, e);
        }
    }

    @Override
    public Optional<DefaultAgenticScope> load(AgenticScopeKey key) {
        Path file = fileFor(key);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(AgenticScopeSerializer.fromJson(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("读取 AgenticScope 失败：" + key, e);
        }
    }

    @Override
    public boolean delete(AgenticScopeKey key) {
        try {
            return Files.deleteIfExists(fileFor(key));
        } catch (IOException e) {
            throw new IllegalStateException("删除 AgenticScope 失败：" + key, e);
        }
    }

    @Override
    public Set<AgenticScopeKey> getAllKeys() {
        if (!Files.exists(directory)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(SUFFIX))
                    .map(this::keyFromFile)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new IllegalStateException("列出 AgenticScope 文件失败", e);
        }
    }

    public Path directory() {
        return directory;
    }

    public Set<String> fileNames() {
        if (!Files.exists(directory)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new IllegalStateException("列出 AgenticScope 文件失败", e);
        }
    }

    public void clear() {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(SUFFIX))
                    .sorted(Comparator.reverseOrder())
                    .forEach(file -> {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException e) {
                            throw new IllegalStateException("清理 AgenticScope 文件失败：" + file, e);
                        }
                    });
        } catch (IOException e) {
            throw new IllegalStateException("清理 AgenticScope 目录失败", e);
        }
    }

    private void createDirectory() {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("创建 AgenticScope 目录失败：" + directory, e);
        }
    }

    private Path fileFor(AgenticScopeKey key) {
        return directory.resolve(encode(key.agentId()) + SEPARATOR + encode(String.valueOf(key.memoryId())) + SUFFIX);
    }

    private AgenticScopeKey keyFromFile(Path file) {
        String fileName = file.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - SUFFIX.length());
        String[] parts = name.split(SEPARATOR, 2);
        if (parts.length != 2) {
            throw new IllegalStateException("非法 AgenticScope 文件名：" + fileName);
        }
        return new AgenticScopeKey(decode(parts[0]), decode(parts[1]));
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}

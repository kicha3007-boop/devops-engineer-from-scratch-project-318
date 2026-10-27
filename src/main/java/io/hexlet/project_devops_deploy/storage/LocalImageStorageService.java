package io.hexlet.project_devops_deploy.storage;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class LocalImageStorageService implements ImageStorageService {

    private final Path rootDir = Paths.get(System.getProperty("java.io.tmpdir"), "bulletin-images");

    @Override
    public String upload(String keyPrefix, MultipartFile file) {
        try {
            Files.createDirectories(rootDir);
            String extension =
                    Optional.ofNullable(file.getOriginalFilename())
                            .filter(StringUtils::hasText)
                            .map(
                                    name ->
                                            name.contains(".")
                                                    ? name.substring(name.lastIndexOf('.'))
                                                    : "")
                            .orElse("");
            String normalizedPrefix = keyPrefix.replaceAll("[^a-zA-Z0-9/_-]", "-");
            String key = "%s/%s%s".formatted(normalizedPrefix, UUID.randomUUID(), extension);
            Path destination = rootDir.resolve(key).normalize();
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            file.transferTo(destination);
            return key;
        } catch (IOException e) {
            throw new StorageException("Failed to store image locally", e);
        }
    }

    @Override
    public Optional<String> getUrl(String key) {
        if (!StringUtils.hasText(key)) {
            return Optional.empty();
        }
        Path destination = rootDir.resolve(key).normalize();
        if (!destination.startsWith(rootDir) || !Files.exists(destination)) {
            return Optional.empty();
        }
        return Optional.of(buildPublicUrl(key));
    }

    @Override
    public Optional<Resource> load(String key) {
        if (!StringUtils.hasText(key)) {
            return Optional.empty();
        }
        try {
            Path destination = rootDir.resolve(key).normalize();
            if (!destination.startsWith(rootDir) || !Files.exists(destination)) {
                return Optional.empty();
            }
            return Optional.of(new UrlResource(destination.toUri()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String buildPublicUrl(String key) {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/raw")
                    .queryParam("key", key)
                    .build(true)
                    .toUriString();
        } catch (IllegalStateException e) {
            return "/api/files/raw?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
        }
    }
}

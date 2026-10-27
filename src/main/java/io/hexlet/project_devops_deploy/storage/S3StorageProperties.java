package io.hexlet.project_devops_deploy.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(
        String bucket,
        String region,
        String endpoint,
        String accessKey,
        String secretKey,
        String cdnUrl) {}

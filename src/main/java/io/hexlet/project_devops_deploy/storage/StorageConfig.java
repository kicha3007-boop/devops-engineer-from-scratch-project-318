package io.hexlet.project_devops_deploy.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnMissingBean(ImageStorageService.class)
    public ImageStorageService imageStorageService(S3StorageProperties properties) {
        if (properties.bucket() != null
                && properties.region() != null
                && properties.accessKey() != null
                && properties.secretKey() != null) {
            return new S3ImageStorageService(properties);
        }
        return new LocalImageStorageService();
    }
}

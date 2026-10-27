package io.hexlet.project_devops_deploy.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class S3ImageStorageService implements ImageStorageService {

    private final S3Client client;
    private final S3StorageProperties properties;
    private final S3Presigner presigner;
    private static final Duration URL_TTL = Duration.ofHours(1);

    public S3ImageStorageService(S3StorageProperties properties) {
        this.properties = properties;
        StaticCredentialsProvider credentialsProvider =
                StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
        this.client = buildClient(properties, credentialsProvider);
        this.presigner = buildPresigner(properties, credentialsProvider);
    }

    private S3Client buildClient(
            S3StorageProperties props, StaticCredentialsProvider credentialsProvider) {
        S3ClientBuilder builder =
                S3Client.builder()
                        .region(Region.of(props.region()))
                        .credentialsProvider(credentialsProvider);

        if (StringUtils.hasText(props.endpoint())) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }

        return builder.build();
    }

    private S3Presigner buildPresigner(
            S3StorageProperties props, StaticCredentialsProvider credentialsProvider) {
        S3Presigner.Builder builder =
                S3Presigner.builder()
                        .region(Region.of(props.region()))
                        .credentialsProvider(credentialsProvider);
        if (StringUtils.hasText(props.endpoint())) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }
        return builder.build();
    }

    @Override
    public String upload(String keyPrefix, MultipartFile file) {
        String extension =
                Optional.ofNullable(file.getOriginalFilename())
                        .filter(StringUtils::hasText)
                        .map(
                                candidate ->
                                        candidate.contains(".")
                                                ? candidate.substring(candidate.lastIndexOf('.'))
                                                : "")
                        .orElse("");

        String key = "%s/%s%s".formatted(keyPrefix, UUID.randomUUID(), extension);

        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(key)
                        .contentType(
                                Optional.ofNullable(file.getContentType())
                                        .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                        .build();

        try {
            client.putObject(
                    request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            throw new StorageException("Failed to upload image", e);
        }

        return key;
    }

    @Override
    public Optional<String> getUrl(String key) {
        if (!StringUtils.hasText(key)) {
            return Optional.empty();
        }

        if (StringUtils.hasText(properties.cdnUrl())) {
            return Optional.of(applyBaseUrl(properties.cdnUrl(), key));
        }

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder().bucket(properties.bucket()).key(key).build();

        try {
            PresignedGetObjectRequest presigned =
                    presigner.presignGetObject(
                            builder ->
                                    builder.getObjectRequest(getObjectRequest)
                                            .signatureDuration(URL_TTL));
            return Optional.of(presigned.url().toString());
        } catch (Exception e) {
            throw new StorageException("Failed to generate image URL", e);
        }
    }

    private String applyBaseUrl(String baseUrl, String key) {
        String sanitizedBase = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return sanitizedBase + key;
    }
}

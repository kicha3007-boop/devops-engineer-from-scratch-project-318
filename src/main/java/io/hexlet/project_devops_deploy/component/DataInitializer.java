package io.hexlet.project_devops_deploy.component;

import io.hexlet.project_devops_deploy.model.Bulletin;
import io.hexlet.project_devops_deploy.repository.BulletinRepository;
import io.hexlet.project_devops_deploy.storage.ImageStorageService;
import io.hexlet.project_devops_deploy.util.ModelGenerator;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
@Profile("!test")
@ConditionalOnProperty(
        prefix = "app.seed",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private static final int BULLETIN_SEED_COUNT = 10;
    private static final String SAMPLE_IMAGES_PATTERN = "classpath:/sample_images/*";

    private final BulletinRepository repository;
    private final ModelGenerator modelGenerator;
    private final ImageStorageService imageStorageService;

    private Resource[] sampleImages = new Resource[0];
    private final ResourcePatternResolver resourceResolver =
            new PathMatchingResourcePatternResolver();

    @PostConstruct
    void loadResources() {
        try {
            sampleImages = resourceResolver.getResources(SAMPLE_IMAGES_PATTERN);
            log.info("Loaded {} sample images for seeding", sampleImages.length);
        } catch (IOException e) {
            log.warn("Failed to load sample images: {}", e.getMessage());
            sampleImages = new Resource[0];
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }

        List<Resource> images = Arrays.stream(sampleImages).filter(Resource::exists).toList();

        IntStream.range(0, BULLETIN_SEED_COUNT)
                .mapToObj(
                        i -> {
                            var bulletin = modelGenerator.generateBulletin();
                            attachSampleImageIfPossible(bulletin, images, i);
                            return bulletin;
                        })
                .forEach(repository::save);
    }

    private void attachSampleImageIfPossible(Bulletin bulletin, List<Resource> images, int index) {
        if (images.isEmpty()) {
            return;
        }

        Resource image = images.get(index % images.size());
        try {
            byte[] content;
            try (var inputStream = image.getInputStream()) {
                content = StreamUtils.copyToByteArray(inputStream);
            }
            String filename = image.getFilename() != null ? image.getFilename() : "sample-image";
            String contentType = URLConnection.guessContentTypeFromName(filename);
            MultipartFile file = new InMemoryMultipartFile("file", filename, contentType, content);
            String key = imageStorageService.upload("bulletins/sample", file);
            bulletin.setImageKey(key);
        } catch (IOException e) {
            log.warn("Failed to attach image {}: {}", image.getFilename(), e.getMessage());
        }
    }

    private static final class InMemoryMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private InMemoryMultipartFile(
                String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }
}

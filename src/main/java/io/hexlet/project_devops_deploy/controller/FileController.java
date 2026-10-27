package io.hexlet.project_devops_deploy.controller;

import io.hexlet.project_devops_deploy.dto.FileUploadResponse;
import io.hexlet.project_devops_deploy.exception.ResourceNotFoundException;
import io.hexlet.project_devops_deploy.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final ImageStorageService imageStorageService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse upload(@RequestParam("file") MultipartFile file) {
        String key = imageStorageService.upload("bulletins", file);
        String url = imageStorageService.getUrl(key).orElse(null);

        return FileUploadResponse.builder().key(key).url(url).build();
    }

    @GetMapping("/view")
    public FileUploadResponse view(@RequestParam("key") String key) {
        String url =
                imageStorageService
                        .getUrl(key)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Image %s not found".formatted(key)));

        return FileUploadResponse.builder().key(key).url(url).build();
    }

    @GetMapping(value = "/raw", produces = MediaType.ALL_VALUE)
    public ResponseEntity<Resource> raw(@RequestParam("key") String key) {
        Resource resource =
                imageStorageService
                        .load(key)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Image %s not found".formatted(key)));

        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try {
            contentType = resource.getURL().openConnection().getContentType();
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=" + resource.getFilename())
                .body(resource);
    }
}

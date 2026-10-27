package io.hexlet.project_devops_deploy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FileUploadResponse {

    private final String key;
    private final String url;
}

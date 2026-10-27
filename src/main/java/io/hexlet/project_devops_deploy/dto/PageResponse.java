package io.hexlet.project_devops_deploy.dto;

import java.util.List;

public record PageResponse<T>(List<T> data, long total) {}

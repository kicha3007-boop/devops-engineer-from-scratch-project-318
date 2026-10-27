package io.hexlet.project_devops_deploy.service;

import io.hexlet.project_devops_deploy.dto.BulletinDto;
import io.hexlet.project_devops_deploy.exception.ResourceNotFoundException;
import io.hexlet.project_devops_deploy.mapper.BulletinMapper;
import io.hexlet.project_devops_deploy.repository.BulletinRepository;
import io.hexlet.project_devops_deploy.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BulletinImageService {

    private final BulletinRepository repository;
    private final BulletinMapper mapper;
    private final ImageStorageService storageService;

    @Transactional
    public BulletinDto upload(Long id, MultipartFile file) {
        var bulletin =
                repository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Bulletin %d not found".formatted(id)));

        String key = storageService.upload("bulletins/" + id, file);
        bulletin.setImageKey(key);
        repository.save(bulletin);
        return mapper.toDto(bulletin);
    }
}

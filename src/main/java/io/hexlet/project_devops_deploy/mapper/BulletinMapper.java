package io.hexlet.project_devops_deploy.mapper;

import io.hexlet.project_devops_deploy.dto.BulletinDto;
import io.hexlet.project_devops_deploy.dto.BulletinRequest;
import io.hexlet.project_devops_deploy.model.Bulletin;
import io.hexlet.project_devops_deploy.storage.ImageStorageService;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class BulletinMapper {

    @Autowired private ImageStorageService imageStorageService;

    @Mapping(target = "imageUrl", ignore = true)
    public abstract BulletinDto toDto(Bulletin entity);

    @AfterMapping
    protected void fillImageUrl(Bulletin entity, @MappingTarget BulletinDto dto) {
        dto.setImageUrl(imageStorageService.getUrl(entity.getImageKey()).orElse(null));
    }

    @BeanMapping(ignoreUnmappedSourceProperties = "imageUrl")
    public abstract Bulletin toEntity(BulletinDto dto);

    public abstract Bulletin toEntity(BulletinRequest request);

    public abstract void updateEntity(BulletinRequest request, @MappingTarget Bulletin bulletin);
}

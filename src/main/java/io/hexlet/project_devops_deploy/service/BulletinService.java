package io.hexlet.project_devops_deploy.service;

import io.hexlet.project_devops_deploy.dto.BulletinDto;
import io.hexlet.project_devops_deploy.dto.BulletinRequest;
import io.hexlet.project_devops_deploy.dto.PageResponse;
import io.hexlet.project_devops_deploy.exception.ResourceNotFoundException;
import io.hexlet.project_devops_deploy.mapper.BulletinMapper;
import io.hexlet.project_devops_deploy.model.Bulletin;
import io.hexlet.project_devops_deploy.repository.BulletinRepository;
import io.hexlet.project_devops_deploy.specification.BulletinSpecifications;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
public class BulletinService {

    @Autowired private final BulletinRepository repository;

    @Autowired private final BulletinMapper mapper;

    public BulletinDto create(BulletinRequest request) {
        Bulletin bulletin = mapper.toEntity(request);
        return mapper.toDto(repository.save(bulletin));
    }

    @Transactional(readOnly = true)
    public PageResponse<BulletinDto> findAll(
            int page, int perPage, String sort, Sort.Direction order, Map<String, String> filters) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.max(perPage, 1);
        String sortProperty = StringUtils.hasText(sort) ? sort : "createdAt";
        PageRequest pageable = PageRequest.of(pageIndex, pageSize, Sort.by(order, sortProperty));

        Map<String, String> safeFilters = filters == null ? Map.of() : filters;
        Specification<Bulletin> specification = BulletinSpecifications.fromFilters(safeFilters);
        Page<BulletinDto> result = repository.findAll(specification, pageable).map(mapper::toDto);
        return new PageResponse<>(result.getContent(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public BulletinDto findById(Long id) {
        return mapper.toDto(getBulletin(id));
    }

    private Bulletin getBulletin(Long id) {
        return repository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Bulletin %d not found".formatted(id)));
    }

    public BulletinDto update(Long id, BulletinRequest request) {
        Bulletin bulletin = getBulletin(id);
        mapper.updateEntity(request, bulletin);
        return mapper.toDto(repository.save(bulletin));
    }

    public void delete(Long id) {
        Bulletin bulletin = getBulletin(id);
        repository.delete(bulletin);
    }
}

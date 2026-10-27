package io.hexlet.project_devops_deploy.repository;

import io.hexlet.project_devops_deploy.model.Bulletin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BulletinRepository
        extends JpaRepository<Bulletin, Long>, JpaSpecificationExecutor<Bulletin> {}

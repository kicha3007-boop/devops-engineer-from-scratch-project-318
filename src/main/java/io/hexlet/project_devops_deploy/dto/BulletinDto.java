package io.hexlet.project_devops_deploy.dto;

import io.hexlet.project_devops_deploy.model.bulletin.BulletinState;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinDto {

    private Long id;
    private String title;
    private String description;
    private BulletinState state;
    private String contact;
    private BigDecimal price;
    private String imageKey;
    private String imageUrl;
}

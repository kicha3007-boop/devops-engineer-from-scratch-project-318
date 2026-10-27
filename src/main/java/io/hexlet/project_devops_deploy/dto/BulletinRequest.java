package io.hexlet.project_devops_deploy.dto;

import io.hexlet.project_devops_deploy.model.bulletin.BulletinState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class BulletinRequest {

    @NotBlank private String title;

    @NotBlank private String description;

    @NotNull private BulletinState state;

    @NotBlank private String contact;

    @NotNull @PositiveOrZero private BigDecimal price;

    private String imageKey;
}

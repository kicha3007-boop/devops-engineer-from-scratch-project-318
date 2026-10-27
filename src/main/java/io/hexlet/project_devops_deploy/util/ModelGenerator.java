package io.hexlet.project_devops_deploy.util;

import io.hexlet.project_devops_deploy.model.Bulletin;
import io.hexlet.project_devops_deploy.model.bulletin.BulletinState;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import lombok.Getter;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.Select;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ModelGenerator {

    private Model<Bulletin> bulletinModel;
    private final Faker faker;

    public ModelGenerator(Faker faker) {
        this.faker = faker;
    }

    public Bulletin generateBulletin() {
        return Instancio.of(bulletinModel).create();
    }

    @PostConstruct
    private void init() {
        bulletinModel =
                Instancio.of(Bulletin.class)
                        .ignore(Select.field(Bulletin::getId))
                        .ignore(Select.field(Bulletin::getCreatedAt))
                        .ignore(Select.field(Bulletin::getUpdatedAt))
                        .supply(Select.field(Bulletin::getTitle), () -> faker.book().title())
                        .supply(
                                Select.field(Bulletin::getDescription),
                                () -> faker.lorem().paragraph(3))
                        .supply(
                                Select.field(Bulletin::getState),
                                () -> faker.options().option(BulletinState.values()))
                        .supply(
                                Select.field(Bulletin::getContact),
                                () -> faker.phoneNumber().phoneNumber())
                        .supply(
                                Select.field(Bulletin::getPrice),
                                () ->
                                        BigDecimal.valueOf(
                                                faker.number().randomDouble(2, 10, 10_000)))
                        .supply(
                                Select.field(Bulletin::getImageKey),
                                () -> "bulletins/%s.jpg".formatted(faker.internet().uuid()))
                        .toModel();
    }
}

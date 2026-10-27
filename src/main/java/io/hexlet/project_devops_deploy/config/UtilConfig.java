package io.hexlet.project_devops_deploy.config;

import net.datafaker.Faker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UtilConfig {

    @Bean
    public Faker faker() {
        return new Faker();
    }
}

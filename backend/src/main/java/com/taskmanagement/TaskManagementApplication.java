package com.taskmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point của ứng dụng Spring Boot.
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * Spring Boot sẽ tự động:
 *   - Quét toàn bộ các class trong package com.taskmanagement
 *   - Tự cấu hình các bean (JPA, Security, Redis,...) dựa trên pom.xml
 *   - Khởi động embedded Tomcat server trên port 8080
 */
@SpringBootApplication
@EntityScan("com.taskmanagement.model")
@EnableJpaRepositories("com.taskmanagement.repository")
@EnableCaching   // Bật tính năng Cache (Redis)
public class TaskManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagementApplication.class, args);
    }
}


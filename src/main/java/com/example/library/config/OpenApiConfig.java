package com.example.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Library Management API")
                .version("2.0.0")
                .description("REST API của hệ thống quản lý mượn trả sách và phí phạt trả muộn. "
                        + "Giao diện chính dùng Thymeleaf tại http://localhost:8080/, "
                        + "các API dưới đây phục vụ tích hợp và kiểm thử nhanh.")
                .contact(new Contact().name("Library Team")));
    }
}

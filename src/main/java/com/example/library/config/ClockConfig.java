package com.example.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Inject Clock thay vì gọi LocalDate.now() trực tiếp để unit test cố định được "hôm nay". */
    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}

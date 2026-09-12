package com.enosistudio.bruine;

import com.enosistudio.bruine.common.DateFormatter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class DateFormatterTestConfig {
    @Bean
    DateFormatter dateFormatter() {
        return new DateFormatter();
    }

}

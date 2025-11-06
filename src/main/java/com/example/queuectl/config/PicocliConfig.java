package com.example.queuectl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationContext;
import picocli.CommandLine;

@Configuration
public class PicocliConfig {
    @Bean
    public CommandLine.IFactory picocliFactory(ApplicationContext ctx) {
        // Try to create from Spring; if not a Spring bean, fall back to default constructor
        return new CommandLine.IFactory() {
            @Override
            public <K> K create(Class<K> cls) throws Exception {
                try {
                    return ctx.getBean(cls);
                } catch (Exception notABean) {
                    return cls.getDeclaredConstructor().newInstance();
                }
            }
        };
    }
}

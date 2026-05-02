package com.lpu.authservice.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//  ModelMapper Bean - enables automatic DTO mapping
// Usage: modelMapper.map(sourceObject, TargetClass.class)
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}

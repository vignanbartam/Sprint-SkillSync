package com.lpu.reviewservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.lpu.reviewservice.config.FeignConfig;

@FeignClient(name = "sessionservice",configuration = FeignConfig.class)
public interface SessionClient {

    @GetMapping("/session/check")
    boolean hasCompletedSession(
            @RequestParam Long userId,
            @RequestParam Long mentorId
    );
}
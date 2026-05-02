package com.lpu.sessionservice.client;

import com.lpu.sessionservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "authservice")
public interface AuthClient {

    @GetMapping("/auth/internal/user/{id}")
    UserDTO getUserById(@PathVariable Long id);
}

package com.credlink.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping({"/", "/api/v1", "/api/v1/"})
    public ApiResponse<Map<String, String>> rootInfo() {
        return ApiResponse.ok(Map.of(
            "app", "CredLink API",
            "status", "UP",
            "version", "1.0.0"
        ));
    }
}

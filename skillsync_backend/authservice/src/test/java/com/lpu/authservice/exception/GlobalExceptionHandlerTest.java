package com.lpu.authservice.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testHandleCustomException() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleCustom(new CustomException("Email already exists"));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Email already exists", resp.getBody().get("message"));
    }

    @Test
    void testHandleRuntimeException() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleRuntime(new RuntimeException("Something went wrong"));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Something went wrong", resp.getBody().get("message"));
    }

    @Test
    void testHandleEntityNotFoundException() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleNotFound(new EntityNotFoundException("User not found"));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("User not found", resp.getBody().get("message"));
    }

    @Test
    void testHandleAccessDeniedException() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleAccessDenied(new AccessDeniedException("Forbidden"));
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertTrue(resp.getBody().get("message").toString().contains("Forbidden"));
    }

    @Test
    void testHandleGeneralException() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleGeneral(new Exception("Unexpected error"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertTrue(resp.getBody().get("message").toString().contains("Unexpected error"));
    }

    @Test
    void testResponseBodyContainsTimestampAndStatus() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleCustom(new CustomException("test"));
        assertNotNull(resp.getBody().get("timestamp"));
        assertEquals(400, resp.getBody().get("status"));
        assertNotNull(resp.getBody().get("error"));
    }
}

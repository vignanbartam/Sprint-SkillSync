package com.lpu.reviewservice.exception;

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
                handler.handleCustom(new CustomException("Already reviewed"));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Already reviewed", resp.getBody().get("message"));
    }

    @Test
    void testHandleRuntimeException() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleRuntime(new RuntimeException("Runtime error"));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void testHandleEntityNotFoundException() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleNotFound(new EntityNotFoundException("Review not found"));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void testHandleAccessDeniedException() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleAccessDenied(new AccessDeniedException("Forbidden"));
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void testHandleGeneralException() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleGeneral(new Exception("General error"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void testResponseBodyHasAllFields() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleCustom(new CustomException("test"));
        assertNotNull(resp.getBody().get("timestamp"));
        assertEquals(400, resp.getBody().get("status"));
        assertNotNull(resp.getBody().get("error"));
    }
}

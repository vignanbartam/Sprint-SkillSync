package com.lpu.groupservice.config;

import com.lpu.groupservice.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

class UserContextTest {

    private final UserContext userContext = new UserContext();

    @Test
    void testGetUserId_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");

        Long userId = userContext.getUserId(request);
        assertEquals(42L, userId);
    }

    @Test
    void testGetUserId_MissingHeader_ThrowsCustomException() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(CustomException.class, () -> userContext.getUserId(request));
    }

    @Test
    void testGetUserId_InvalidValue_ThrowsCustomException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "not-a-number");

        assertThrows(CustomException.class, () -> userContext.getUserId(request));
    }

    @Test
    void testGetUserId_BlankHeader_ThrowsCustomException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "  ");

        assertThrows(CustomException.class, () -> userContext.getUserId(request));
    }
}

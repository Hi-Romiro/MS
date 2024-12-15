package com.itm.space.backendresources;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private Keycloak keycloak;

    private final String realm = "ITM";

    private final UserRequest userRequest = new UserRequest(
            "testUsername",
            "testEmail@example.com",
            "testPassword",
            "TestFirstName",
            "TestLastName"
    );

    @BeforeEach
    public void setUp() {
        // Удаляем пользователя перед каждым тестом
        removeTestUserIfExists();
    }

    @AfterEach
    public void tearDown() {
        // Удаляем пользователя после каждого теста
        removeTestUserIfExists();
    }

    /**
     * Удаляет тестового пользователя, если он существует
     */
    private void removeTestUserIfExists() {
        List<UserRepresentation> existingUsers = keycloak.realm(realm).users().search(userRequest.getUsername());
        if (!existingUsers.isEmpty()) {
            keycloak.realm(realm).users().get(existingUsers.get(0).getId()).remove();
        }
    }

    /**
     * Тест: Создание пользователя
     */
    @Test
    @WithMockUser(roles = "MODERATOR")
    public void testCreateUser() throws Exception {
        mvc.perform(requestWithContent(post("/api/users"), userRequest))
                .andExpect(status().isOk());

        UserRepresentation createdUser = keycloak.realm("ITM").users().search(userRequest.getUsername()).get(0);

        assertEquals(userRequest.getUsername().toLowerCase(), createdUser.getUsername());
        assertEquals(userRequest.getEmail().toLowerCase(), createdUser.getEmail());
        assertEquals(userRequest.getFirstName(), createdUser.getFirstName());
        assertEquals(userRequest.getLastName(), createdUser.getLastName());

        keycloak.realm("ITM").users().get(createdUser.getId()).remove();
    }

    /**
     * Тест: Получение пользователя по ID
     */
    @Test
    @WithMockUser(roles = "MODERATOR")
    public void testGetUserById() throws Exception {
        mvc.perform(requestWithContent(post("/api/users"), userRequest))
                .andExpect(status().isOk());

        UserRepresentation createdUser = keycloak.realm("ITM").users().search(userRequest.getUsername()).get(0);
        UserResponse userResponse = userService.getUserById(UUID.fromString(createdUser.getId()));

        assertNotNull(userResponse);
        assertEquals(userRequest.getEmail().toLowerCase(), userResponse.getEmail());
        assertEquals(userRequest.getFirstName(), userResponse.getFirstName());
        assertEquals(userRequest.getLastName(), userResponse.getLastName());

        keycloak.realm("ITM").users().get(createdUser.getId()).remove();
    }
}
package com.itm.space.backendresources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Тест: Успешное создание пользователя
     */
    @Test
    @WithMockUser(roles = "MODERATOR")
    public void createUser() throws Exception {

        UserRequest userRequest = new UserRequest("username", "email@example.com", "password", "firstName", "lastName");

        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk());

        verify(userService, times(1)).createUser(any(UserRequest.class));
    }

    /**
     * Тест: Создание пользователя с некорректными данными
     */
    @Test
    @WithMockUser(roles = "MODERATOR")
    public void createUser_WithInvalidData() throws Exception {
        UserRequest invalidRequest = new UserRequest("", "invalid-email", "", "", "");

        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email should be valid"))
                .andExpect(jsonPath("$.username").value("Username should be between 2 and 30 characters long"))
                .andExpect(jsonPath("$.password").value("Password should be greater than 4 characters long"))
                .andExpect(jsonPath("$.firstName").value("must not be blank"))
                .andExpect(jsonPath("$.lastName").value("must not be blank"));
    }

    /**
     * Тест: Успешное получение пользователя по ID
     */
    @Test
    @WithMockUser(roles = "MODERATOR")
    public void getUserById() throws Exception {
        // Генерация ID
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse("firstName", "lastName", "email@example.com", null, null);

        Mockito.when(userService.getUserById(userId)).thenReturn(userResponse);

        mvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("firstName"))
                .andExpect(jsonPath("$.lastName").value("lastName"))
                .andExpect(jsonPath("$.email").value("email@example.com"))
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.groups").doesNotExist());

        verify(userService, times(1)).getUserById(userId);
    }

    /**
     * Тест: Обработка ошибки при отсутствии пользователя
     */
    @Test
    @WithMockUser(roles = "MODERATOR")
    public void getUserById_WhenUserNotFound() throws Exception {
        UUID userId = UUID.randomUUID();

        Mockito.when(userService.getUserById(userId))
                .thenThrow(new BackendResourcesException("User not found", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/users/" + userId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    /**
     * Тест: Проверка работы эндпоинта "hello"
     */
    @Test
    @WithMockUser(username = "testModerator", roles = "MODERATOR")
    public void helloEndpoint() throws Exception {
        mvc.perform(get("/api/users/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("testModerator"));
    }
}
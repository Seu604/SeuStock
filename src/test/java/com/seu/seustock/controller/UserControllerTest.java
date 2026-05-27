package com.seu.seustock.controller;

import com.seu.seustock.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 웹 계층 테스트.
 * <p>
 * 검증 범위:
 * <ul>
 *   <li>Security: 공개 엔드포인트 접근 허용, CSRF 보호</li>
 *   <li>Validation: 잘못된 폼 제출 시 register 뷰 재반환</li>
 *   <li>Response Shape: 뷰 이름, 모델 속성, 리다이렉트 URL</li>
 * </ul>
 */
class UserControllerTest extends AbstractControllerTest {

    @MockitoBean
    private UserService userService;

    // ── Security ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /login - 미인증 상태 → 200 login 뷰 (permitAll)")
    void getLogin_whenUnauthenticated_returns200() throws Exception {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk())
               .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("GET /login - 이미 인증된 상태 → / 로 리다이렉트")
    void getLogin_whenAuthenticated_redirectsToRoot() throws Exception {
        mockMvc.perform(get("/login").with(user("testuser")))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("POST /register - CSRF 토큰 없음 → 403 Forbidden")
    void postRegister_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "validuser")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123"))
               .andExpect(status().isForbidden());
    }

    // ── Response Shape ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /register → 200 register 뷰, form 모델 속성 존재")
    void getRegister_returns200WithRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
               .andExpect(status().isOk())
               .andExpect(view().name("register"))
               .andExpect(model().attributeExists("form"));
    }

    @Test
    @DisplayName("GET /register?success → model.success == true")
    void getRegister_withSuccessParam_setsSuccessAttributeTrue() throws Exception {
        mockMvc.perform(get("/register").param("success", ""))
               .andExpect(status().isOk())
               .andExpect(view().name("register"))
               .andExpect(model().attribute("success", true));
    }

    @Test
    @DisplayName("GET /register/check-username (파라미터 없음) → 프래그먼트 반환, empty=true")
    void getCheckUsername_withBlankUsername_returnsEmptyFlag() throws Exception {
        mockMvc.perform(get("/register/check-username"))
               .andExpect(status().isOk())
               .andExpect(view().name("fragments/username-check :: result"))
               .andExpect(model().attribute("empty", true));
    }

    @Test
    @DisplayName("GET /register/check-username?username=taken → taken=true")
    void getCheckUsername_withTakenUsername_returnsTakenFlag() throws Exception {
        given(userService.existsByUsername("taken")).willReturn(true);

        mockMvc.perform(get("/register/check-username").param("username", "taken"))
               .andExpect(status().isOk())
               .andExpect(view().name("fragments/username-check :: result"))
               .andExpect(model().attribute("taken", true))
               .andExpect(model().attribute("empty", false));
    }

    @Test
    @DisplayName("POST /register 정상 폼 → /register?success 리다이렉트")
    void postRegister_withValidForm_redirectsToSuccess() throws Exception {
        given(userService.existsByUsername("newuser1")).willReturn(false);

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "newuser1")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/register?success"));
    }

    // ── Validation ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register - username 빈 값 → register 뷰 (200)")
    void postRegister_withBlankUsername_returnsRegisterView() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123"))
               .andExpect(status().isOk())
               .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("POST /register - username 4자 미만 → register 뷰 (@Size min=4)")
    void postRegister_withTooShortUsername_returnsRegisterView() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "ab")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123"))
               .andExpect(status().isOk())
               .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("POST /register - username 패턴 불일치 (공백 포함) → register 뷰 (@Pattern)")
    void postRegister_withInvalidUsernamePattern_returnsRegisterView() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "ab cd")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123"))
               .andExpect(status().isOk())
               .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("POST /register - password 8자 미만 → register 뷰 (@Size min=8)")
    void postRegister_withTooShortPassword_returnsRegisterView() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "validuser")
                        .param("password", "short")
                        .param("passwordConfirm", "short"))
               .andExpect(status().isOk())
               .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("POST /register - password와 passwordConfirm 불일치 → register 뷰")
    void postRegister_withMismatchedPasswords_returnsRegisterView() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "validuser")
                        .param("password", "password123")
                        .param("passwordConfirm", "different1"))
               .andExpect(status().isOk())
               .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("POST /register - 이미 사용 중인 username → register 뷰 (중복 검사)")
    void postRegister_withDuplicateUsername_returnsRegisterView() throws Exception {
        given(userService.existsByUsername(anyString())).willReturn(true);

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "existuser")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123"))
               .andExpect(status().isOk())
               .andExpect(view().name("register"));
    }
}

package com.seu.seustock.controller;

import com.seu.seustock.model.dto.UserDTO;
import com.seu.seustock.model.form.LoginForm;
import com.seu.seustock.model.form.UserRegistrationForm;
import com.seu.seustock.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/register")
    public String registerForm(@RequestParam(required = false) String success, Model model) {
        model.addAttribute("form", new UserRegistrationForm());
        model.addAttribute("success", success != null);
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") UserRegistrationForm form,
                           BindingResult result) {
        if (!form.getPassword().equals(form.getPasswordConfirm())) {
            result.rejectValue("passwordConfirm", "match", "비밀번호가 일치하지 않습니다.");
        }

        if (!result.hasFieldErrors("username") && userService.existsByUsername(form.getUsername())) {
            result.rejectValue("username", "duplicate", "이미 사용 중인 아이디입니다.");
        }

        if (result.hasErrors()) {
            return "register";
        }

        userService.register(form);
        return "redirect:/register?success";
    }

    @GetMapping("/register/check-username")
    public String checkUsername(@RequestParam(defaultValue = "") String username, Model model) {
        model.addAttribute("username", username);
        model.addAttribute("taken", !username.isBlank() && userService.existsByUsername(username));
        model.addAttribute("empty", username.isBlank());
        return "fragments/username-check :: result";
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String redirect,
                            HttpSession session, Model model) {
        if (session.getAttribute("loginUser") != null) {
            return "redirect:/";
        }
        model.addAttribute("form", new LoginForm());
        model.addAttribute("redirect", redirect);
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("form") LoginForm form,
                        BindingResult result,
                        @RequestParam(required = false) String redirect,
                        HttpServletRequest request,
                        HttpSession session) {
        if (result.hasErrors()) {
            return "login";
        }

        Optional<UserDTO> user = userService.authenticate(form);
        if (user.isEmpty()) {
            result.reject("invalidCredentials", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "login";
        }

        session.invalidate();
        request.getSession(true).setAttribute("loginUser", user.get().getUsername());

        if (redirect != null && !redirect.isBlank()) {
            return "redirect:" + redirect;
        }
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}

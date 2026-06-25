package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.dto.LoginRequest;
import com.epam.rd.autocode.spring.project.dto.RegistrationRequest;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.security.JwtAuthenticationFilter;
import com.epam.rd.autocode.spring.project.security.JwtService;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.validation.PasswordRequirementsValidator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private ClientService clientService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordRequirementsValidator passwordRequirementsValidator;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                authenticationManager,
                jwtService,
                clientService,
                passwordEncoder,
                passwordRequirementsValidator
        );
    }

    @Test
    void loginPageAddsLoginRequest() {
        Model model = new ExtendedModelMap();

        String view = controller.loginPage(model);

        assertEquals("login", view);
        assertTrue(model.containsAttribute("loginRequest"));
    }

    @Test
    void loginReturnsRedirectAndSetsCookieOnSuccess() {
        LoginRequest request = loginRequest("client@example.com", "Password1");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "loginRequest");
        Authentication authentication = mock(Authentication.class);
        HttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(authentication)).thenReturn("token-value");

        String view = controller.login(request, bindingResult, response);

        assertEquals("redirect:/", view);
        Cookie cookie = ((MockHttpServletResponse) response).getCookie(JwtAuthenticationFilter.COOKIE_NAME);
        assertNotNull(cookie);
        assertEquals("token-value", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals(24 * 60 * 60, cookie.getMaxAge());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(authentication);
    }

    @Test
    void loginReturnsLoginViewWhenBindingHasErrors() {
        LoginRequest request = loginRequest("client@example.com", "Password1");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "loginRequest");
        bindingResult.rejectValue("email", "validation.email");

        String view = controller.login(request, bindingResult, new MockHttpServletResponse());

        assertEquals("login", view);
        verifyNoInteractions(authenticationManager, jwtService);
    }

    @Test
    void loginReturnsLoginViewWhenAccountIsBlocked() {
        LoginRequest request = loginRequest("client@example.com", "Password1");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "loginRequest");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("blocked"));

        String view = controller.login(request, bindingResult, new MockHttpServletResponse());

        assertEquals("login", view);
        assertTrue(bindingResult.hasErrors());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void registerPageAddsRegistrationRequest() {
        Model model = new ExtendedModelMap();

        String view = controller.registerPage(model);

        assertEquals("register", view);
        assertTrue(model.containsAttribute("registrationRequest"));
    }

    @Test
    void registerReturnsRedirectOnSuccess() {
        RegistrationRequest request = registrationRequest("new@example.com", "Alice", "Password1");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "registrationRequest");

        when(passwordRequirementsValidator.validate("Password1")).thenReturn(List.of());
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");

        String view = controller.register(request, bindingResult);

        assertEquals("redirect:/auth/login", view);
        verify(passwordEncoder).encode("Password1");
        verify(clientService).addClient(any(ClientDTO.class));
    }

    @Test
    void registerReturnsRegisterViewOnPasswordErrors() {
        RegistrationRequest request = registrationRequest("new@example.com", "Alice", "short");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "registrationRequest");

        when(passwordRequirementsValidator.validate("short")).thenReturn(List.of("password.min"));

        String view = controller.register(request, bindingResult);

        assertEquals("register", view);
        assertTrue(bindingResult.hasErrors());
        verify(clientService, never()).addClient(any());
    }

    @Test
    void registerReturnsRegisterViewWhenClientAlreadyExists() {
        RegistrationRequest request = registrationRequest("new@example.com", "Alice", "Password1");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "registrationRequest");

        when(passwordRequirementsValidator.validate("Password1")).thenReturn(List.of());
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(clientService.addClient(any(ClientDTO.class))).thenThrow(new AlreadyExistException("exists"));

        String view = controller.register(request, bindingResult);

        assertEquals("register", view);
        assertTrue(bindingResult.hasErrors());
    }

    @Test
    void logoutClearsCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = controller.logout(response);

        assertEquals("redirect:/auth/login", view);
        Cookie cookie = response.getCookie(JwtAuthenticationFilter.COOKIE_NAME);
        assertNotNull(cookie);
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
    }

    private static LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static RegistrationRequest registrationRequest(String email, String name, String password) {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(email);
        request.setName(name);
        request.setPassword(password);
        request.setConfirmPassword(password);
        return request;
    }
}

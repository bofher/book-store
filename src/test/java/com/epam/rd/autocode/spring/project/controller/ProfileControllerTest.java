package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.dto.ClientProfileForm;
import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.dto.EmployeeProfileForm;
import com.epam.rd.autocode.spring.project.security.JwtAuthenticationFilter;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ClientService clientService;
    @Mock
    private EmployeeService employeeService;

    private ProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new ProfileController(clientService, employeeService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void profileLoadsEmployeeProfileForEmployee() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "employee@example.com",
                "pass",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        EmployeeDTO employee = employee("employee@example.com", "Bob");
        Model model = new ExtendedModelMap();

        when(employeeService.getEmployeeByEmail("employee@example.com")).thenReturn(employee);

        String view = controller.profile(authentication, model);

        assertEquals("profile", view);
        assertEquals(employee, model.getAttribute("employee"));
        assertTrue(model.containsAttribute("employeeProfile"));
    }

    @Test
    void profileLoadsClientProfileForClient() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("client@example.com", "pass");
        ClientDTO client = client("client@example.com", "Alice");
        Model model = new ExtendedModelMap();

        when(clientService.getClientByEmail("client@example.com")).thenReturn(client);

        String view = controller.profile(authentication, model);

        assertEquals("profile", view);
        assertEquals(client, model.getAttribute("client"));
        assertTrue(model.containsAttribute("clientProfile"));
    }

    @Test
    void updateClientReturnsProfileWhenBindingHasErrors() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("client@example.com", "pass");
        ClientProfileForm form = new ClientProfileForm("Alice", new BigDecimal("10.00"));
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "clientProfile");
        bindingResult.rejectValue("name", "validation.required");
        Model model = new ExtendedModelMap();
        when(clientService.getClientByEmail("client@example.com")).thenReturn(client("client@example.com", "Alice"));

        String view = controller.updateClient(form, bindingResult, authentication, model);

        assertEquals("profile", view);
        assertTrue(model.containsAttribute("clientProfile"));
        verify(clientService).getClientByEmail("client@example.com");
        verify(clientService, never()).updateClientByEmail(any(String.class), any(ClientDTO.class));
    }

    @Test
    void updateClientUpdatesBalanceAndRedirects() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("client@example.com", "pass");
        ClientProfileForm form = new ClientProfileForm("Alice Updated", new BigDecimal("5.50"));
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "clientProfile");
        ClientDTO current = client("client@example.com", "Alice");
        current.setBalance(new BigDecimal("10.00"));

        when(clientService.getClientByEmail("client@example.com")).thenReturn(current);

        String view = controller.updateClient(form, bindingResult, authentication, new ExtendedModelMap());

        assertEquals("redirect:/profile", view);
        verify(clientService).updateClientByEmail(any(String.class), any(ClientDTO.class));
    }

    @Test
    void updateEmployeeReturnsProfileWhenBindingHasErrors() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("employee@example.com", "pass");
        EmployeeProfileForm form = new EmployeeProfileForm("Bob", "+380000000000", LocalDate.of(1990, 1, 1));
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "employeeProfile");
        bindingResult.rejectValue("name", "validation.required");
        Model model = new ExtendedModelMap();
        when(employeeService.getEmployeeByEmail("employee@example.com")).thenReturn(employee("employee@example.com", "Bob"));

        String view = controller.updateEmployee(form, bindingResult, authentication, model);

        assertEquals("profile", view);
        verify(employeeService).getEmployeeByEmail("employee@example.com");
        verify(employeeService, never()).updateEmployeeByEmail(any(String.class), any(EmployeeDTO.class));
    }

    @Test
    void updateEmployeeRedirectsOnSuccess() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("employee@example.com", "pass");
        EmployeeProfileForm form = new EmployeeProfileForm("Bob Updated", "+380000000001", LocalDate.of(1990, 1, 1));
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "employeeProfile");
        EmployeeDTO current = employee("employee@example.com", "Bob");

        when(employeeService.getEmployeeByEmail("employee@example.com")).thenReturn(current);

        String view = controller.updateEmployee(form, bindingResult, authentication, new ExtendedModelMap());

        assertEquals("redirect:/profile", view);
        verify(employeeService).updateEmployeeByEmail(any(String.class), any(EmployeeDTO.class));
    }

    @Test
    void deleteOwnAccountClearsCookieAndSecurityContext() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("client@example.com", "pass");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = controller.deleteOwnAccount(authentication, response);

        assertEquals("redirect:/auth/login", view);
        assertNotNull(response.getCookie(JwtAuthenticationFilter.COOKIE_NAME));
        assertEquals("", response.getCookie(JwtAuthenticationFilter.COOKIE_NAME).getValue());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(clientService).deleteClientByEmail("client@example.com");
    }

    private static ClientDTO client(String email, String name) {
        return new ClientDTO(email, "password", name, BigDecimal.ZERO, false);
    }

    private static EmployeeDTO employee(String email, String name) {
        return new EmployeeDTO(email, "password", name, "+380000000000", LocalDate.of(1990, 1, 1));
    }
}

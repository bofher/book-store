package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.dto.EmployeeRegistrationRequest;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import com.epam.rd.autocode.spring.project.service.OrderService;
import com.epam.rd.autocode.spring.project.validation.PasswordRequirementsValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    private ClientService clientService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private OrderService orderService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordRequirementsValidator passwordRequirementsValidator;

    private EmployeeController controller;

    @BeforeEach
    void setUp() {
        controller = new EmployeeController(
                clientService,
                employeeService,
                orderService,
                passwordEncoder,
                passwordRequirementsValidator
        );
    }

    @Test
    void clientsAddsPageToModel() {
        Pageable pageable = PageRequest.of(0, 8, Sort.by("name"));
        Page<ClientDTO> page = new PageImpl<>(List.of(client("client@example.com", "Client")));
        Model model = new ExtendedModelMap();

        when(clientService.getAllClients(pageable)).thenReturn(page);

        String view = controller.clients(pageable, model);

        assertEquals("employee-clients", view);
        assertSame(page, model.getAttribute("clients"));
    }

    @Test
    void blockAndUnblockClientRedirectToClients() {
        assertEquals("redirect:/employee/clients", controller.blockClient("client@example.com"));
        assertEquals("redirect:/employee/clients", controller.unblockClient("client@example.com"));
        verify(clientService).blockClientByEmail("client@example.com");
        verify(clientService).unblockClientByEmail("client@example.com");
    }

    @Test
    void ordersAddsSearchAndPageToModel() {
        Pageable pageable = PageRequest.of(0, 8, Sort.Direction.DESC, "orderDate");
        Page<OrderDTO> page = new PageImpl<>(List.of());
        Model model = new ExtendedModelMap();

        when(orderService.searchOrdersByClient(null, pageable)).thenReturn(page);

        String view = controller.orders(null, pageable, model);

        assertEquals("employee-orders", view);
        assertSame(page, model.getAttribute("orders"));
        assertEquals("", model.getAttribute("search"));
    }

    @Test
    void registerEmployeePageAddsForm() {
        Model model = new ExtendedModelMap();

        String view = controller.registerEmployeePage(model);

        assertEquals("employee-register", view);
        assertTrue(model.containsAttribute("employeeRegistrationRequest"));
    }

    @Test
    void registerEmployeeReturnsRedirectLikeSuccessState() {
        EmployeeRegistrationRequest request = request("emp@example.com", "Bob", "Password1");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "employeeRegistrationRequest");
        Model model = new ExtendedModelMap();

        when(passwordRequirementsValidator.validate("Password1")).thenReturn(List.of());
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");

        String view = controller.registerEmployee(request, bindingResult, model);

        assertEquals("employee-register", view);
        assertTrue((Boolean) model.getAttribute("employeeRegistered"));
        verify(passwordEncoder).encode("Password1");
        verify(employeeService).addEmployee(any(EmployeeDTO.class));
    }

    @Test
    void registerEmployeeReturnsFormWhenPasswordsMismatch() {
        EmployeeRegistrationRequest request = request("emp@example.com", "Bob", "Password1");
        request.setConfirmPassword("Password2");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "employeeRegistrationRequest");
        Model model = new ExtendedModelMap();

        when(passwordRequirementsValidator.validate("Password1")).thenReturn(List.of());

        String view = controller.registerEmployee(request, bindingResult, model);

        assertEquals("employee-register", view);
        assertTrue(bindingResult.hasErrors());
        verifyNoInteractions(employeeService);
    }

    @Test
    void registerEmployeeReturnsFormWhenEmployeeAlreadyExists() {
        EmployeeRegistrationRequest request = request("emp@example.com", "Bob", "Password1");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "employeeRegistrationRequest");
        Model model = new ExtendedModelMap();

        when(passwordRequirementsValidator.validate("Password1")).thenReturn(List.of());
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(employeeService.addEmployee(any(EmployeeDTO.class))).thenThrow(new AlreadyExistException("exists"));

        String view = controller.registerEmployee(request, bindingResult, model);

        assertEquals("employee-register", view);
        assertTrue(bindingResult.hasErrors());
    }

    @Test
    void confirmAndCancelOrderRedirectToOrders() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("employee@example.com", "pass");
        LocalDateTime orderDate = LocalDateTime.of(2026, 6, 25, 10, 0);

        assertEquals("redirect:/employee/orders", controller.confirmOrder("client@example.com", orderDate, authentication));
        assertEquals("redirect:/employee/orders", controller.cancelOrder("client@example.com", orderDate, authentication));
        verify(orderService).confirmOrder("client@example.com", orderDate, "employee@example.com");
        verify(orderService).cancelOrder("client@example.com", orderDate, "employee@example.com");
    }

    private static EmployeeRegistrationRequest request(String email, String name, String password) {
        EmployeeRegistrationRequest request = new EmployeeRegistrationRequest();
        request.setEmail(email);
        request.setName(name);
        request.setPassword(password);
        request.setConfirmPassword(password);
        request.setPhone("+380000000000");
        request.setBirthDate(LocalDate.of(1990, 1, 1));
        return request;
    }

    private static ClientDTO client(String email, String name) {
        return new ClientDTO(email, "pass", name, BigDecimal.ZERO, false);
    }
}

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/employee")
public class EmployeeController {

    private static final int PAGE_SIZE = 8;

    private final ClientService clientService;
    private final EmployeeService employeeService;
    private final OrderService orderService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordRequirementsValidator passwordRequirementsValidator;

    @GetMapping("/clients")
    public String clients(@PageableDefault(size = PAGE_SIZE, sort = "name") Pageable pageable,
                          Model model) {
        Page<ClientDTO> clients = clientService.getAllClients(pageable);
        model.addAttribute("clients", clients);
        return "employee-clients";
    }

    @PostMapping("/clients/{email}/block")
    public String blockClient(@PathVariable String email) {
        clientService.blockClientByEmail(email);
        return "redirect:/employee/clients";
    }

    @PostMapping("/clients/{email}/unblock")
    public String unblockClient(@PathVariable String email) {
        clientService.unblockClientByEmail(email);
        return "redirect:/employee/clients";
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String search,
                         @PageableDefault(size = PAGE_SIZE, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable,
                         Model model) {
        Page<OrderDTO> orders = orderService.searchOrdersByClient(search, pageable);
        model.addAttribute("orders", orders);
        model.addAttribute("search", search == null ? "" : search);
        return "employee-orders";
    }

    @GetMapping("/register")
    public String registerEmployeePage(Model model) {
        if (!model.containsAttribute("employeeRegistrationRequest")) {
            model.addAttribute("employeeRegistrationRequest", new EmployeeRegistrationRequest());
        }
        return "employee-register";
    }

    @PostMapping("/register")
    public String registerEmployee(@Valid @ModelAttribute("employeeRegistrationRequest") EmployeeRegistrationRequest registrationRequest,
                                   BindingResult bindingResult,
                                   Model model) {
        passwordRequirementsValidator.validate(registrationRequest.getPassword())
                .forEach(message -> bindingResult.rejectValue("password", message));

        if (!registrationRequest.getPassword().equals(registrationRequest.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch");
        }
        if (bindingResult.hasErrors()) {
            return "employee-register";
        }

        try {
            EmployeeDTO employee = new EmployeeDTO(
                    registrationRequest.getEmail(),
                    passwordEncoder.encode(registrationRequest.getPassword()),
                    registrationRequest.getName(),
                    registrationRequest.getPhone(),
                    registrationRequest.getBirthDate()
            );
            employeeService.addEmployee(employee);
        } catch (AlreadyExistException ex) {
            bindingResult.rejectValue("email", "email.exists");
            return "employee-register";
        }

        model.addAttribute("employeeRegistered", true);
        model.addAttribute("employeeRegistrationRequest", new EmployeeRegistrationRequest());
        return "employee-register";
    }

    @PostMapping("/orders/confirm")
    public String confirmOrder(@RequestParam String clientEmail,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime orderDate,
                               Authentication authentication) {
        orderService.confirmOrder(clientEmail, orderDate, authentication.getName());
        return "redirect:/employee/orders";
    }

    @PostMapping("/orders/cancel")
    public String cancelOrder(@RequestParam String clientEmail,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime orderDate,
                              Authentication authentication) {
        orderService.cancelOrder(clientEmail, orderDate, authentication.getName());
        return "redirect:/employee/orders";
    }

    @PostMapping("/orders/delete")
    public String deleteOrder(@RequestParam String clientEmail,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime orderDate,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        orderService.deleteOrder(clientEmail, orderDate, authentication.getName());
        redirectAttributes.addFlashAttribute("orderDeleted", true);
        return "redirect:/employee/orders";
    }
}

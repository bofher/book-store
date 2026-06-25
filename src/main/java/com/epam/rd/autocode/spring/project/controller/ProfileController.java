package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.dto.ClientProfileForm;
import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.dto.EmployeeProfileForm;
import com.epam.rd.autocode.spring.project.security.JwtAuthenticationFilter;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final ClientService clientService;
    private final EmployeeService employeeService;

    @GetMapping
    public String profile(Authentication authentication, Model model) {
        boolean isEmployee = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_EMPLOYEE".equals(authority.getAuthority()));

        if (isEmployee) {
            EmployeeDTO employee = employeeService.getEmployeeByEmail(authentication.getName());
            model.addAttribute("employeeProfile", new EmployeeProfileForm(
                    employee.getName(),
                    employee.getPhone(),
                    employee.getBirthDate()
            ));
            model.addAttribute("employee", employee);
        } else {
            ClientDTO client = clientService.getClientByEmail(authentication.getName());
            model.addAttribute("clientProfile", new ClientProfileForm(client.getName(), BigDecimal.ZERO));
            model.addAttribute("client", client);
        }
        return "profile";
    }

    @PostMapping("/client")
    public String updateClient(@Valid @ModelAttribute("clientProfile") ClientProfileForm profileForm,
                               BindingResult bindingResult,
                               Authentication authentication,
                               Model model) {
        ClientDTO current = clientService.getClientByEmail(authentication.getName());

        if (bindingResult.hasErrors()) {
            model.addAttribute("clientProfile", profileForm);
            model.addAttribute("client", current);
            return "profile";
        }
        clientService.updateClientByEmail(authentication.getName(), new ClientDTO(
                current.getEmail(),
                current.getPassword(),
                profileForm.getName(),
                current.getBalance().add(profileForm.getTopUpAmount()),
                current.isBlocked()
        ));
        return "redirect:/profile";
    }

    @PostMapping("/employee")
    public String updateEmployee(@Valid @ModelAttribute("employeeProfile") EmployeeProfileForm profileForm,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 Model model) {
        EmployeeDTO current = employeeService.getEmployeeByEmail(authentication.getName());

        if (bindingResult.hasErrors()) {
            model.addAttribute("employeeProfile", profileForm);
            model.addAttribute("employee", current);
            return "profile";
        }
        employeeService.updateEmployeeByEmail(authentication.getName(), new EmployeeDTO(
                current.getEmail(),
                current.getPassword(),
                profileForm.getName(),
                profileForm.getPhone(),
                profileForm.getBirthDate()
        ));
        return "redirect:/profile";
    }

    @PostMapping("/delete")
    public String deleteOwnAccount(Authentication authentication, HttpServletResponse response) {
        clientService.deleteClientByEmail(authentication.getName());
        clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/auth/login";
    }

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JwtAuthenticationFilter.COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}

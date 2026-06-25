package com.epam.rd.autocode.spring.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EmployeeRegistrationRequest {

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    private String email;

    @NotBlank(message = "{validation.required}")
    private String name;

    @NotBlank(message = "{password.required}")
    @Size(min = 8, message = "{password.min}")
    private String password;

    @NotBlank(message = "{password.required}")
    @Size(min = 8, message = "{password.min}")
    private String confirmPassword;

    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "^[+\\d()\\-\\s]{7,20}$", message = "{validation.phone}")
    private String phone;

    @NotNull(message = "{validation.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Past(message = "{validation.past}")
    private LocalDate birthDate;
}

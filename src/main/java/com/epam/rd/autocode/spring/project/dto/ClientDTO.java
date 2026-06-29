package com.epam.rd.autocode.spring.project.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClientDTO{

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    private String email;

    @NotBlank(message = "{validation.required}")
    private String password;

    @NotBlank(message = "{validation.required}")
    private String name;

    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0.00", message = "{validation.non_negative}")
    private BigDecimal balance;

    private boolean blocked;
}

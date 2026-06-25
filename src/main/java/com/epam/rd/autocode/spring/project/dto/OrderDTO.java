package com.epam.rd.autocode.spring.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderDTO{

    @NotBlank(message = "{validation.required}")
    private String clientEmail;

    private String employeeEmail;

    @NotNull(message = "{validation.required}")
    private LocalDateTime orderDate;

    @NotNull(message = "{validation.required}")
    @Positive(message = "{validation.positive}")
    private BigDecimal price;

    @NotBlank(message = "{validation.required}")
    private String status;

    @Valid
    @NotEmpty(message = "{validation.required}")
    private List<BookItemDTO> bookItems;
}

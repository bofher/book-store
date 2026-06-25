package com.epam.rd.autocode.spring.project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BookItemDTO {

    @NotBlank(message = "{validation.required}")
    private String bookName;

    @NotNull(message = "{validation.required}")
    @Min(value = 1, message = "{validation.positive}")
    private Integer quantity;

}

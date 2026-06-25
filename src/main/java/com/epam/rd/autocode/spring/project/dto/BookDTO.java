package com.epam.rd.autocode.spring.project.dto;

import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BookDTO{

    @NotBlank(message = "{validation.required}")
    private String name;

    @NotBlank(message = "{validation.required}")
    private String genre;

    @NotNull(message = "{validation.required}")
    private AgeGroup ageGroup;

    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0.01", message = "{validation.positive}")
    private BigDecimal price;

    @NotNull(message = "{validation.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "{validation.past_or_present}")
    private LocalDate publicationDate;

    @NotBlank(message = "{validation.required}")
    private String author;

    @NotNull(message = "{validation.required}")
    @Min(value = 1, message = "{validation.positive}")
    private Integer pages;

    @NotBlank(message = "{validation.required}")
    private String characteristics;

    @NotBlank(message = "{validation.required}")
    private String description;

    @NotNull(message = "{validation.required}")
    private Language language;
}

package com.epam.rd.autocode.spring.project.dto;

import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class BookDTO{

    private String name;

    private String genre;

    private AgeGroup ageGroup;

    private BigDecimal price;

    private LocalDate publicationDate;

    private String author;

    private Integer pages;

    private String characteristics;

    private String description;

    private Language language;
}

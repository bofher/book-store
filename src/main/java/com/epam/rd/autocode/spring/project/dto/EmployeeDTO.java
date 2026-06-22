package com.epam.rd.autocode.spring.project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class EmployeeDTO{

    private String email;

    private String password;

    private String name;

    private String phone;

    private LocalDate birthDate;
}

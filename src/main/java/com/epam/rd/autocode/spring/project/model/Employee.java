package com.epam.rd.autocode.spring.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "employees")
public class Employee extends User {

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private LocalDate birthDate;

    public Employee(Long id, String email, String password, String name,
                    String phone, LocalDate birthDate) {
        super(id, email, password, name, false);
        this.phone = phone;
        this.birthDate = birthDate;
    }
}

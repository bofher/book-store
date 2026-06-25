package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public EmployeeDTO getEmployeeByEmail(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Employee not found by email: " + email));

        return modelMapper.map(employee, EmployeeDTO.class);
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public EmployeeDTO updateEmployeeByEmail(String email, EmployeeDTO employee) {
        Employee existing = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Employee not found by email: " + email));

        String employeeEmail = currentEmployeeEmail();
        log.debug("Employee {} is updating employee profile {}", employeeEmail, email);
        modelMapper.map(employee, existing);

        Employee updated = employeeRepository.save(existing);
        EmployeeDTO result = modelMapper.map(updated, EmployeeDTO.class);
        log.debug("Employee {} updated employee profile {} successfully", employeeEmail, email);
        return result;
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public void deleteEmployeeByEmail(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Employee not found by email: " + email));

        String employeeEmail = currentEmployeeEmail();
        log.debug("Employee {} is deleting employee account {}", employeeEmail, email);
        employeeRepository.delete(employee);
        log.debug("Employee {} deleted employee account {} successfully", employeeEmail, email);
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public EmployeeDTO addEmployee(EmployeeDTO employee) {
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new AlreadyExistException("Employee with email '" + employee.getEmail() + "' already exists");
        }
        String employeeEmail = currentEmployeeEmail();
        log.debug("Employee {} is adding employee {}", employeeEmail, employee.getEmail());
        Employee saved = employeeRepository.save(modelMapper.map(employee, Employee.class));
        EmployeeDTO result = modelMapper.map(saved, EmployeeDTO.class);
        log.debug("Employee {} added employee {} successfully", employeeEmail, result.getEmail());
        return result;
    }

    private String currentEmployeeEmail() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "unknown";
    }
}

package com.epam.rd.autocode.spring.project.service;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository, new ModelMapper());
    }

    @Test
    void getEmployeeByEmailReturnsDto() {
        when(employeeRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee("employee@example.com")));

        EmployeeDTO result = employeeService.getEmployeeByEmail("employee@example.com");

        assertEquals("employee@example.com", result.getEmail());
    }

    @Test
    void getEmployeeByEmailThrowsWhenMissing() {
        when(employeeRepository.findByEmail("employee@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> employeeService.getEmployeeByEmail("employee@example.com"));
    }

    @Test
    void addEmployeeSavesWhenEmailIsFree() {
        EmployeeDTO input = employeeDto("employee@example.com");
        when(employeeRepository.existsByEmail("employee@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeDTO result = employeeService.addEmployee(input);

        assertEquals("employee@example.com", result.getEmail());
    }

    @Test
    void addEmployeeThrowsWhenEmailExists() {
        when(employeeRepository.existsByEmail("employee@example.com")).thenReturn(true);

        assertThrows(AlreadyExistException.class, () -> employeeService.addEmployee(employeeDto("employee@example.com")));
    }

    @Test
    void updateEmployeeByEmailUpdatesExistingEmployee() {
        Employee existing = employee("employee@example.com");
        EmployeeDTO update = new EmployeeDTO(
                "employee@example.com",
                "pass",
                "Updated",
                "+380000000001",
                LocalDate.of(1991, 2, 2)
        );
        when(employeeRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(existing));
        when(employeeRepository.save(existing)).thenReturn(existing);

        EmployeeDTO result = employeeService.updateEmployeeByEmail("employee@example.com", update);

        assertEquals("Updated", result.getName());
        assertEquals("Updated", existing.getName());
    }

    @Test
    void deleteEmployeeByEmailDeletesExistingEmployee() {
        Employee existing = employee("employee@example.com");
        when(employeeRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(existing));

        employeeService.deleteEmployeeByEmail("employee@example.com");

        verify(employeeRepository).delete(existing);
    }

    private static Employee employee(String email) {
        return new Employee(1L, email, "pass", "Employee", "+380000000000", LocalDate.of(1990, 1, 1));
    }

    private static EmployeeDTO employeeDto(String email) {
        return new EmployeeDTO(email, "pass", "Employee", "+380000000000", LocalDate.of(1990, 1, 1));
    }
}

package com.epam.rd.autocode.spring.project.security;

import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthenticatedUserService implements UserDetailsService {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;

    public AuthenticatedUserService(ClientRepository clientRepository, EmployeeRepository employeeRepository) {
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Client client = clientRepository.findByEmail(email).orElse(null);

        if (client != null) {
            return toUserDetails(client.getEmail(), client.getPassword(), "ROLE_CLIENT", client.isBlocked());
        }
        Employee employee = employeeRepository.findByEmail(email).orElse(null);

        if (employee != null) {
            return toUserDetails(employee.getEmail(), employee.getPassword(), "ROLE_EMPLOYEE", employee.isBlocked());
        }
        throw new UsernameNotFoundException("User not found: " + email);
    }

    private UserDetails toUserDetails(String email, String password, String role, boolean blocked) {
        return new User(email, password, true, true, true, !blocked, List.of(new SimpleGrantedAuthority(role)));
    }
}

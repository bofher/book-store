package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ModelMapper modelMapper;

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<ClientDTO> getAllClients() {
        return clientRepository.findAll()
                .stream()
                .map(client -> modelMapper.map(client, ClientDTO.class))
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public Page<ClientDTO> getAllClients(Pageable pageable) {
        return clientRepository.findAll(pageable)
                .map(client -> modelMapper.map(client, ClientDTO.class));
    }

    @Override
    @PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE')")
    public ClientDTO getClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Client not found by email: " + email));

        return modelMapper.map(client, ClientDTO.class);
    }

    @Override
    @PreAuthorize("hasRole('CLIENT')")
    public ClientDTO updateClientByEmail(String email, ClientDTO client) {
        Client existing = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Client not found by email: " + email));

        String clientEmail = currentUserEmail();
        log.debug("Client {} is updating profile", clientEmail);
        modelMapper.map(client, existing);

        Client updated = clientRepository.save(existing);
        ClientDTO result = modelMapper.map(updated, ClientDTO.class);
        log.debug("Client {} updated profile successfully", clientEmail);
        return result;
    }

    @Override
    @PreAuthorize("hasRole('CLIENT')")
    public void deleteClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Client not found by email: " + email));

        String clientEmail = currentUserEmail();
        log.debug("Client {} is deleting account {}", clientEmail, email);
        clientRepository.delete(client);
        log.debug("Client {} deleted account {} successfully", clientEmail, email);
    }

    @Override
    public ClientDTO addClient(ClientDTO client) {
        if (clientRepository.existsByEmail(client.getEmail())) {
            throw new AlreadyExistException("Client with email '" + client.getEmail() + "' already exists");
        }
        Client saved = clientRepository.save(modelMapper.map(client, Client.class));
        return modelMapper.map(saved, ClientDTO.class);
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ClientDTO blockClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Client not found by email: " + email));
        String employeeEmail = currentEmployeeEmail();
        log.debug("Employee {} is blocking client {}", employeeEmail, email);
        client.setBlocked(true);
        ClientDTO result = modelMapper.map(clientRepository.save(client), ClientDTO.class);
        log.debug("Employee {} blocked client {} successfully", employeeEmail, email);
        return result;
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ClientDTO unblockClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Client not found by email: " + email));
        String employeeEmail = currentEmployeeEmail();
        log.debug("Employee {} is unblocking client {}", employeeEmail, email);
        client.setBlocked(false);
        ClientDTO result = modelMapper.map(clientRepository.save(client), ClientDTO.class);
        log.debug("Employee {} unblocked client {} successfully", employeeEmail, email);
        return result;
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "unknown";
    }

    private String currentEmployeeEmail() {
        return currentUserEmail();
    }
}

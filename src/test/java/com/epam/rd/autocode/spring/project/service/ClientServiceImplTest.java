package com.epam.rd.autocode.spring.project.service;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.service.impl.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    private ClientServiceImpl clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientServiceImpl(clientRepository, new ModelMapper());
    }

    @Test
    void getAllClientsReturnsMappedList() {
        when(clientRepository.findAll()).thenReturn(List.of(client("client@example.com", false)));

        List<ClientDTO> result = clientService.getAllClients();

        assertEquals(1, result.size());
        assertEquals("client@example.com", result.get(0).getEmail());
    }

    @Test
    void getAllClientsPageReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(clientRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(client("client@example.com", false))));

        Page<ClientDTO> result = clientService.getAllClients(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getClientByEmailReturnsDto() {
        when(clientRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client("client@example.com", false)));

        ClientDTO result = clientService.getClientByEmail("client@example.com");

        assertEquals("client@example.com", result.getEmail());
    }

    @Test
    void getClientByEmailThrowsWhenMissing() {
        when(clientRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> clientService.getClientByEmail("missing@example.com"));
    }

    @Test
    void addClientSavesWhenEmailIsFree() {
        ClientDTO input = clientDto("client@example.com", false);
        when(clientRepository.existsByEmail("client@example.com")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientDTO result = clientService.addClient(input);

        assertEquals("client@example.com", result.getEmail());
    }

    @Test
    void addClientThrowsWhenEmailExists() {
        when(clientRepository.existsByEmail("client@example.com")).thenReturn(true);

        assertThrows(AlreadyExistException.class, () -> clientService.addClient(clientDto("client@example.com", false)));
    }

    @Test
    void updateClientByEmailUpdatesExistingClient() {
        Client existing = client("client@example.com", false);
        ClientDTO update = clientDto("client@example.com", true);
        when(clientRepository.findByEmail("client@example.com")).thenReturn(Optional.of(existing));
        when(clientRepository.save(existing)).thenReturn(existing);

        ClientDTO result = clientService.updateClientByEmail("client@example.com", update);

        assertTrue(result.isBlocked());
        assertTrue(existing.isBlocked());
    }

    @Test
    void deleteClientByEmailDeletesExistingClient() {
        Client existing = client("client@example.com", false);
        when(clientRepository.findByEmail("client@example.com")).thenReturn(Optional.of(existing));

        clientService.deleteClientByEmail("client@example.com");

        verify(clientRepository).delete(existing);
    }

    @Test
    void blockClientByEmailSetsBlockedFlag() {
        Client existing = client("client@example.com", false);
        when(clientRepository.findByEmail("client@example.com")).thenReturn(Optional.of(existing));
        when(clientRepository.save(existing)).thenReturn(existing);

        ClientDTO result = clientService.blockClientByEmail("client@example.com");

        assertTrue(result.isBlocked());
    }

    @Test
    void unblockClientByEmailClearsBlockedFlag() {
        Client existing = client("client@example.com", true);
        when(clientRepository.findByEmail("client@example.com")).thenReturn(Optional.of(existing));
        when(clientRepository.save(existing)).thenReturn(existing);

        ClientDTO result = clientService.unblockClientByEmail("client@example.com");

        assertFalse(result.isBlocked());
    }

    private static Client client(String email, boolean blocked) {
        Client client = new Client(1L, email, "pass", "Client", new BigDecimal("100.00"));
        client.setBlocked(blocked);
        return client;
    }

    private static ClientDTO clientDto(String email, boolean blocked) {
        return new ClientDTO(email, "pass", "Client", new BigDecimal("100.00"), blocked);
    }
}

package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.BookInUseException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.repo.BookItemRepository;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookItemRepository bookItemRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(book -> modelMapper.map(book, BookDTO.class))
                .toList();
    }

    @Override
    public Page<BookDTO> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable)
                .map(book -> modelMapper.map(book, BookDTO.class));
    }

    @Override
    public Page<BookDTO> searchBooks(String search, Pageable pageable) {
        if (!StringUtils.hasText(search)) {
            return getAllBooks(pageable);
        }

        return bookRepository.searchBooks(search.trim(), pageable)
                .map(book -> modelMapper.map(book, BookDTO.class));
    }

    @Override
    public BookDTO getBookByName(String name) {
        Book book = bookRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Book not found by name: " + name));

        return modelMapper.map(book, BookDTO.class);
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public BookDTO updateBookByName(String name, BookDTO book) {
        Book existing = bookRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Book not found by name: " + name));

        if (!existing.getName().equals(book.getName()) && bookRepository.existsByName(book.getName())) {
            throw new AlreadyExistException("Book with name '" + book.getName() + "' already exists");
        }

        String employeeEmail = currentEmployeeEmail();
        log.debug("Employee {} is updating book {}", employeeEmail, name);
        modelMapper.map(book, existing);
        BookDTO saved = modelMapper.map(bookRepository.save(existing), BookDTO.class);
        log.debug("Employee {} updated book {} successfully", employeeEmail, saved.getName());
        return saved;
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public void deleteBookByName(String name) {
        Book book = bookRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Book not found by name: " + name));

        if (bookItemRepository.existsByBook_Id(book.getId())) {
            throw new BookInUseException("Cannot delete book because it is used in existing orders");
        }

        String employeeEmail = currentEmployeeEmail();
        log.debug("Employee {} is deleting book {}", employeeEmail, name);
        bookRepository.delete(book);
        log.debug("Employee {} deleted book {} successfully", employeeEmail, name);
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public BookDTO addBook(BookDTO book) {
        if (bookRepository.existsByName(book.getName())) {
            throw new AlreadyExistException("Book with name '" + book.getName() + "' already exists");
        }
        String employeeEmail = currentEmployeeEmail();
        log.debug("Employee {} is adding book {}", employeeEmail, book.getName());
        Book saved = bookRepository.save(modelMapper.map(book, Book.class));
        BookDTO result = modelMapper.map(saved, BookDTO.class);
        log.debug("Employee {} added book {} successfully", employeeEmail, result.getName());
        return result;
    }

    private String currentEmployeeEmail() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "unknown";
    }
}

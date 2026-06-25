package com.epam.rd.autocode.spring.project.service;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.BookInUseException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import com.epam.rd.autocode.spring.project.repo.BookItemRepository;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.service.impl.BookServiceImpl;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookItemRepository bookItemRepository;

    private BookServiceImpl bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookServiceImpl(bookRepository, bookItemRepository, new ModelMapper());
    }

    @Test
    void getAllBooksReturnsMappedList() {
        when(bookRepository.findAll()).thenReturn(List.of(book("Clean Code")));

        List<BookDTO> result = bookService.getAllBooks();

        assertEquals(1, result.size());
        assertEquals("Clean Code", result.get(0).getName());
    }

    @Test
    void getAllBooksPageReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(bookRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(book("Dune"))));

        Page<BookDTO> result = bookService.getAllBooks(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Dune", result.getContent().get(0).getName());
    }

    @Test
    void searchBooksWithoutTextFallsBackToFindAll() {
        Pageable pageable = PageRequest.of(0, 5);
        when(bookRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(book("1984"))));

        Page<BookDTO> result = bookService.searchBooks("   ", pageable);

        assertEquals(1, result.getContent().size());
        verify(bookRepository).findAll(pageable);
    }

    @Test
    void searchBooksWithTextUsesRepositorySearch() {
        Pageable pageable = PageRequest.of(0, 5);
        when(bookRepository.searchBooks("martin", pageable)).thenReturn(new PageImpl<>(List.of(book("Martin Eden"))));

        Page<BookDTO> result = bookService.searchBooks(" martin ", pageable);

        assertEquals("Martin Eden", result.getContent().get(0).getName());
    }

    @Test
    void getBookByNameReturnsMappedDto() {
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(book("Dune")));

        BookDTO result = bookService.getBookByName("Dune");

        assertEquals("Dune", result.getName());
    }

    @Test
    void getBookByNameThrowsWhenMissing() {
        when(bookRepository.findByName("Missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookService.getBookByName("Missing"));
    }

    @Test
    void addBookSavesBookWhenNameIsFree() {
        BookDTO input = bookDto("Dune");
        when(bookRepository.existsByName("Dune")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO result = bookService.addBook(input);

        assertEquals("Dune", result.getName());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void addBookThrowsWhenNameExists() {
        when(bookRepository.existsByName("Dune")).thenReturn(true);

        assertThrows(AlreadyExistException.class, () -> bookService.addBook(bookDto("Dune")));
    }

    @Test
    void updateBookByNameUpdatesExistingBook() {
        Book existing = book("Old Name");
        BookDTO update = bookDto("New Name");
        when(bookRepository.findByName("Old Name")).thenReturn(Optional.of(existing));
        when(bookRepository.existsByName("New Name")).thenReturn(false);
        when(bookRepository.save(existing)).thenReturn(existing);

        BookDTO result = bookService.updateBookByName("Old Name", update);

        assertEquals("New Name", result.getName());
        assertEquals("New Name", existing.getName());
    }

    @Test
    void updateBookByNameThrowsWhenNewNameAlreadyExists() {
        when(bookRepository.findByName("Old Name")).thenReturn(Optional.of(book("Old Name")));
        when(bookRepository.existsByName("New Name")).thenReturn(true);

        assertThrows(AlreadyExistException.class, () -> bookService.updateBookByName("Old Name", bookDto("New Name")));
    }

    @Test
    void deleteBookByNameDeletesExistingBook() {
        Book existing = book("Dune");
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(existing));
        when(bookItemRepository.existsByBook_Id(existing.getId())).thenReturn(false);

        bookService.deleteBookByName("Dune");

        verify(bookRepository).delete(existing);
    }

    @Test
    void deleteBookByNameThrowsWhenBookIsUsedInOrders() {
        Book existing = book("Dune");
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(existing));
        when(bookItemRepository.existsByBook_Id(existing.getId())).thenReturn(true);

        assertThrows(BookInUseException.class, () -> bookService.deleteBookByName("Dune"));
    }

    private static Book book(String name) {
        return new Book(
                1L,
                name,
                "Novel",
                AgeGroup.ADULT,
                new BigDecimal("20.00"),
                LocalDate.of(2020, 1, 1),
                "Author",
                300,
                "Hardcover",
                "Description",
                Language.ENGLISH
        );
    }

    private static BookDTO bookDto(String name) {
        return new BookDTO(
                name,
                "Novel",
                AgeGroup.ADULT,
                new BigDecimal("20.00"),
                LocalDate.of(2020, 1, 1),
                "Author",
                300,
                "Hardcover",
                "Description",
                Language.ENGLISH
        );
    }
}

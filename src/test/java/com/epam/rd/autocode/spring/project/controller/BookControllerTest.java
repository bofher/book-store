package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.BookInUseException;
import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import com.epam.rd.autocode.spring.project.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService bookService;

    @Mock
    private RedirectAttributes redirectAttributes;

    private BookController controller;

    @BeforeEach
    void setUp() {
        controller = new BookController(bookService);
    }

    @Test
    void booksAddsPageData() {
        Pageable pageable = PageRequest.of(0, 8, Sort.by("name"));
        Page<BookDTO> page = new PageImpl<>(List.of(book("Clean Code", new BigDecimal("12.50"))));
        Model model = new ExtendedModelMap();

        when(bookService.searchBooks(null, pageable)).thenReturn(page);

        String view = controller.books(null, pageable, model, Locale.ENGLISH);

        assertEquals("books", view);
        assertSame(page, model.getAttribute("books"));
        assertEquals("", model.getAttribute("search"));
        assertEquals(List.of(8, 16, 32, 64), model.getAttribute("pageSizes"));
        assertEquals("name,asc", model.getAttribute("currentSort"));
        assertEquals("/books?page=0&size=8&sort=name,asc&lang=en", model.getAttribute("currentBooksUrl"));
    }

    @Test
    void bookDetailsAddsBookToModel() {
        BookDTO book = book("Clean Code", new BigDecimal("12.50"));
        Model model = new ExtendedModelMap();

        when(bookService.getBookByName("Clean Code")).thenReturn(book);

        String view = controller.bookDetails("Clean Code", model);

        assertEquals("book-details", view);
        assertSame(book, model.getAttribute("book"));
    }

    @Test
    void addBookPageAddsEmptyForm() {
        Model model = new ExtendedModelMap();

        String view = controller.addBookPage(model);

        assertEquals("book-form", view);
        assertTrue(model.containsAttribute("bookForm"));
        assertEquals(false, model.getAttribute("editMode"));
        assertEquals(null, model.getAttribute("name"));
    }

    @Test
    void addBookReturnsRedirectOnSuccess() {
        BookDTO form = book("New Book", new BigDecimal("10.00"));
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "bookForm");
        BookDTO created = book("New Book", new BigDecimal("10.00"));
        Model model = new ExtendedModelMap();

        when(bookService.addBook(form)).thenReturn(created);

        String view = controller.addBook(form, bindingResult, model);

        assertEquals("redirect:/books/New Book", view);
        verify(bookService).addBook(form);
    }

    @Test
    void addBookReturnsFormWhenBindingHasErrors() {
        BookDTO form = new BookDTO();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "bookForm");
        bindingResult.rejectValue("name", "validation.required");
        Model model = new ExtendedModelMap();

        String view = controller.addBook(form, bindingResult, model);

        assertEquals("book-form", view);
        verifyNoInteractions(bookService);
    }

    @Test
    void editBookPageAddsFormInEditMode() {
        BookDTO book = book("Clean Code", new BigDecimal("12.50"));
        Model model = new ExtendedModelMap();

        when(bookService.getBookByName("Clean Code")).thenReturn(book);

        String view = controller.editBookPage("Clean Code", model);

        assertEquals("book-form", view);
        assertSame(book, model.getAttribute("bookForm"));
        assertEquals(true, model.getAttribute("editMode"));
        assertEquals("Clean Code", model.getAttribute("name"));
    }

    @Test
    void editBookReturnsErrorWhenNameAlreadyExists() {
        BookDTO form = book("Updated Book", new BigDecimal("13.00"));
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "bookForm");
        Model model = new ExtendedModelMap();

        when(bookService.updateBookByName(anyString(), any(BookDTO.class)))
                .thenThrow(new AlreadyExistException("exists"));

        String view = controller.editBook("Old Name", form, bindingResult, model);

        assertEquals("book-form", view);
        assertTrue(bindingResult.hasErrors());
    }

    @Test
    void deleteBookRedirectsToBooks() {
        String view = controller.deleteBook("Clean Code", redirectAttributes);

        assertEquals("redirect:/books", view);
        verify(bookService).deleteBookByName("Clean Code");
    }

    @Test
    void deleteBookRedirectsBackToBookWhenBookIsUsed() {
        doThrow(new BookInUseException("used in orders"))
                .when(bookService).deleteBookByName("Clean Code");

        String view = controller.deleteBook("Clean Code", redirectAttributes);

        assertEquals("redirect:/books/Clean%20Code", view);
        verify(redirectAttributes).addFlashAttribute("bookDeleteError", true);
    }

    private static BookDTO book(String name, BigDecimal price) {
        return new BookDTO(
                name,
                "Programming",
                AgeGroup.ADULT,
                price,
                LocalDate.of(2020, 1, 1),
                "Author",
                100,
                "Hardcover",
                "Description",
                Language.ENGLISH
        );
    }
}

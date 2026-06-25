package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.BookInUseException;
import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import com.epam.rd.autocode.spring.project.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class BookController {

    private static final int PAGE_SIZE = 8;
    private static final List<Integer> PAGE_SIZES = List.of(8, 16, 32, 64);

    private final BookService bookService;

    @GetMapping({"/", "/books"})
    public String books(@RequestParam(required = false) String search,
                        @PageableDefault(size = PAGE_SIZE, sort = "name") Pageable pageable,
                        Model model,
                        Locale locale) {
        return renderBooksPage(search, pageable, model, locale);
    }

    private String renderBooksPage(String search,
                                   Pageable pageable,
                                   Model model,
                                   Locale locale) {
        Page<BookDTO> books = bookService.searchBooks(search, pageable);

        model.addAttribute("books", books);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("pageSizes", PAGE_SIZES);
        model.addAttribute("currentSort", toSortParam(pageable));
        model.addAttribute("currentBooksUrl", buildBooksUrl(search, pageable, locale));
        return "books";
    }

    @GetMapping("/books/{name}")
    public String bookDetails(@PathVariable String name,
                              Model model) {
        model.addAttribute("book", bookService.getBookByName(name));
        return "book-details";
    }

    @GetMapping("/books/add")
    public String addBookPage(Model model) {
        addFormModel(model, new BookDTO(), false, null);
        return "book-form";
    }

    @PostMapping("/books/add")
    public String addBook(@Valid @ModelAttribute("bookForm") BookDTO bookForm,
                          BindingResult bindingResult,
                          Model model) {
        if (bindingResult.hasErrors()) {
            addFormModel(model, bookForm, false, null);
            return "book-form";
        }
        BookDTO created = bookService.addBook(bookForm);
        return "redirect:/books/" + created.getName();
    }

    @GetMapping("/books/{name}/edit")
    public String editBookPage(@PathVariable String name,
                               Model model) {
        addFormModel(model, bookService.getBookByName(name), true, name);
        return "book-form";
    }

    @PostMapping("/books/{name}/edit")
    public String editBook(@PathVariable String name,
                           @Valid @ModelAttribute("bookForm") BookDTO bookForm,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            addFormModel(model, bookForm, true, name);
            return "book-form";
        }
        try {
            bookService.updateBookByName(name, bookForm);
            return "redirect:/books/" + bookForm.getName();
        } catch (AlreadyExistException ex) {
            bindingResult.reject("books.name.exists");
            addFormModel(model, bookForm, true, name);
            return "book-form";
        }
    }

    @PostMapping("/books/{name}/delete")
    public String deleteBook(@PathVariable String name,
                             RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBookByName(name);
            return "redirect:/books";
        } catch (BookInUseException ex) {
            redirectAttributes.addFlashAttribute("bookDeleteError", true);
            return "redirect:" + UriComponentsBuilder.fromPath("/books/{name}")
                    .buildAndExpand(name)
                    .encode()
                    .toUriString();
        }
    }

    private void addFormModel(Model model,
                              BookDTO bookForm,
                              boolean editMode,
                              String name) {
        model.addAttribute("bookForm", bookForm);
        model.addAttribute("editMode", editMode);
        model.addAttribute("name", name);
        model.addAttribute("ageGroups", AgeGroup.values());
        model.addAttribute("languages", Language.values());
    }

    private String toSortParam(Pageable pageable) {
        Optional<Sort.Order> order = pageable.getSort().stream().findFirst();
        if (order.isEmpty()) {
            return "name,asc";
        }
        Sort.Order first = order.get();
        return first.getProperty() + "," + first.getDirection().name().toLowerCase();
    }

    private String buildBooksUrl(String search, Pageable pageable, Locale locale) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/books")
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize())
                .queryParam("sort", toSortParam(pageable))
                .queryParam("lang", locale.getLanguage());

        if (search != null && !search.isBlank()) {
            builder.queryParam("search", search);
        }
        return builder.build().toUriString();
    }
}

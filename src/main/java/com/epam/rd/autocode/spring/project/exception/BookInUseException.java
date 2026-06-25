package com.epam.rd.autocode.spring.project.exception;

public class BookInUseException extends RuntimeException {
    public BookInUseException(String message) {
        super(message);
    }
}

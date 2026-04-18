package com.example.exceptions;

public class BooksException extends RuntimeException {
    public BooksException(String message) { super(message); }
    public BooksException(String message, Throwable cause) { super(message, cause); }
}
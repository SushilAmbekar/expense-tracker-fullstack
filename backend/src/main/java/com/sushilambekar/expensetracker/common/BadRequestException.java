package com.sushilambekar.expensetracker.common;

/** The request is well-formed but refers to something invalid. Answered with 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}

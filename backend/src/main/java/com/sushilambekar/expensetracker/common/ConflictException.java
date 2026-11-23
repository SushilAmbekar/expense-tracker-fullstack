package com.sushilambekar.expensetracker.common;

/** The request conflicts with existing data, such as a duplicate name. Answered with 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

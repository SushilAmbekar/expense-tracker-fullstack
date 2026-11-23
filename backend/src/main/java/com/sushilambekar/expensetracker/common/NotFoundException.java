package com.sushilambekar.expensetracker.common;

/** A resource does not exist, or belongs to another user. Answered with 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String resource, Long id) {
        return new NotFoundException(resource + " " + id + " not found");
    }
}

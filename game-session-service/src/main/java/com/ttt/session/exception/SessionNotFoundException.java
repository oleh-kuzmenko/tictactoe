package com.ttt.session.exception;

public class SessionNotFoundException extends RuntimeException {
    
    public SessionNotFoundException(String id) {
        super("Session not found: " + id);
    }
}

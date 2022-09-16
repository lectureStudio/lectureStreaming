package org.lecturestudio.web.portal.exception;

public class MessageInterceptedException extends Exception {

    private String sessionId;


    public MessageInterceptedException(String message) {
        super(message);
    }

    public MessageInterceptedException(String message, String sessionId) {
        super(message);
        this.sessionId = sessionId;
    }

    public MessageInterceptedException(String message, String sessionId, Throwable cause) {
        super(message, cause);
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return this.sessionId;
    }

}
package org.mifos.connector.tnm.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a required field is missing in the request.
 */
public class MissingFieldException extends TNMConnectorException {

    public MissingFieldException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public MissingFieldException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_REQUEST);
    }

}

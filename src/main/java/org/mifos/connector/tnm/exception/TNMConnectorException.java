package org.mifos.connector.tnm.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Parent class for TNM Connector exceptions
 *
 * @author amy.muhimpundu
 */
@Getter
public class TNMConnectorException extends RuntimeException {

    private final HttpStatus httpStatus;

    public TNMConnectorException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public TNMConnectorException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

}

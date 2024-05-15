package org.mifos.connector.tnm.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Error response class
 */
@Getter
@Setter
@NoArgsConstructor
public class PayBillErrorResponse implements Serializable {

    private String code;
    private String message;

}

package org.mifos.connector.tnm.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TnmPayBillPayRequestDtoTest {

    @DisplayName("Returns string representation with all populated fields in correct format")
    @Test
    void test_populated_fields_string_representation() {
        TnmPayBillPayRequestDto request = new TnmPayBillPayRequestDto();
        request.setTransactionType("Pay Bill");
        request.setTransactionId("RKTQDM7W6S");
        request.setTransactionAmount("10");
        request.setShortCode("600638");
        request.setBillRefNo("A123");
        request.setInvoiceNumber("INV001");
        request.setAccountBalance("49197.00");
        request.setThirdPartyTransactionId("TPT123");
        request.setMsisdn("254712345149");
        request.setFirstname("John");

        String expected = "PayBillRequestDTO{transactionType='Pay Bill', transactionID='RKTQDM7W6S', transactionAmount='10', shortCode='600638', billRefNo='A123', invoiceNumber='INV001', accountBalance='49197.00', thirdPatrytransactionID='TPT123', msisdn='254712345149', firstname='John'}";
        assertEquals(expected, request.toString());
    }

    @DisplayName("Returns correct string when all fields are null")
    @Test
    void test_null_fields_string_representation() {
        TnmPayBillPayRequestDto request = new TnmPayBillPayRequestDto();

        String expected = "PayBillRequestDTO{transactionType='null', transactionID='null', transactionAmount='null', shortCode='null', billRefNo='null', invoiceNumber='null', accountBalance='null', thirdPatrytransactionID='null', msisdn='null', firstname='null'}";
        assertEquals(expected, request.toString());
    }

}

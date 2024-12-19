package org.mifos.connector.tnm.util;

import static org.mifos.connector.tnm.camel.config.CamelProperties.FINERACT_PRIMARY_IDENTIFIER_NAME;
import static org.mifos.connector.tnm.camel.config.CamelProperties.ROSTER_PRIMARY_IDENTIFIER_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.common.channel.dto.TransactionStatusResponseDTO;
import org.mifos.connector.common.mojaloop.type.TransferState;
import org.mifos.connector.tnm.dto.ChannelRequestDto;
import org.mifos.connector.tnm.dto.TnmPayBillPayRequestDto;

class TnmUtilsTest {

    @DisplayName("Successful transaction with COMMITTED transfer state returns 200 status and success message")
    @Test
    void test_successful_transaction_committed_state() {
        TransactionStatusResponseDTO responseDto = new TransactionStatusResponseDTO();
        responseDto.setTransferState(TransferState.COMMITTED);
        responseDto.setTransferId("transfer-123");
        responseDto.setTransactionId("txn-123");

        JSONObject response = TnmUtils.buildPayBillTransactionResponseResponse(true, responseDto);

        Assertions.assertEquals(200, response.getInt("status"));
        Assertions.assertEquals("Payment successful", response.getString("message"));
        Assertions.assertEquals("transfer-123", response.getString("receipt_number"));
        Assertions.assertEquals("txn-123", response.getString("trans_id"));
    }

    @DisplayName("Successful transaction with COMMITTED transfer state returns 200 status and success message")
    @Test
    void test_successful_transaction_received_state() {
        TransactionStatusResponseDTO responseDto = new TransactionStatusResponseDTO();
        responseDto.setTransferState(TransferState.RECEIVED);
        responseDto.setTransferId("transfer-123");
        responseDto.setTransactionId("txn-123");

        JSONObject response = TnmUtils.buildPayBillTransactionResponseResponse(true, responseDto);

        Assertions.assertEquals(404, response.getInt("status"));
        Assertions.assertEquals("Transaction not found", response.getString("message"));
    }

    @DisplayName("Handling null response parameter")
    @Test
    void test_null_response_parameter() {
        JSONObject response = TnmUtils.buildPayBillTransactionResponseResponse(true, null);

        Assertions.assertEquals(404, response.getInt("status"));
        Assertions.assertEquals("Transaction not found", response.getString("message"));
        Assertions.assertFalse(response.has("receipt_number"));
        Assertions.assertFalse(response.has("trans_id"));
    }

    @DisplayName("Response includes receipt_number and trans_id for successful transactions")
    @Test
    void test_successful_transaction_includes_receipt_and_trans_id() {
        TransactionStatusResponseDTO responseDto = mock(TransactionStatusResponseDTO.class);
        when(responseDto.getTransferState()).thenReturn(TransferState.COMMITTED);
        when(responseDto.getTransferId()).thenReturn("receipt123");
        when(responseDto.getTransactionId()).thenReturn("trans123");

        JSONObject result = TnmUtils.buildPayBillTransactionResponseResponse(true, responseDto);

        Assertions.assertEquals("receipt123", result.get("receipt_number"));
        Assertions.assertEquals("trans123", result.get("trans_id"));
    }

    @DisplayName("Method returns JSONObject with status and message fields for all cases")
    @Test
    void test_method_returns_status_and_message_fields() {
        JSONObject result = TnmUtils.buildPayBillTransactionResponseResponse(false, null);

        Assertions.assertTrue(result.has("status"));
        Assertions.assertTrue(result.has("message"));
        Assertions.assertEquals(404, result.get("status"));
        Assertions.assertEquals("Transaction not found", result.get("message"));
    }

    @DisplayName("Successful transaction with non-null response of correct type processes normally")
    @Test
    void test_successful_transaction_processes_normally() {
        TransactionStatusResponseDTO responseDto = mock(TransactionStatusResponseDTO.class);
        when(responseDto.getTransferState()).thenReturn(TransferState.COMMITTED);
        when(responseDto.getTransferId()).thenReturn("receipt123");
        when(responseDto.getTransactionId()).thenReturn("trans123");

        JSONObject result = TnmUtils.buildPayBillTransactionResponseResponse(true, responseDto);

        Assertions.assertEquals(200, result.get("status"));
        Assertions.assertEquals("Payment successful", result.get("message"));
    }

    @DisplayName("Successful transaction with COMMITTED transfer state returns 200 status and success message")
    @Test
    void test_successful_transaction_committed_state_wrong_object() {
        TransferState responseDto = TransferState.COMMITTED;

        JSONObject response = TnmUtils.buildPayBillTransactionResponseResponse(true, responseDto);

        Assertions.assertEquals(404, response.getInt("status"));
        Assertions.assertEquals("Transaction not found", response.getString("message"));
    }

    @DisplayName("Returns ROSTER_PRIMARY_IDENTIFIER_NAME when amsName is 'roster'")
    @Test
    void test_returns_roster_identifier_for_roster_ams() {
        String amsName = "roster";
        String result = TnmUtils.getPrimaryIdentifierName(amsName);
        Assertions.assertEquals(ROSTER_PRIMARY_IDENTIFIER_NAME, result);
    }

    @DisplayName("Pass null as amsName parameter")
    @Test
    void test_null_ams_name_throws_exception() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            TnmUtils.getPrimaryIdentifierName(null);
        });
    }

    @DisplayName("Returns ROSTER_PRIMARY_IDENTIFIER_NAME when amsName is 'roster'")
    @Test
    void test_returns_roster_identifier_for_fineract_ams() {
        String amsName = "fineract";
        String result = TnmUtils.getPrimaryIdentifierName(amsName);
        Assertions.assertEquals(FINERACT_PRIMARY_IDENTIFIER_NAME, result);
    }

    @DisplayName("Successfully converts TnmPayBillPayRequestDto to ChannelRequestDto with valid inputs")
    @Test
    void test_convert_paybill_to_channel_payload_success() {
        TnmPayBillPayRequestDto payBillRequest = new TnmPayBillPayRequestDto();
        payBillRequest.setMsisdn("254712345678");
        payBillRequest.setAccountNumber("ACC123");
        payBillRequest.setTransactionAmount("100");

        String amsName = "fineract";
        String currency = "USD";

        ChannelRequestDto result = TnmUtils.convertPayBillToChannelPayload(payBillRequest, amsName, currency);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("254712345678", result.getPayer().getJSONObject("partyIdInfo").getString("partyIdentifier"));
        Assertions.assertEquals("MSISDN", result.getPayer().getJSONObject("partyIdInfo").getString("partyIdType"));
        Assertions.assertEquals("ACC123", result.getPayee().getJSONObject("partyIdInfo").getString("partyIdentifier"));
        Assertions.assertEquals("fineractAccountID", result.getPayee().getJSONObject("partyIdInfo").getString("partyIdType"));
        Assertions.assertEquals("100", result.getAmount().getString("amount"));
        Assertions.assertEquals("USD", result.getAmount().getString("currency"));
    }

}

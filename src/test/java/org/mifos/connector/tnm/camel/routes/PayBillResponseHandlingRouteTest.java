package org.mifos.connector.tnm.camel.routes;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mifos.connector.common.mojaloop.type.TransferState.COMMITTED;
import static org.mifos.connector.tnm.camel.config.CamelProperties.TNM_PAY_OAF_TRANSACTION_REFERENCE;

import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.common.channel.dto.TransactionStatusResponseDTO;
import org.mifos.connector.tnm.ConnectorTemplateApplicationTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PayBillResponseHandlingRouteTest extends ConnectorTemplateApplicationTests {

    @Autowired
    private FluentProducerTemplate fluentProducerTemplate;

    @DisplayName("Test pay bill response success")
    @Test
    void test_payBill_response_success() {

        // Send to the route
        Exchange result = fluentProducerTemplate.to("direct:paybill-pay-response-success")
                .withHeader(TNM_PAY_OAF_TRANSACTION_REFERENCE, "test").send();
        String receivedBody = result.getIn().getBody(String.class);

        assertTrue(receivedBody.contains("\"status\":200"));
        assertTrue(receivedBody.contains("\"message\":\"Payment successful\""));
        assertTrue(receivedBody.contains("\"receipt_number\":\"test\""));
    }

    @DisplayName("Test pay bill response success without receipt id")
    @Test
    void test_payBill_response_success_with_out_receipt_id() {

        // Send to the route
        Exchange result = fluentProducerTemplate.to("direct:paybill-pay-response-success").send();
        String receivedBody = result.getIn().getBody(String.class);

        assertTrue(receivedBody.contains("\"status\":200"));
        assertTrue(receivedBody.contains("\"message\":\"Payment successful\""));
        assertTrue(receivedBody.contains("\"receipt_number\":\"\""));
    }

    @DisplayName("Test pay bill response error")
    @Test
    void test_payBill_response_error() {

        // Send to the route
        Exchange result = fluentProducerTemplate.to("direct:paybill-transaction-status-response-failure").send();
        String receivedBody = result.getIn().getBody(String.class);

        assertTrue(receivedBody.contains("\"status\":404"));
        assertTrue(receivedBody.contains("\"message\":\"Transaction not found\""));
    }

    @DisplayName("Test pay bill response sucess with transaction id")
    @Test
    void test_payBill_response_success_with_transaction_id() {

        // Prepare the response body as a successful transaction
        TransactionStatusResponseDTO transactionStatusResponseDto = new TransactionStatusResponseDTO();
        transactionStatusResponseDto.setTransactionId("12345");
        transactionStatusResponseDto.setClientRefId("12345");
        transactionStatusResponseDto.setTransferId("54321");
        transactionStatusResponseDto.setTransferState(COMMITTED);
        // Send the exchange to the route
        Exchange result = fluentProducerTemplate.to("direct:paybill-transaction-status-response-success")
                .withBody(transactionStatusResponseDto).send();

        String receivedBody = result.getIn().getBody(String.class);
        System.out.println("Received body: " + receivedBody);

        // Assertions
        assertTrue(receivedBody.contains("\"status\":200"));
        assertTrue(receivedBody.contains("\"message\":\"Payment successful\""));
        assertTrue(receivedBody.contains("\"trans_id\":\"12345\""));
        assertTrue(receivedBody.contains("\"receipt_number\":\"54321\""));

    }
}

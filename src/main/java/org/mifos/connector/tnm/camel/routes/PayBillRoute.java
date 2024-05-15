package org.mifos.connector.tnm.camel.routes;

import static org.mifos.connector.tnm.camel.config.CamelProperties.BRIDGE_ENDPOINT_QUERY_PARAM;
import static org.mifos.connector.tnm.camel.config.CamelProperties.CAMEL_HTTP_RESPONSE_CODE;
import static org.mifos.connector.tnm.camel.config.CamelProperties.CHANNEL_URL_PROPERTY_IN_HEADER;
import static org.mifos.connector.tnm.camel.config.CamelProperties.CUSTOM_HEADER_FILTER_STRATEGY;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.common.camel.ErrorHandlerRouteBuilder;
import org.mifos.connector.common.channel.dto.TransactionStatusResponseDTO;
import org.mifos.connector.tnm.dto.PayBillErrorResponse;
import org.mifos.connector.tnm.dto.PayBillValidationResponseDto;
import org.mifos.connector.tnm.dto.TnmPayBillPayRequestDto;
import org.mifos.connector.tnm.exception.TNMConnectorException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Class where Paybill routes are defined.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayBillRoute extends ErrorHandlerRouteBuilder {

    private final PayBillRouteProcessor payBillRouteProcessor;

    @Override
    public void configure() {

        onException(Exception.class).to("direct:error-response").handled(true);

        // Validate Route
        from("rest:GET:/paybill/validate/{clientAccountNumber}").id("paybill-validation-route")
                .to("direct:account-status").unmarshal().json(JsonLibrary.Jackson, PayBillValidationResponseDto.class)
                .log("## Is reconciled: ${body.isReconciled}").choice().when().simple("${body.isReconciled} == 'true'")
                .to("direct:start-paybill-workflow").to("direct:paybill-validation-response-success").otherwise()
                .to("direct:paybill-validation-response-failure").end();

        // Pay Route
        from("rest:POST:/paybill/pay").id("paybill-pay-route-base")
                .log(LoggingLevel.INFO, "## PayBill start Pay request processing").to("direct:paybill-pay-route")
                .log("## PayBill start request sent to channel").end();

        // Get Transaction status Route
        from("rest:GET:/paybill/confirm/{paybillTransactionId}").id("paybill-transaction-status-check-route")
                .log(LoggingLevel.INFO, " ## PayBill Transaction status request")
                .to("direct:paybill-transaction-status-check-base").unmarshal()
                .json(JsonLibrary.Jackson, TransactionStatusResponseDTO.class).choice()
                .when(header(CAMEL_HTTP_RESPONSE_CODE).isEqualTo("200"))
                .to("direct:paybill-transaction-status-response-success").otherwise()
                .to("direct:paybill-transaction-status-response-failure").end();

        from("direct:account-status").id("account-status-route")
                .log(LoggingLevel.INFO, "## PayBill Validation Payload request")
                .setBody(payBillRouteProcessor::buildBodyForAccountStatus)
                .toD(CHANNEL_URL_PROPERTY_IN_HEADER
                        + "/accounts/validate/${header.primaryIdentifier}/${header.primaryIdentifierValue}"
                        + BRIDGE_ENDPOINT_QUERY_PARAM)
                .log(LoggingLevel.INFO, "Account Status request sent to channel Paybill validate endpoint")
                .log(LoggingLevel.DEBUG, "Status: ${header.CamelHttpResponseCode}")
                .log(LoggingLevel.DEBUG, "Channel Validation response: \n.. ${body}");

        from("direct:start-paybill-workflow").id("start-paybill-workflow")
                .log(LoggingLevel.INFO, "Starting Tnm Workflow for PayBill")
                .setBody(payBillRouteProcessor::buildBodyForStartPayBillWorkflow)
                .toD(CHANNEL_URL_PROPERTY_IN_HEADER + "/channel/gsma/transaction" + BRIDGE_ENDPOINT_QUERY_PARAM
                        + "&headerFilterStrategy=#" + CUSTOM_HEADER_FILTER_STRATEGY)
                .log(LoggingLevel.INFO, "Starting GSMA Txn workflow in channel")
                .to("log:INFO?showBody=true&showHeaders=true");

        from("direct:paybill-pay-route").id("paybill-pay-route")
                .log(LoggingLevel.INFO, "Starting Tnm PayBill Pay route").unmarshal()
                .json(JsonLibrary.Jackson, TnmPayBillPayRequestDto.class)
                .process(payBillRouteProcessor::processRequestForPayBillPayRoute).onException(Exception.class)
                .log(LoggingLevel.ERROR, "Error: ${exception.message}").end();

        from("direct:paybill-transaction-status-check-base").id("paybill-transaction-status-check-base")
                .log(LoggingLevel.INFO, "## PayBill Transaction status request")
                .process(payBillRouteProcessor::processRequestForTransactionStatusCheck)
                .toD(CHANNEL_URL_PROPERTY_IN_HEADER + "/channel/transfer/${header.paybillTransactionId}"
                        + BRIDGE_ENDPOINT_QUERY_PARAM)
                .log(LoggingLevel.INFO, "Transaction Status request sent to channel")
                .log(LoggingLevel.DEBUG, "Status: ${header.CamelHttpResponseCode}")
                .log(LoggingLevel.DEBUG, "Channel Trx status response: \n\n.. ${body}");

        from("direct:error-response").id("error-response")
                .log(LoggingLevel.ERROR, "Error message: ${exception.message}")
                .log(LoggingLevel.ERROR, "Error: ${exception}").process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    PayBillErrorResponse errorResponse = new PayBillErrorResponse();
                    errorResponse.setMessage(exception.getMessage());
                    exchange.getIn().setBody(errorResponse);
                    HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                    if (exception instanceof TNMConnectorException tnmConnectorException) {
                        httpStatus = tnmConnectorException.getHttpStatus();
                    }
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, httpStatus.value());
                }).marshal().json(JsonLibrary.Jackson);
    }
}

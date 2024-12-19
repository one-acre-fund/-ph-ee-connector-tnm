package org.mifos.connector.tnm.camel.routes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mifos.connector.tnm.camel.config.CamelProperties.*;
import static org.mifos.connector.tnm.zeebe.ZeebeVariables.CURRENCY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.CreateProcessInstanceCommandStep1;
import io.camunda.zeebe.client.api.command.PublishMessageCommandStep1;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import java.util.UUID;
import org.apache.camel.*;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.tnm.ConnectorTemplateApplicationTests;
import org.mifos.connector.tnm.camel.config.AmsPayBillProperties;
import org.mifos.connector.tnm.camel.config.AmsProperties;
import org.mifos.connector.tnm.camel.config.ZeebeProperties;
import org.mifos.connector.tnm.dto.ChannelValidationRequestDto;
import org.mifos.connector.tnm.dto.PayBillValidationResponseDto;
import org.mifos.connector.tnm.dto.TnmPayBillPayRequestDto;
import org.mifos.connector.tnm.exception.MissingFieldException;
import org.mifos.connector.tnm.exception.TnmConnectorExistingTransactionIdException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

class PayBillRouteProcessorTest extends ConnectorTemplateApplicationTests {

    @Mock
    private ZeebeClient zeebeClient;

    private PayBillRouteProcessor processor;
    @Mock
    private ProducerTemplate producerTemplate;
    @Mock
    private AmsPayBillProperties amsPayBillProps;
    @Mock
    private ZeebeProperties zeebeProperties;

    @Autowired
    private CamelContext camelContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new PayBillRouteProcessor(producerTemplate, zeebeClient, amsPayBillProps, zeebeProperties);
    }

    @DisplayName("Successfully builds account status request body with all required headers present")
    @Test
    void test_build_body_with_valid_headers() throws JsonProcessingException {
        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();

        exchange.getIn().setHeader(CLIENT_ACCOUNT_NUMBER, "12345");
        exchange.getIn().setHeader(CURRENCY, "MWK");
        exchange.getIn().setHeader(BUSINESS_SHORT_CODE, "24322607");
        exchange.getIn().setHeader(SECONDARY_IDENTIFIER_NAME, "roster");
        exchange.getIn().setHeader(GET_ACCOUNT_DETAILS_FLAG, false);

        AmsProperties amsProperties = new AmsProperties();
        amsProperties.setAms("fineract");
        amsProperties.setCurrency("MWK");
        amsProperties.setBaseUrl("http://test.com");
        amsProperties.setBusinessShortCode("24322607");

        when(amsPayBillProps.getAmsPropertiesFromShortCode(anyString())).thenReturn(amsProperties);
        when(amsPayBillProps.getDefaultAmsShortCode()).thenReturn("BSC001");
        when(amsPayBillProps.getDefaultAmsShortCode()).thenReturn("BSC001");
        when(amsPayBillProps.getAccountHoldingInstitutionId()).thenReturn("TEST_ID");

        ObjectMapper objectMapper = new ObjectMapper();
        String result = processor.buildBodyForAccountStatus(exchange);
        ChannelValidationRequestDto requestDto = objectMapper.readValue(result, ChannelValidationRequestDto.class);

        assertNotNull(result);
        assertEquals("fineractAccountID", requestDto.getPrimaryIdentifier().getKey());
        assertEquals("12345", requestDto.getPrimaryIdentifier().getValue());
        assertEquals("transactionId", requestDto.getCustomData().get(0).key);
        assertDoesNotThrow(() -> UUID.fromString(requestDto.getCustomData().get(0).value.toString()), "transaction id is not a valid UUID");
        assertEquals("currency", requestDto.getCustomData().get(1).key);
        assertEquals("MWK", requestDto.getCustomData().get(1).value);
        assertEquals("getAccountDetails", requestDto.getCustomData().get(2).key);
        assertEquals(false, requestDto.getCustomData().get(2).value);
        assertEquals("application/json", exchange.getIn().getHeader(CONTENT_TYPE));
        assertEquals("fineract", exchange.getIn().getHeader("amsName"));
    }

    @DisplayName("Successfully builds account status request body with all required headers present but without the currency and short code")
    @Test
    void test_build_body_with_valid_headers_without_currency_and_short_code() throws JsonProcessingException {
        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();

        exchange.getIn().setHeader(CLIENT_ACCOUNT_NUMBER, "12345");
        exchange.getIn().setHeader(SECONDARY_IDENTIFIER_NAME, "roster");
        AmsProperties amsProperties = new AmsProperties();
        amsProperties.setAms("fineract");
        amsProperties.setCurrency("MWK");
        amsProperties.setBaseUrl("http://test.com");
        amsProperties.setBusinessShortCode("24322607");

        when(amsPayBillProps.getAmsPropertiesFromShortCode(anyString())).thenReturn(amsProperties);
        when(amsPayBillProps.getDefaultAmsShortCode()).thenReturn("BSC001");
        when(amsPayBillProps.getDefaultAmsShortCode()).thenReturn("BSC001");
        when(amsPayBillProps.getAccountHoldingInstitutionId()).thenReturn("TEST_ID");

        ObjectMapper objectMapper = new ObjectMapper();
        String result = processor.buildBodyForAccountStatus(exchange);
        ChannelValidationRequestDto requestDto = objectMapper.readValue(result, ChannelValidationRequestDto.class);

        assertNotNull(result);
        assertEquals("fineractAccountID", requestDto.getPrimaryIdentifier().getKey());
        assertEquals("12345", requestDto.getPrimaryIdentifier().getValue());
        assertEquals("transactionId", requestDto.getCustomData().get(0).key);
        assertDoesNotThrow(() -> UUID.fromString(requestDto.getCustomData().get(0).value.toString()), "transaction id is not a valid UUID");
        assertEquals("currency", requestDto.getCustomData().get(1).key);
        assertEquals("MWK", requestDto.getCustomData().get(1).value);
        assertEquals("getAccountDetails", requestDto.getCustomData().get(2).key);
        assertEquals(true, requestDto.getCustomData().get(2).value);
        assertEquals("application/json", exchange.getIn().getHeader(CONTENT_TYPE));
        assertEquals("fineract", exchange.getIn().getHeader("amsName"));
    }

    @DisplayName("Handles missing MSISDN by throwing MissingFieldException")
    @Test
    void test_build_body_missing_msisdn_throws_exception() {
        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();

        exchange.getIn().setHeader(CLIENT_ACCOUNT_NUMBER, "12345");
        exchange.getIn().setHeader(CURRENCY, "USD");
        exchange.getIn().setHeader(BUSINESS_SHORT_CODE, "BSC001");
        // MSISDN header intentionally omitted
        exchange.getIn().setHeader(GET_ACCOUNT_DETAILS_FLAG, true);

        AmsProperties amsProperties = new AmsProperties();
        amsProperties.setAms("TEST_AMS");
        amsProperties.setCurrency("USD");
        amsProperties.setBaseUrl("http://test.com");

        when(amsPayBillProps.getAmsPropertiesFromShortCode(anyString())).thenReturn(amsProperties);

        MissingFieldException exception = assertThrows(MissingFieldException.class, () -> {
            processor.buildBodyForAccountStatus(exchange);
        });
        assertEquals("MSISDN is required for PayBill validation", exception.getMessage());

    }

    @DisplayName("Successfully processes PayBillValidationResponseDto and returns serialized GsmaTransfer")
    @Test
    void test_successful_processing_of_payBill_validation_response() {
        // Arrange
        PayBillValidationResponseDto validationResponseDto = new PayBillValidationResponseDto();
        validationResponseDto.setReconciled(true);
        validationResponseDto.setTransactionId("test-txn-123");
        validationResponseDto.setAccountHoldingInstitutionId("inst-123");
        validationResponseDto.setAmsName("test-ams");
        validationResponseDto.setClientName("test-client");
        validationResponseDto.setMsisdn("123456789");
        validationResponseDto.setAmount("100");
        validationResponseDto.setCurrency("USD");

        when(zeebeProperties.getWaitTnmPayRequestPeriod()).thenReturn(5);

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();
        exchange.getIn().setBody(validationResponseDto);

        // Act
        String result = processor.buildBodyForStartPayBillWorkflow(exchange);

        // Assert
        assertNotNull(result);
        assertEquals("inst-123", exchange.getIn().getHeader(ACCOUNT_HOLDING_INSTITUTION_ID));
        assertEquals("test-ams", exchange.getIn().getHeader(AMS_NAME));
        assertEquals("application/json", exchange.getIn().getHeader(CONTENT_TYPE));
        assertEquals("inst-123", exchange.getIn().getHeader(TENANT_ID));
        assertEquals("test-txn-123", exchange.getIn().getHeader(X_CORRELATION_ID));
        assertEquals("test-client", exchange.getIn().getHeader(CLIENT_NAME));
        assertTrue((Boolean) exchange.getProperty("isValidationReferencePresent"));
    }

    @DisplayName("Successfully processes PayBill request with valid transaction ID and OAF reference")
    @Test
    void test_process_valid_paybill_request() {
        // Arrange

        TnmPayBillPayRequestDto requestDto = new TnmPayBillPayRequestDto();
        requestDto.setTransactionId("TEST-TXN-123");
        requestDto.setOafValidationRef("OAF-REF-123");
        requestDto.setMsisdn("123456789");
        requestDto.setTransactionAmount("100");
        requestDto.setAccountNumber("ACC123");

        Exchange exchange = mock(Exchange.class);
        PublishMessageCommandStep1 publishMessageCommand = mock(PublishMessageCommandStep1.class);

        Message message = mock(Message.class);

        AmsProperties amsProps = new AmsProperties();
        amsProps.setAms("TEST-AMS");
        amsProps.setCurrency("USD");
        amsProps.setBaseUrl("http://test-url");

        when(amsPayBillProps.getAmsPropertiesFromShortCode(any())).thenReturn(amsProps);
        when(zeebeClient.newPublishMessageCommand()).thenReturn(mock(PublishMessageCommandStep1.class));
        when(producerTemplate.send(eq("direct:paybill-transaction-status-check-base"), any(Processor.class))).thenAnswer(invocation -> {
            Processor processor = invocation.getArgument(1, Processor.class);
            processor.process(exchange);
            return exchange;
        });
        when(exchange.getIn()).thenReturn(message);
        when(message.getBody(TnmPayBillPayRequestDto.class)).thenReturn(requestDto);

        CreateProcessInstanceCommandStep1 createProcessInstanceCommand = mock(CreateProcessInstanceCommandStep1.class);
        CreateProcessInstanceCommandStep1.CreateProcessInstanceCommandStep2 createProcessInstanceCommandStep2 = mock(
                CreateProcessInstanceCommandStep1.CreateProcessInstanceCommandStep2.class);
        CreateProcessInstanceCommandStep1.CreateProcessInstanceCommandStep3 createProcessInstanceCommandStep3 = mock(
                CreateProcessInstanceCommandStep1.CreateProcessInstanceCommandStep3.class);
        when(zeebeClient.newCreateInstanceCommand()).thenReturn(createProcessInstanceCommand);
        when(createProcessInstanceCommand.bpmnProcessId(anyString())).thenReturn(createProcessInstanceCommandStep2);
        when(createProcessInstanceCommandStep2.latestVersion()).thenReturn(createProcessInstanceCommandStep3);
        when(createProcessInstanceCommandStep3.variables(anyMap())).thenReturn(createProcessInstanceCommandStep3);
        when(createProcessInstanceCommandStep3.send()).thenReturn(mock(ZeebeFuture.class));

        PublishMessageCommandStep1.PublishMessageCommandStep2 publishMessageCommandStep2 = mock(
                PublishMessageCommandStep1.PublishMessageCommandStep2.class);

        PublishMessageCommandStep1.PublishMessageCommandStep3 publishMessageCommandStep3 = mock(
                PublishMessageCommandStep1.PublishMessageCommandStep3.class);

        ZeebeFuture<PublishMessageResponse> zeebeFutureMock = mock(ZeebeFuture.class);

        when(zeebeClient.newPublishMessageCommand()).thenReturn(publishMessageCommand);
        when(publishMessageCommand.messageName(anyString())).thenReturn(publishMessageCommandStep2);
        when(publishMessageCommandStep2.correlationKey(anyString())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.timeToLive(any())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.variables(anyMap())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.send()).thenReturn(zeebeFutureMock);

        // Act
        processor.processRequestForPayBillPayRoute(exchange);

        // Assert
        verify(zeebeClient).newPublishMessageCommand();
        verify(zeebeClient).newCreateInstanceCommand();
    }

    @DisplayName("Successfully processes PayBill request with valid transaction ID and OAF reference")
    @Test
    void test_process_valid_paybill_requests() {
        // Arrange

        TnmPayBillPayRequestDto requestDto = new TnmPayBillPayRequestDto();
        requestDto.setTransactionId("TEST-TXN-123");
        requestDto.setOafValidationRef("OAF-REF-123");
        requestDto.setMsisdn("123456789");
        requestDto.setTransactionAmount("100");
        requestDto.setAccountNumber("ACC123");

        Exchange exchange = mock(Exchange.class);
        PublishMessageCommandStep1 publishMessageCommand = mock(PublishMessageCommandStep1.class);

        Message message = mock(Message.class);

        AmsProperties amsProps = new AmsProperties();
        amsProps.setAms("TEST-AMS");
        amsProps.setCurrency("USD");
        amsProps.setBaseUrl("http://test-url");

        when(amsPayBillProps.getAmsPropertiesFromShortCode(any())).thenReturn(amsProps);
        when(zeebeClient.newPublishMessageCommand()).thenReturn(mock(PublishMessageCommandStep1.class));
        when(producerTemplate.send(eq("direct:paybill-transaction-status-check-base"), any(Processor.class))).thenAnswer(invocation -> {
            Processor processor = invocation.getArgument(1, Processor.class);
            processor.process(exchange);
            return exchange;
        });
        when(exchange.getIn()).thenReturn(message);
        when(message.getBody(TnmPayBillPayRequestDto.class)).thenReturn(requestDto);

        PublishMessageCommandStep1.PublishMessageCommandStep2 publishMessageCommandStep2 = mock(
                PublishMessageCommandStep1.PublishMessageCommandStep2.class);

        PublishMessageCommandStep1.PublishMessageCommandStep3 publishMessageCommandStep3 = mock(
                PublishMessageCommandStep1.PublishMessageCommandStep3.class);

        ZeebeFuture<PublishMessageResponse> zeebeFutureMock = mock(ZeebeFuture.class);

        when(zeebeClient.newPublishMessageCommand()).thenReturn(publishMessageCommand);
        when(publishMessageCommand.messageName(anyString())).thenReturn(publishMessageCommandStep2);
        when(publishMessageCommandStep2.correlationKey(anyString())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.timeToLive(any())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.variables(anyMap())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.send()).thenReturn(zeebeFutureMock);

        PayBillRouteProcessor.workflowInstanceStore.put(requestDto.getOafValidationRef(), "TEST-INSTANCE-123");
        // Act
        processor.processRequestForPayBillPayRoute(exchange);

        // Assert
        verify(zeebeClient).newPublishMessageCommand();
        // verify(zeebeClient).newCreateInstanceCommand();
    }

    @DisplayName("Validate transaction ID that does not exist in the system")
    @Test
    void test_validate_non_existing_transaction_id() {
        // Arrange
        String transactionId = "test-123";
        Exchange mockExchange = mock(Exchange.class);
        Message mockMessage = mock(Message.class);

        when(producerTemplate.send(eq("direct:paybill-transaction-status-check-base"), any(Processor.class))).thenReturn(mockExchange);
        when(mockExchange.getIn()).thenReturn(mockMessage);
        when(mockMessage.getBody(String.class)).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> processor.validateUniqueTransactionId(transactionId));
    }

    @DisplayName("Handle JsonProcessingException during response deserialization")
    @Test
    void test_handle_json_processing_exception() {
        // Arrange
        String transactionId = "test-123";
        String invalidJson = "invalid-json";
        Exchange mockExchange = mock(Exchange.class);
        Message mockMessage = mock(Message.class);

        when(producerTemplate.send(eq("direct:paybill-transaction-status-check-base"), any(Processor.class))).thenReturn(mockExchange);
        when(mockExchange.getIn()).thenReturn(mockMessage);
        when(mockMessage.getBody(String.class)).thenReturn(invalidJson);
        // Act & Assert
        assertThrows(JsonProcessingException.class, () -> processor.validateUniqueTransactionId(transactionId));
    }

    // Process response with COMMITTED transfer state throws exception
    @Test
    void test_committed_transfer_state() {
        String transactionId = "test-123";
        String validJson = "{\"transferState\":\"COMMITTED\"}";
        Exchange mockExchange = mock(Exchange.class);
        Message mockMessage = mock(Message.class);

        when(producerTemplate.send(eq("direct:paybill-transaction-status-check-base"), any(Processor.class))).thenReturn(mockExchange);
        when(mockExchange.getIn()).thenReturn(mockMessage);
        when(mockMessage.getBody(String.class)).thenReturn(validJson);

        // Act & Assert
        assertThrows(TnmConnectorExistingTransactionIdException.class, () -> processor.validateUniqueTransactionId(transactionId));
    }

    @DisplayName("Process response with non-COMMITTED transfer state")
    @Test
    void test_non_committed_transfer_state() {
        String transactionId = "test-123";
        String validJson = "{\"transferState\":\"RECEIVED\"}";
        Exchange mockExchange = mock(Exchange.class);
        Message mockMessage = mock(Message.class);

        when(producerTemplate.send(eq("direct:paybill-transaction-status-check-base"), any(Processor.class))).thenReturn(mockExchange);
        when(mockExchange.getIn()).thenReturn(mockMessage);
        when(mockMessage.getBody(String.class)).thenReturn(validJson);

        // Act & Assert
        assertDoesNotThrow(() -> processor.validateUniqueTransactionId(transactionId));
    }

    @DisplayName("Valid transaction ID in header leads to successful processing")
    @Test
    void test_valid_transaction_id_processing() {

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();

        Message message = exchange.getIn();
        message.setHeader(PAYBILL_TRANSACTION_ID_URL_PARAM, "valid-transaction-id");

        processor.processRequestForTransactionStatusCheck(exchange);

        assertEquals("application/json", exchange.getIn().getHeader(CONTENT_TYPE));
        assertEquals("transfers", exchange.getIn().getHeader("requestType"));
        assertEquals("oaf", exchange.getIn().getHeader(TENANT_ID));
        assertEquals("valid-transaction-id", exchange.getProperty(PAYBILL_TRANSACTION_ID_URL_PARAM));
    }

    @DisplayName("Null transaction ID in header throws MissingFieldException")
    @Test
    void test_null_transaction_id_throws_exception() {
        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();

        Message message = exchange.getIn();
        message.setHeader(PAYBILL_TRANSACTION_ID_URL_PARAM, null);

        assertThrows(MissingFieldException.class, () -> {
            processor.processRequestForTransactionStatusCheck(exchange);
        });
    }

    @DisplayName("Null transaction ID in header throws MissingFieldException")
    @Test
    void test_empty_transaction_id_throws_exception() {
        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();

        Message message = exchange.getIn();
        message.setHeader(PAYBILL_TRANSACTION_ID_URL_PARAM, "");

        assertThrows(MissingFieldException.class, () -> {
            processor.processRequestForTransactionStatusCheck(exchange);
        });
    }

    @DisplayName("Successfully processes PayBill validation response with valid channel response and client correlation ID")
    @Test
    void test_successful_payBill_validation_response_processing() {
        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();

        String channelResponse = "{\"transactionId\":\"123\"}";
        exchange.getIn().setBody(channelResponse);
        exchange.getIn().setHeader(X_CORRELATION_ID, "corr-123");
        exchange.getIn().setHeader(CLIENT_NAME, "John Doe");

        PayBillRouteProcessor.reconciledStore.put("corr-123", true);

        processor.processResponseForPayBillValidationResponseSuccess(exchange);

        String responseBody = exchange.getIn().getBody(String.class);
        JSONObject response = new JSONObject(responseBody);

        assertEquals(200, response.getInt("status"));
        assertEquals("Account exists", response.getString("message"));
        assertEquals("corr-123", response.getString("oafTransactionReference"));
        assertEquals("John Doe", response.getString("clientName"));
        assertEquals("123", PayBillRouteProcessor.workflowInstanceStore.get("corr-123"));
        assertFalse(PayBillRouteProcessor.reconciledStore.containsKey("corr-123"));
    }

    @DisplayName("Processes error response")
    @Test
    void test_process_error_response_creates_validation_response() {
        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("error", "Some error");
        exchange.getIn().setBody(errorResponse.toString());

        processor.processResponseForPayBillValidationResponseError(exchange);

        JSONObject response = new JSONObject(exchange.getIn().getBody(String.class));
        assertEquals(404, response.getInt("status"));
        assertEquals("Account does not exists or payment not allowed", response.getString("message"));
        assertNull(response.optString("clientName", null));
        assertNull(response.optString("oafTransactionReference", null));
    }

}

package org.mifos.connector.tnm.camel.routes;

import com.fasterxml.jackson.core.JsonParseException;
import io.camunda.zeebe.client.api.command.ClientException;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.tnm.ConnectorTemplateApplicationTests;
import org.mifos.connector.tnm.dto.PayBillErrorResponse;
import org.mifos.connector.tnm.exception.TnmConnectorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PayBillRouteTest extends ConnectorTemplateApplicationTests {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private FluentProducerTemplate fluentProducerTemplate;

    @DisplayName("Test error response route with TnmConnectorException")
    @Test
    void testErrorResponseRoute_withTnmConnectorException() {

        // Set up an exception to simulate a TnmConnectorException
        Exchange exchange = camelContext.getEndpoint("direct:error-response").createExchange();
        TnmConnectorException tnmConnectorException = new TnmConnectorException("Connector error", HttpStatus.BAD_REQUEST);
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, tnmConnectorException);

        // Send to the route
        Exchange result = fluentProducerTemplate.to("direct:error-response").withExchange(exchange).send();
        PayBillErrorResponse receivedBody = result.getIn().getBody(PayBillErrorResponse.class);

        System.out.println("Result: " + receivedBody);
        System.out.println("Result body: " + result.getIn().getBody().toString());

        Assertions.assertEquals(400, receivedBody.getStatus());
        Assertions.assertEquals("Connector error", receivedBody.getMessage());
    }

    @DisplayName("Test error response route with JsonParseException")
    @Test
    void testErrorResponseRoute_withJsonParseException() {

        // Set up an exception to simulate a TnmConnectorException
        Exchange exchange = camelContext.getEndpoint("direct:error-response").createExchange();
        JsonParseException jsonParseException = new JsonParseException(null, "Json parse exception ");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, jsonParseException);

        // Send to the route
        Exchange result = fluentProducerTemplate.to("direct:error-response").withExchange(exchange).send();
        PayBillErrorResponse receivedBody = result.getIn().getBody(PayBillErrorResponse.class);

        Assertions.assertEquals(500, receivedBody.getStatus());
        Assertions.assertEquals("Internal error while processing the request. Please try again later.", receivedBody.getMessage());
    }

    @DisplayName("Test error response route with ClientException")
    @Test
    void testErrorResponseRoute_withClientException() {

        // Set up an exception to simulate a TnmConnectorException
        Exchange exchange = camelContext.getEndpoint("direct:error-response").createExchange();
        ClientException clientException = new ClientException("Client error");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, clientException);

        // Send to the route
        Exchange result = fluentProducerTemplate.to("direct:error-response").withExchange(exchange).send();
        PayBillErrorResponse receivedBody = result.getIn().getBody(PayBillErrorResponse.class);

        System.out.println("Result: " + receivedBody.getStatus());
        System.out.println("Result body: " + result.getIn().getBody().toString());

        Assertions.assertEquals(503, receivedBody.getStatus());
        Assertions.assertEquals("Internal systems are not available. Please try again later.", receivedBody.getMessage());
    }

}

package service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.GreenActivity;

/**
 * Handler for requests to Lambda function.
 */
public class ReadActivityLambda {

    public APIGatewayProxyResponseEvent getActivity(final APIGatewayProxyRequestEvent input) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        GreenActivity greenActivity = new GreenActivity("12312342", "ABC200", "DAL-HOU",
                true, 1, "2021-08-11");
        String json = objectMapper.writeValueAsString(greenActivity);
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(json);
    }
}

package service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementRequest;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementResult;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.RequestLimitExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.GreenActivitySummary;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class ReadSummaryLambda {

    static AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
    static ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        APIGatewayProxyResponseEvent responseEvent = new ReadSummaryLambda().readSummary();
        System.out.println(responseEvent.getHeaders());
        System.out.println(responseEvent.getBody());
    }


    public APIGatewayProxyResponseEvent readSummary() {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(getGreenActivitySummary())
                .withHeaders(corsHeaders());
    }

    private static String getGreenActivitySummary() {
        GreenActivitySummary result = new GreenActivitySummary(0, 0, 0, 0, 0);

        try {
            ExecuteStatementRequest request = createRequest();
            ExecuteStatementResult statementResult = dynamoDB.executeStatement(request);
            result = extractSummary(statementResult);
        } catch (Exception e) {
            handleCommonErrors(e);
        }

        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return result.toString();
        }
    }

    private static GreenActivitySummary extractSummary(ExecuteStatementResult result) {
        int electronicBoardingPassRewards = 0;
        int checkedBagRewards = 0;
        int greenIdeasRewards = 0;
        int approvedGreenIdeasRewards = 0;

        for (Map<String, AttributeValue> item : result.getItems()) {
            int checkedBags = getIntValue(item, "checkedBags");
            if (checkedBags == 0) {
                checkedBagRewards += 10;
            }

            if (Boolean.TRUE.equals(item.get("isElectronicBoardingPass").getBOOL())) {
                electronicBoardingPassRewards++;
            }

            greenIdeasRewards += getIntValue(item, "greenIdeas") * 2;
            approvedGreenIdeasRewards += getIntValue(item, "approvedGreenIdeas") * 50;
        }

        return new GreenActivitySummary(electronicBoardingPassRewards, checkedBagRewards, 0, greenIdeasRewards, approvedGreenIdeasRewards);
    }

    private static int getIntValue(Map<String, AttributeValue> item, String field) {
        try {
            return Integer.parseInt(item.get(field).getN());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static ExecuteStatementRequest createRequest() {
        ExecuteStatementRequest request = new ExecuteStatementRequest();
        DateTime dateTime = DateTime.now().withDayOfYear(1).withTimeAtStartOfDay();

        // Create statements
        request.setStatement("select  checkedBags, isElectronicBoardingPass, greenIdeas, approvedGreenIdeas from GreenActivity where activityDate > ?");
        AttributeValue dateAttribute = new AttributeValue(dateTime.toString("yyyy-MM-dd"));
        request.setParameters(Collections.singletonList(dateAttribute));

        return request;
    }

    private static void handleCommonErrors(Exception exception) {
        try {
            throw exception;
        } catch (InternalServerErrorException isee) {
            System.out.println("Internal Server Error, generally safe to retry with exponential back-off. Error: " + isee.getErrorMessage());
        } catch (RequestLimitExceededException rlee) {
            System.out.println("Throughput exceeds the current throughput limit for your account, increase account level throughput before " +
                    "retrying. Error: " + rlee.getErrorMessage());
        } catch (ProvisionedThroughputExceededException ptee) {
            System.out.println("Request rate is too high. If you're using a custom retry strategy make sure to retry with exponential back-off. " +
                    "Otherwise consider reducing frequency of requests or increasing provisioned capacity for your table or secondary index. Error: " +
                    ptee.getErrorMessage());
        } catch (ResourceNotFoundException rnfe) {
            System.out.println("One of the tables was not found, verify table exists before retrying. Error: " + rnfe.getErrorMessage());
        } catch (AmazonServiceException ase) {
            System.out.println("An AmazonServiceException occurred, indicates that the request was correctly transmitted to the DynamoDB " +
                    "service, but for some reason, the service was not able to process it, and returned an error response instead. Investigate and " +
                    "configure retry strategy. Error type: " + ase.getErrorType() + ". Error message: " + ase.getErrorMessage());
        } catch (AmazonClientException ace) {
            System.out.println("An AmazonClientException occurred, indicates that the client was unable to get a response from DynamoDB " +
                    "service, or the client was unable to parse the response from the service. Investigate and configure retry strategy. " +
                    "Error: " + ace.getMessage());
        } catch (Exception e) {
            System.out.println("An exception occurred, investigate and configure retry strategy. Error: " + e.getMessage());
        }
    }

    private static Map<String, String> corsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "OPTIONS,POST,GET");

        return headers;
    }
}

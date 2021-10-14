package service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementRequest;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementResult;
import com.amazonaws.services.dynamodbv2.model.ExecuteTransactionRequest;
import com.amazonaws.services.dynamodbv2.model.ExecuteTransactionResult;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ItemResponse;
import com.amazonaws.services.dynamodbv2.model.ParameterizedStatement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.RequestLimitExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import domain.GreenActivitySummary;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class ReadSummaryLambda {

    static AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();

    public static void main(String[] args) throws Exception {
        System.out.println(getGreenActivitySummary());
    }


    public APIGatewayProxyResponseEvent readSummary(final APIGatewayProxyRequestEvent input) throws JsonProcessingException {
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(getGreenActivitySummary().toString());
    }

    private static String getGreenActivitySummary() {
        GreenActivitySummary result = new GreenActivitySummary(0, 0, 0);

        try {
            ExecuteStatementRequest request = createRequest();
            ExecuteStatementResult statementResult = dynamoDB.executeStatement(request);

            result = extractSummary(statementResult);
        } catch (Exception e) {
            handleCommonErrors(e);
        }

        return result.toString();
    }

    private static GreenActivitySummary extractSummary(ExecuteStatementResult result) {
        int electronicBoardingPassRewards = 0;
        int checkedBagRewards = 0;
        for (Map<String, AttributeValue> item : result.getItems()) {
            int checkedBags = getCheckedBags(item.get("checkedBags").getN());
            if (checkedBags == 0) {
                checkedBagRewards += 10;
            }
            if (item.get("isElectronicBoardingPass").getBOOL()) {
                electronicBoardingPassRewards++;
            }
        }

        return new GreenActivitySummary(electronicBoardingPassRewards, checkedBagRewards, 0);
    }

    private static int getCheckedBags(String checkedBags) {
        return Integer.parseInt(checkedBags);
    }

    private static ExecuteStatementRequest createRequest() {
        ExecuteStatementRequest request = new ExecuteStatementRequest();
        DateTime dateTime = DateTime.now().withDayOfYear(1).withTimeAtStartOfDay();

        // Create statements
        request.setStatement("select  checkedBags, isElectronicBoardingPass  from GreenActivity where activityDate > ?");
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
}

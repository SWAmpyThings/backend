package service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.GreenActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class CreateActivityLambda {

    public APIGatewayProxyResponseEvent createActivity(final APIGatewayProxyRequestEvent input) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        GreenActivity greenActivity = objectMapper.readValue(input.getBody(), GreenActivity.class);

        DynamoDB dynamoDB = new DynamoDB((AmazonDynamoDBClientBuilder.defaultClient()));
        Table activityTable = dynamoDB.getTable("GreenActivity");
        Item item = new Item().withPrimaryKey("rapidRewardsNumber", greenActivity.getRapidRewardsNumber())
                .withString("recordLocator", greenActivity.getRecordLocator())
                .withString("bound", greenActivity.getBound())
                .withBoolean("isElectronicBoardingPass", greenActivity.getIsElectronicBoardingPass())
                .withInt("checkedBags", greenActivity.getCheckedBags())
                .withInt("greenIdeas", greenActivity.getGreenIdeas())
                .withInt("approvedGreenIdeas", greenActivity.getApprovedGreenIdeas())
                .withPrimaryKey("activityDate", greenActivity.getActivityDate());
        activityTable.putItem(item);

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "OPTIONS,POST,GET");

        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("RRId " + greenActivity.getRapidRewardsNumber()).withHeaders(headers);
    }
}

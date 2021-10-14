package service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.GreenActivityRedemptionEvent;

public class GreenActivityStoringLambda {
    private ObjectMapper objectMapper = new ObjectMapper();

    public void storeActivity(SNSEvent event) {
        event.getRecords().forEach(snsRecord -> {
            try {
                GreenActivityRedemptionEvent greenActivityRedemptionEvent = objectMapper.readValue(snsRecord.getSNS().getMessage(), GreenActivityRedemptionEvent.class);
                DynamoDB dynamoDB = new DynamoDB((AmazonDynamoDBClientBuilder.defaultClient()));
                Table activityRedemptionTable = dynamoDB.getTable("GreenActivityRedemptionHistory");
                Item item = new Item().withPrimaryKey("rapidRewardsNumber", greenActivityRedemptionEvent.getRapidRewardsNumber())
                        .withString("activityDate", greenActivityRedemptionEvent.getActivityDate())
                        .withInt("greenPoints", greenActivityRedemptionEvent.getGreenPoints());
                activityRedemptionTable.putItem(item);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }
}

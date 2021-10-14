package service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.ActivityDetailsResponse;
import domain.ActivitySummary;
import domain.GreenActivity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class ReadActivityLambda {

    public APIGatewayProxyResponseEvent getActivity(final APIGatewayProxyRequestEvent request) throws JsonProcessingException {
        Map<String, String> queryStringParameters = request.getQueryStringParameters();
        if (queryStringParameters == null) {
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("{}");
        }
        String rrNumber = queryStringParameters.get("rapidRewardsNumber");
        ObjectMapper objectMapper = new ObjectMapper();

        DynamoDB dynamoDB = new DynamoDB((AmazonDynamoDBClientBuilder.defaultClient()));
        Table activityTable = dynamoDB.getTable("GreenActivity");

        HashMap<String, Object> valueMap = new HashMap<>();
        valueMap.put(":rr", rrNumber);

        QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("rapidRewardsNumber = :rr")
                .withValueMap(valueMap);

        ItemCollection<QueryOutcome> items;
        Iterator<Item> iterator;
        Item item;
        List<GreenActivity> activities = new ArrayList<>();
        BigDecimal totalElectronicBoardingPass = BigDecimal.ZERO;
        BigDecimal totalGreenRewardPoints = BigDecimal.ZERO;

        try {
            items = activityTable.query(querySpec);

            iterator = items.iterator();
            while (iterator.hasNext()) {
                item = iterator.next();
                GreenActivity greenActivity = new GreenActivity(rrNumber, item.getString("recordLocator"), item.getString("bound"),
                        item.getBoolean("isElectronicBoardingPass"), item.getInt("checkedBags"), item.getString("activityDate"));
                if (greenActivity.getIsElectronicBoardingPass()) {
                    totalElectronicBoardingPass = totalElectronicBoardingPass.add(BigDecimal.ONE);
                }
                totalGreenRewardPoints = totalGreenRewardPoints.add(calculateGreenPoints(greenActivity));
                activities.add(greenActivity);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        ActivityDetailsResponse response = new ActivityDetailsResponse(activities, new ActivitySummary(totalElectronicBoardingPass, totalGreenRewardPoints));

        String json = objectMapper.writeValueAsString(response);

        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(json);
    }

    private BigDecimal calculateGreenPoints(GreenActivity greenActivity) {
        BigDecimal totalGreenPoints = BigDecimal.ZERO;
        if (greenActivity.getIsElectronicBoardingPass()) {
            totalGreenPoints = totalGreenPoints.add(BigDecimal.TEN);
        }
        Integer checkedBags = greenActivity.getCheckedBags();
        if (checkedBags == 0) {
            totalGreenPoints = totalGreenPoints.add(BigDecimal.valueOf(20));
        } else if (checkedBags == 1) {
            totalGreenPoints = totalGreenPoints.add(BigDecimal.TEN);
        }
        return totalGreenPoints;
    }
}

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
        BigDecimal totalGreenRewardPointsEarned = BigDecimal.ZERO;

        try {
            items = activityTable.query(querySpec);

            iterator = items.iterator();
            while (iterator.hasNext()) {
                item = iterator.next();
                GreenActivity greenActivity = new GreenActivity(rrNumber, item.getString("recordLocator"), item.getString("bound"),
                        item.getBoolean("isElectronicBoardingPass"), item.getInt("checkedBags"), item.getInt("greenIdeas"),
                        item.getInt("approvedGreenIdeas"), item.getString("activityDate"));
                if (greenActivity.getIsElectronicBoardingPass()) {
                    totalElectronicBoardingPass = totalElectronicBoardingPass.add(BigDecimal.ONE);
                }
                totalGreenRewardPointsEarned = totalGreenRewardPointsEarned.add(calculateGreenPoints(greenActivity));
                activities.add(greenActivity);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        Table redemptionHistoryTable = dynamoDB.getTable("GreenActivityRedemptionHistory");
        QuerySpec redemptionQuerySpec = new QuerySpec().withKeyConditionExpression("rapidRewardsNumber = :rr")
                .withValueMap(valueMap);
        BigDecimal pointsRedeemed = getPointsRedeemed(redemptionHistoryTable, redemptionQuerySpec);

        BigDecimal totalGreenRewardPointsAvailable = totalGreenRewardPointsEarned.subtract(pointsRedeemed);
        BigDecimal totalLifeTimePoints = totalGreenRewardPointsEarned.add(BigDecimal.valueOf(1542));

        ActivityDetailsResponse response = new ActivityDetailsResponse(activities, new ActivitySummary(totalGreenRewardPointsEarned, totalGreenRewardPointsAvailable, totalLifeTimePoints));

        String json = objectMapper.writeValueAsString(response);

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "OPTIONS,POST,GET");

        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(json).withHeaders(headers);
    }

    private BigDecimal getPointsRedeemed(Table redemptionHistoryTable, QuerySpec querySpec) {
        ItemCollection<QueryOutcome> items;
        Iterator<Item> iterator;
        Item item;
        BigDecimal totalPointsRedeemed = BigDecimal.ZERO;

        try {
            items = redemptionHistoryTable.query(querySpec);

            iterator = items.iterator();
            while (iterator.hasNext()) {
                item = iterator.next();
                int greenPoints = item.getInt("greenPoints");
                totalPointsRedeemed = totalPointsRedeemed.add(BigDecimal.valueOf(greenPoints));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return totalPointsRedeemed;
    }

    private BigDecimal calculateGreenPoints(GreenActivity greenActivity) {
        BigDecimal totalGreenPoints = BigDecimal.ZERO;
        if (greenActivity.getIsElectronicBoardingPass()) {
            totalGreenPoints = totalGreenPoints.add(BigDecimal.ONE);
        }
        Integer checkedBags = greenActivity.getCheckedBags();
        if (checkedBags == 0) {
            totalGreenPoints = totalGreenPoints.add(BigDecimal.TEN);
        }
        totalGreenPoints = totalGreenPoints.add(BigDecimal.valueOf(greenActivity.getGreenIdeas() * 2));
        totalGreenPoints = totalGreenPoints.add(BigDecimal.valueOf(greenActivity.getApprovedGreenIdeas() * 50));
        return totalGreenPoints;
    }
}

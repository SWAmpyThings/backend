package service;


import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.GreenActivityRedemptionEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GreenActivityRedemptionLambda {

    private AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
    private ObjectMapper objectMapper = new ObjectMapper();
    private AmazonSNS sns = AmazonSNSClientBuilder.defaultClient();

    public void readActivity(S3Event event) {
        event.getRecords().forEach(record -> {
            S3ObjectInputStream s3InputStream = s3.getObject(record.getS3().getBucket().getName(), record.getS3().getObject().getKey())
                    .getObjectContent();
            try {
                List<GreenActivityRedemptionEvent> greenActivityRedemptionEvents = Arrays.asList(objectMapper.readValue(s3InputStream, GreenActivityRedemptionEvent[].class));
                System.out.println(greenActivityRedemptionEvents);
                greenActivityRedemptionEvents.forEach(redemptionEvent-> {
                    try {
                        sns.publish(System.getenv("GREEN_ACTIVITY_REDEMPTION_TOPIC"), objectMapper.writeValueAsString(redemptionEvent));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

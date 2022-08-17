package pt.ulisboa.tecnico.cnv.lbas.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import pt.ulisboa.tecnico.cnv.lbas.InstanceManager;

public class DynamoDBAdapter {

    private static DynamoDBAdapter instance = null;
    private DynamoDBMapper dynamoDBMapper;
    Map<String, AttributeValue> eav;
    Map<String, String> attributeNames;

    private DynamoDBAdapter() {
        initializeDynamoDB();
    }

    public static DynamoDBAdapter getInstance() {
        if (instance == null) {
            instance = new DynamoDBAdapter();
        }
        return instance;
    }

    public List<CnvProjectMetrics> fetchData() {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (10 * 1000); /* Sleep time */
        eav.put(":val1", new AttributeValue().withN(String.valueOf(startTime)));
        eav.put(":val2", new AttributeValue().withN(String.valueOf(endTime)));
        DynamoDBScanExpression scanRequest = new DynamoDBScanExpression().withFilterExpression("#timeStamp > :val1 and #timeStamp < :val2")
                .withExpressionAttributeNames(attributeNames)
                .withExpressionAttributeValues(eav);
        List<CnvProjectMetrics> metrics = dynamoDBMapper.scan(CnvProjectMetrics.class, scanRequest);
        return metrics;
    }

    public void initializeDynamoDB() {
        AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(InstanceManager.AWS_REGION).build();
        dynamoDBMapper = new DynamoDBMapper(dynamoDB);
        eav = new HashMap<>();
        attributeNames = new HashMap<String, String>();
        attributeNames.put("#timeStamp", "timeStamp");
    }
}

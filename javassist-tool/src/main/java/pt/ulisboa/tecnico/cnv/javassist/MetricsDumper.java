package pt.ulisboa.tecnico.cnv.javassist;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;

public class MetricsDumper implements Runnable {

    private static final int TIME_TO_SLEEP = 1000 * 10;
    private static final int BATCH_LIMIT = 25;

    private static final String AWS_REGION = "us-east-1"; // System.getenv("AWS_REGION");
    private static final String TABLE_NAME = "";
    private AmazonDynamoDB dynamoDB;
    private DynamoDBMapper dynamoDBMapper;

    @Override
    public void run() {
        initializeDynamoDB();
        while (true) {
            try {
                RequestMetricsRecord rec = null;
                int counter = 0;
                List<CnvProjectMetrics> items = new LinkedList<>();
                while ((rec = MetricsProvider.metricsQueue.poll()) != null && counter < BATCH_LIMIT) {
                    ++counter;
                    System.out.println(String.format("[%s] Number of executed methods: %s", MetricsProvider.class.getSimpleName(), rec.getNMethods()));
                    System.out.println(String.format("[%s] Number of executed basic blocks: %s", MetricsProvider.class.getSimpleName(), rec.getNBlocks()));
                    System.out.println(String.format("[%s] Number of executed instructions: %s", MetricsProvider.class.getSimpleName(), rec.getNInsts()));
                    System.out.println(String.format("[%s] Image size: %s", MetricsProvider.class.getSimpleName(), rec.getImageSize()));
                    System.out.println(String.format("[%s] Request class: %s", MetricsProvider.class.getSimpleName(), rec.getRequestClass()));
                    items.add(newItem(rec.getNMethods(), rec.getNBlocks(), rec.getNInsts(), rec.getImageSize(), rec.getRequestClass()));
                }
                List<FailedBatch> failed = dynamoDBMapper.batchSave(items);
                for (FailedBatch fb : failed) {
                    System.out.println(fb.toString());
                    System.out.println(fb.getException());
                    fb.getException().printStackTrace();
                    for (List<WriteRequest> wrl : fb.getUnprocessedItems().values()) {
                        for (WriteRequest wr : wrl) {
                            System.out.println(wr.toString());
                        }
                    }
                }
                Thread.sleep(TIME_TO_SLEEP);
            } catch (InterruptedException ie) {
                System.out.println(ie);
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println(e);
            }
        }
    }

    private static CnvProjectMetrics newItem(long nmethods, long nblocks, long ninsts, int imageSize,
            String requestClass) {
        CnvProjectMetrics item = new CnvProjectMetrics();
        item.id = UUID.randomUUID().toString();
        item.nmethods = nmethods;
        item.nblocks = nblocks;
        item.ninsts = ninsts;
        item.imageSize = imageSize;
        item.requestClass = requestClass;
        item.timeStamp = System.currentTimeMillis();
        return item;
    }

    private void initializeDynamoDB() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials("", "");
        dynamoDB = AmazonDynamoDBClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(AWS_REGION).build();
        dynamoDBMapper = new DynamoDBMapper(dynamoDB);
    }

    @DynamoDBTable(tableName = TABLE_NAME)
    @SuppressWarnings("unused")
    public static class CnvProjectMetrics {
        private long nmethods;
        private long nblocks;
        private long ninsts;
        private int imageSize;
        private String requestClass;
        private String id;
        private long timeStamp;

        @DynamoDBHashKey(attributeName = "record-id")
        public String getId() {
            return id;
        }

        @DynamoDBAttribute(attributeName = "nmethods")
        public long getNMethods() {
            return nmethods;
        }

        @DynamoDBAttribute(attributeName = "nblocks")
        public long getNBlocks() {
            return nblocks;
        }

        @DynamoDBAttribute(attributeName = "ninsts")
        public long getNInsts() {
            return ninsts;
        }

        @DynamoDBAttribute(attributeName = "imageSize")
        public long getImageSize() {
            return imageSize;
        }

        @DynamoDBAttribute(attributeName = "requestClass")
        public String getRequestClass() {
            return requestClass;
        }

        @DynamoDBAttribute(attributeName = "timeStamp")
        public long getTimeStamp() {
            return timeStamp;
        }
    }
}

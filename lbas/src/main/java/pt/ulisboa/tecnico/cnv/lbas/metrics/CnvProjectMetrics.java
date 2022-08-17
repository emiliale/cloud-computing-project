package pt.ulisboa.tecnico.cnv.lbas.metrics;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "cnv-project-metrics")
@SuppressWarnings("unused")
public class CnvProjectMetrics {
    private long nmethods;
    private long nblocks;
    private long ninsts;
    private int imageSize;
    private String requestClass;
    private String id;
    private long timeStamp;

    public CnvProjectMetrics() {
        nmethods = 0;
        nblocks = 0;
        ninsts = 0;
        imageSize = 0;
        requestClass = "";
        id = "";
    }

    public CnvProjectMetrics(long nmethods, long nblocks, long ninsts, int imageSize, String requestClass) {
        this.nmethods = nmethods;
        this.nblocks = nblocks;
        this.ninsts = ninsts;
        this.imageSize = imageSize;
        this.requestClass = requestClass;
    }

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

    public void setId(String id) {
        this.id = id;
    }

    public void setNMethods(long nmethods) {
        this.nmethods = nmethods;
    }

    public void setNBlocks(long nblocks) {
        this.nblocks = nblocks;
    }

    public void setNInsts(long ninsts) {
        this.ninsts = ninsts;
    }

    public void setImageSize(long imageSize) {
        this.imageSize = (int) imageSize;
    }

    public void setRequestClass(String requestClass) {
        this.requestClass = requestClass;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public String toString() {
        return "{ id: " + id + ", request class: " + requestClass + " }";
    }
}

package pt.ulisboa.tecnico.cnv.lbas.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pt.ulisboa.tecnico.cnv.lbas.metrics.CnvProjectMetrics;

public class HealthzResponseParser {

    public static JsonNode getData(String json) throws IOException {
        return getJsonAsJsonNode(json);
    }

    public static List<CnvProjectMetrics> getCurrentMetrics(JsonNode responseObject) {
        List<CnvProjectMetrics> metrics = new LinkedList<>();
        ArrayNode requestsList = (ArrayNode) responseObject.get("requests");
        for (JsonNode requestInfo : requestsList) {
            ObjectNode request = (ObjectNode) requestInfo;
            CnvProjectMetrics obj = new CnvProjectMetrics(request.get("nmethods").asLong(),
                    request.get("nblocks").asLong(), request.get("ninsts").asLong(), request.get("imageSize").asInt(),
                    request.get("requestClass").asText());
            metrics.add(obj);
        }
        return metrics;
    }

    private static JsonNode getJsonAsJsonNode(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(json);
            return actualObj;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't parse json:" + json, e);
        }
    }

    public static double getCPU(JsonNode responseObject) {
        return responseObject.get("cpu").asDouble();
    }
}

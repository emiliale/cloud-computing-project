package pt.ulisboa.tecnico.cnv.lbas;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

public class LambdaFunction {

    private final LambdaClient awsLambda;

    public LambdaFunction() {
        awsLambda = LambdaClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build();
    }

    public String invokeFunction(String functionName, String format, String image) throws IOException, LambdaException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("fileFormat", format);
        rootNode.put("body", image);
        String jsonPayload = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        SdkBytes payload = SdkBytes.fromUtf8String(jsonPayload);
        InvokeRequest request = InvokeRequest.builder().functionName(functionName).payload(payload).build();
        InvokeResponse res = awsLambda.invoke(request);
        return res.payload().asUtf8String();
    }

    public void close() {
        awsLambda.close();
    }
}

package pt.ulisboa.tecnico.cnv.lbas;

import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pt.ulisboa.tecnico.cnv.lbas.metrics.RequestComplexityCalculator;
import software.amazon.awssdk.services.lambda.model.LambdaException;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class LoadBalancer implements HttpHandler {

    private static final InstanceManager manager = InstanceManager.getInstance();
    private static final double LAMBDA_THRESHOLD = 0.3;
    static final String INSTANCE_PROTOCOL = "http://";
    static final String INSTANCE_PORT = ":8000";
    private static final Map<String, String> endpointMapper = Map.of(
            "/blurimage", "BlurImageHandler",
            "/enhanceimage", "EnhanceImageHandler",
            "/detectqrcode", "DetectQrCodeHandler",
            "/classifyimage", "ImageClassificationHandler");

    @Override
    public void handle(HttpExchange t) throws IOException {
        /* forward request */
        if (t.getRequestHeaders().getFirst("Origin") != null) {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", t.getRequestHeaders().getFirst("Origin"));
        }
        if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "POST");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,API-Key");
            t.sendResponseHeaders(204, -1);
        }
        if (t.getRequestMethod().equalsIgnoreCase("GET")) {
            t.sendResponseHeaders(200, 0);
        } else {
            InputStream requestStream = t.getRequestBody();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            requestStream.transferTo(baos);
            /* We need to clone the stream because it can be consumed in two places */
            InputStream requestStreamClone = new ByteArrayInputStream(baos.toByteArray());
            InputStream secondClone = new ByteArrayInputStream(baos.toByteArray());
            String[] imageData = getFormatAndImage(requestStreamClone);
            BufferedImage bi = getBufferedImage(imageData[1]);
            long imageSize = bi.getHeight() * bi.getWidth();
            double estimatedWeight = RequestComplexityCalculator.estimateWeight(imageSize,
                    endpointMapper.get(t.getRequestURI().toString()));

            if (estimatedWeight < LAMBDA_THRESHOLD && manager.isPendingInstance()) {
                System.out.println("*** Using lambdas for weight " + estimatedWeight);
                useLambda(t, imageData[0], imageData[1]);
            } else {
                System.out.println("--- Using EC2 for weight " + estimatedWeight);
                useEC2(t, secondClone, estimatedWeight);
            }
        }
    }

    private void useLambda(HttpExchange t, String format, String image) throws IOException {
        LambdaFunction lambda = new LambdaFunction();

        String functionName = t.getRequestURI().toString().split("/")[1];
        try {
            String responseBody = lambda.invokeFunction(functionName, format, image);
            responseBody = responseBody.substring(1, responseBody.length() - 1);
            t.sendResponseHeaders(200, responseBody.length());
            OutputStream os = t.getResponseBody();
            os.write(responseBody.getBytes());
            os.close();
        } catch (LambdaException e) {
            e.printStackTrace();
            System.out.println(e);
            throw new RuntimeException(e);
        } catch (Throwable th) {
            th.printStackTrace();
            System.out.println(th);
        } finally {
            lambda.close();
        }
    }

    private void useEC2(HttpExchange t, InputStream stream, double estimatedWeight) throws IOException {

        Instance instance = manager.getNextInstance(estimatedWeight);

        if (instance == null) {
            throw new IOException("No running instances detected.");
        }
        System.out.println("instance: " + instance.getPublicIpAddress());

        /* register the instance to the request */
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        String address = INSTANCE_PROTOCOL + instance.getPublicDnsName() + INSTANCE_PORT + t.getRequestURI().toString();
        HttpRequest request = HttpRequest.newBuilder(URI.create(address))
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> stream))
                .header("accept", "text/plain").build();

        manager.addHttpRequestToInstance(instance.getInstanceId(), request, estimatedWeight);
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            t.sendResponseHeaders(200, response.body().length());
            OutputStream os = t.getResponseBody();
            os.write(response.body().getBytes());
            os.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Throwable th) {
            th.printStackTrace();
            System.out.println(th);
        } finally {
            /* when request is completed, remove the request record */
            manager.removeHttpRequestFromInstance(instance.getInstanceId(), request);
        }
    }

    private String[] getFormatAndImage(InputStream stream) {
        /* Result syntax: data:image/<format>;base64,<encoded image> */
        String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
        String[] resultSplits = result.split(",");
        String format = resultSplits[0].split("/")[1].split(";")[0];
        return new String[] { format, resultSplits[1] };
    }

    private BufferedImage getBufferedImage(String image) throws IOException {
        byte[] decoded = Base64.getDecoder().decode(image);
        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
        return ImageIO.read(bais);
    }
}

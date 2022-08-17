package pt.ulisboa.tecnico.cnv.imageproc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import javax.management.*;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;

public class HealthzHandler implements HttpHandler {

    private void handleRequest(ObjectMapper mapper, ObjectNode rootNode) {
        double value = 0;
        try {
            value = (Double) ManagementFactory.getPlatformMBeanServer()
                    .getAttribute(new ObjectName("java.lang:type=OperatingSystem"), "CpuLoad");
        } catch (MBeanException | AttributeNotFoundException | MalformedObjectNameException | InstanceNotFoundException
                | ReflectionException e) {
            e.printStackTrace();
        }
        if (Double.isNaN(value)) {
            rootNode.put("cpu", "0");
        } else {
            rootNode.put("cpu", String.valueOf(value * 100));
        }
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        handleRequest(mapper, rootNode);

        String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        byte[] bytearray = jsonString.getBytes();

        t.sendResponseHeaders(200, bytearray.length);
        OutputStream os = t.getResponseBody();
        os.write(bytearray);
        os.close();
    }
}

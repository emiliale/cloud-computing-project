package pt.ulisboa.tecnico.cnv.javassist;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javassist.CannotCompileException;
import javassist.CtBehavior;

public class MetricsProvider extends CodeDumper {

    private static Map<Thread, RequestMetricsRecord> metrics = new ConcurrentHashMap<>();
    static Queue<RequestMetricsRecord> metricsQueue = new ConcurrentLinkedQueue<RequestMetricsRecord>();

    public MetricsProvider(List<String> packageNameList) {
        super(packageNameList);
    }

    public static void incBasicBlock(int position, int length) {
        try {
            if (metrics.get(Thread.currentThread()) == null) {
                initializeMetricsRecord();
            }
            metrics.get(Thread.currentThread()).incrementNBlocks();
            metrics.get(Thread.currentThread()).addNInstructions(length);
        } catch (Throwable t) {
            System.out.println(t);
        }
    }

    public static void incBehavior(String name) {
        try {
            if (metrics.get(Thread.currentThread()) == null) {
                initializeMetricsRecord();
            }
            metrics.get(Thread.currentThread()).incrementNMethods();
        } catch (Throwable t) {
            System.out.println(t);
        }
    }

    public static void dumpStatistics() {
        if (metrics.get(Thread.currentThread()) != null) {
            metricsQueue.add(metrics.get(Thread.currentThread()));
            /* To reset request-specific metrics after finishing request */
            initializeMetricsRecord();
        }
    }

    public static void initializeMetricsRecord() {
        metrics.put(Thread.currentThread(), new RequestMetricsRecord());
    }

    public static void setRequestInformation(BufferedImage bi, Class<?> requestClass) {
        try {
            if (metrics.get(Thread.currentThread()) == null) {
                initializeMetricsRecord();
            }
            metrics.get(Thread.currentThread()).setImage(bi);
            metrics.get(Thread.currentThread()).setRequestClass(requestClass.getSimpleName());
        } catch (Throwable t) {
            System.out.println(t);
        }
    }

    public static void fillMetrics(ObjectMapper mapper, ObjectNode rootNode) {
        ArrayNode requestsArray = mapper.createArrayNode();
        for (RequestMetricsRecord entry : metrics.values()) {
            if (entry.getImageSize() != 0) {
                ObjectNode requestObject = mapper.createObjectNode();
                requestObject.put("ninsts", entry.getNInsts());
                requestObject.put("nblocks", entry.getNBlocks());
                requestObject.put("nmethods", entry.getNMethods());
                requestObject.put("imageSize", entry.getImageSize());
                requestObject.put("requestClass", entry.getRequestClass());
                requestsArray.add(requestObject);
            }
        }
        rootNode.set("requests", requestsArray);
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        /* Health handler requires specific instrumentation */
        if (behavior.getDeclaringClass().getName().equals("pt.ulisboa.tecnico.cnv.imageproc.HealthzHandler")) {
            if (behavior.getName().equals("handleRequest")) {
                behavior.insertBefore(String.format("%s.fillMetrics($1, $2);", MetricsProvider.class.getName()));
            }
            return;
        }
        super.transform(behavior);
        /* Do not take into consideration constructors or main methods */
        if (!behavior.getName().equals("main") && !behavior.getName().equals(behavior.getDeclaringClass().getSimpleName())) {
            behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", MetricsProvider.class.getName(), behavior.getLongName()));
        }

        if (behavior.getName().equals("process")) {
            /* This insert relies on a name of the local variable */
            behavior.insertBefore(String.format("%s.setRequestInformation($1, $0.getClass());", MetricsProvider.class.getName()));
            behavior.insertAfter(String.format("%s.dumpStatistics();", MetricsProvider.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        /* Do not take into consideration constructors or main methods */
        if (!block.getBehavior().getName().equals("main") && !block.getBehavior().getName().equals(block.getBehavior().getDeclaringClass().getSimpleName())) {
            block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", MetricsProvider.class.getName(), block.getPosition(), block.getLength()));
        }
    }

}

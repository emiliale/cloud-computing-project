package pt.ulisboa.tecnico.cnv.lbas;

import java.util.HashMap;
import java.util.Map;

public class InstanceMetrics {
    private final Map<String, Map<String, Double>> metrics;

    public InstanceMetrics() {
        this.metrics = new HashMap<>();
    }

    public void putMetric(String instanceId, String metric, Double value) {
        Map<String, Double> instanceMetric = metrics.get(instanceId);
        if (instanceMetric != null) {
            instanceMetric.put(metric, value);
        } else {
            metrics.put(instanceId, Map.of(metric, value));
        }
    }

    public Map<String, Double> getInstanceMetrics(String instanceId) {
        return metrics.get(instanceId);
    }

    public Map<String, Metric> getInstanceCPUBoundaries() {
        Metric minCPU = new Metric("", Integer.MAX_VALUE);
        Metric maxCPU = new Metric("", Integer.MIN_VALUE);


        for (Map.Entry<String, Map<String, Double>> entry : metrics.entrySet()) {
            double cpuValue = entry.getValue().get("cpu");
            if (cpuValue < minCPU.getValue()) {
                minCPU.setInstanceId(entry.getKey());
                minCPU.setValue(cpuValue);
            }

            if (cpuValue > maxCPU.getValue()) {
                maxCPU.setInstanceId(entry.getKey());
                maxCPU.setValue(cpuValue);
            }
        }

        return Map.of("min", minCPU, "max", maxCPU);
    }
}

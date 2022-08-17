package pt.ulisboa.tecnico.cnv.lbas;

import com.amazonaws.services.ec2.model.Instance;

import java.util.List;
import java.util.Map;

public class AutoScaler implements Runnable {

    private static final int TIME_TO_SLEEP = 1000 * 2;

    private static final int THRESHOLD_START = 65;
    private static final int THRESHOLD_TERMINATE = 10;

    @Override
    public void run() {
        InstanceManager manager = InstanceManager.getInstance();
        List<Instance> instances = manager.getActiveInstances();
        if (instances.isEmpty()) {
            manager.createInstances(1, 1);
        }
        while (true) {
            try {
                instances = manager.getActiveInstances();
                InstanceMetrics instanceMetrics = new InstanceMetrics();
                for (Instance inst : instances) {
                    double cpu = manager.measureCPU(inst);
                    instanceMetrics.putMetric(inst.getInstanceId(), "cpu", cpu);
                }
                scale(manager, instanceMetrics);
                Thread.sleep(TIME_TO_SLEEP);
            } catch (InterruptedException ie) {
                System.out.println(ie);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
            }
        }
    }

    private void scale(InstanceManager manager, InstanceMetrics instanceMetrics) {
        Map<String, Metric> getInstanceCPUBoundaries = instanceMetrics.getInstanceCPUBoundaries();

        // knowing that there is only 1 min
        Metric minMetric = getInstanceCPUBoundaries.get("min");
        // knowing that there is only 1 max
        Metric maxMetric = getInstanceCPUBoundaries.get("max");

        if (minMetric.getValue() > THRESHOLD_START) {
            manager.createInstances(1, 1);
            manager.refreshInstances();
        }
        if (maxMetric.getValue() < THRESHOLD_TERMINATE && manager.getRunningInstances().size() > 1) {
            // we need to set instance as marked for termination
            manager.setInstanceToTerminate(minMetric.getInstanceId());
            // if it is already marked, we need to check whether it has any new requests
            if (manager.canRemoveInstance(minMetric.getInstanceId())) {
                // if above things pass, we remove that instance
                manager.terminateInstance(minMetric.getInstanceId());
                manager.removeInstanceRecord(minMetric.getInstanceId());
            }

        }
    }

}

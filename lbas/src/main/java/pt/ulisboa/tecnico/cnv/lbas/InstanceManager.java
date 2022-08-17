package pt.ulisboa.tecnico.cnv.lbas;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.fasterxml.jackson.databind.JsonNode;

import pt.ulisboa.tecnico.cnv.lbas.metrics.CnvProjectMetrics;
import pt.ulisboa.tecnico.cnv.lbas.metrics.InputRecord;
import pt.ulisboa.tecnico.cnv.lbas.metrics.MetricsCalculator;
import pt.ulisboa.tecnico.cnv.lbas.util.HealthzResponseParser;
import pt.ulisboa.tecnico.cnv.lbas.util.Pair;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.cnv.lbas.LoadBalancer.INSTANCE_PORT;
import static pt.ulisboa.tecnico.cnv.lbas.LoadBalancer.INSTANCE_PROTOCOL;

public class InstanceManager {

    public static final String AWS_REGION = System.getenv("AWS_REGION");
    private static final String AMI_ID = System.getenv("AMI_ID");
    private static final String KEY_NAME = System.getenv("KEY_NAME");
    private static final String SEC_GROUP_ID = System.getenv("SEC_GROUP_ID");

    private static final int PENDING = 0;
    private static final int RUNNING = 16;

    private static InstanceManager instance = null;

    private final AmazonEC2 ec2;
    private final DescribeAvailabilityZonesResult availabilityZonesResult;

    private final Map<String, InstanceRecord> requests;

    public static Map<String, List<CnvProjectMetrics>> currentRequests;
    public static Map<String, Double> currentCpu;

    private InstanceManager() {
        ec2 = AmazonEC2ClientBuilder.standard().withRegion(AWS_REGION).withCredentials(new EnvironmentVariableCredentialsProvider()).build();
        availabilityZonesResult = ec2.describeAvailabilityZones();
        requests = new ConcurrentHashMap<>();
        currentRequests = new ConcurrentHashMap<>();
        currentCpu = new ConcurrentHashMap<>();
    }

    public void addHttpRequestToInstance(String instanceId, HttpRequest request, double weight) {
        InstanceRecord record = this.requests.get(instanceId);
        if (record == null) {
            this.requests.put(instanceId, new InstanceRecord(request, weight));
        } else {
            record.addInstanceRequest(request, weight);
        }
    }

    public void removeHttpRequestFromInstance(String instanceId, HttpRequest request) {
        InstanceRecord record = this.requests.get(instanceId);
        if (record != null) {
            record.removeRequest(request);
        }
    }

    public double getEstimatedLoad(String id) {
        if (requests.get(id) == null) {
            return 0;
        }
        double load = 0;
        List<CnvProjectMetrics> requestsData = currentRequests.get(id);
        for (CnvProjectMetrics metricsData : requestsData) {
            double requestWeight = MetricsCalculator.calculateWeight(metricsData);
            InputRecord input = new InputRecord(metricsData.getImageSize(), metricsData.getRequestClass());
            /* Base of knowledge weights */
            Map<InputRecord, Double> basedWeights = MetricsCalculator.getWeights();
            Double basedWeight = basedWeights.get(input);
            if (basedWeight != null) {
                double distance = requestWeight / basedWeight;
                if (distance <= 0.8) {
                    /* Less than 80% progress - consider this weight */
                    load += basedWeight;
                }
            } else {
                /* Fallback to conservative estimation based on estimated weights for requests */
                load = 0;
                InstanceRecord record = requests.get(id);
                if (record != null) {
                    for (double estimatedWeight : record.getInstanceRequests().values()) {
                        load += estimatedWeight;
                    }
                }
                return load;
            }
        }
        return load;
    }

    public void setInstanceToTerminate(String instanceId) {
        InstanceRecord ir = requests.get(instanceId);
        if (ir != null) {
            ir.setToTerminate(true);
        } else {
            requests.put(instanceId, new InstanceRecord());
        }
    }

    public void refreshInstances() {
        List<Instance> instances = getActiveInstances();
        for (Instance inst : instances) {
            if (requests.get(inst.getInstanceId()) == null) {
                requests.put(inst.getInstanceId(), new InstanceRecord());
            }
        }
    }

    public void removeInstanceRecord(String instanceId) {
        requests.remove(instanceId);
    }

    private boolean instanceHasActiveRequests(String instanceId) {
        return !requests.get(instanceId).getInstanceRequests().isEmpty();
    }

    public boolean canRemoveInstance(String instanceId) {
        return !instanceHasActiveRequests(instanceId) && requests.get(instanceId).isToTerminate();
    }

    public List<Instance> getRunningInstances() {
        List<Instance> instances = new ArrayList<>();
        for (Reservation reservation : this.ec2.describeInstances().getReservations()) {
            instances.addAll(reservation.getInstances().stream().filter((inst) -> inst.getImageId().equals(AMI_ID) && inst.getState().getCode() == RUNNING).collect(Collectors.toList()));
        }
        return instances;
    }

    public boolean isPendingInstance() {
        for (Reservation reservation : this.ec2.describeInstances().getReservations()) {
            for (Instance i : reservation.getInstances()) {
                if(i.getImageId().equals(AMI_ID) && i.getState().getCode() == PENDING) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Instance> getAllInstances() {
        List<Instance> instances = new ArrayList<>();
        for (Reservation reservation : this.ec2.describeInstances().getReservations()) {
            instances.addAll(reservation.getInstances().stream().filter(inst -> inst.getImageId().equals(AMI_ID)).collect(Collectors.toList()));
        }
        return instances;
    }

    public List<Instance> getActiveInstances() {
        List<Instance> instances = new ArrayList<>();
        for (Reservation reservation : this.ec2.describeInstances().getReservations()) {
            instances.addAll(reservation.getInstances().stream().filter((inst) -> inst.getImageId().equals(AMI_ID) && (inst.getState().getCode() == RUNNING || inst.getState().getCode() == PENDING)).collect(Collectors.toList()));
        }
        return instances;
    }

    public static InstanceManager getInstance() {
        if (instance == null) {
            instance = new InstanceManager();
        }
        return instance;
    }

    public List<AvailabilityZone> getAvailableZones() {
        if (instance == null) {
            return null;
        }
        return this.availabilityZonesResult.getAvailabilityZones();
    }

    public Instance getNextInstance(double estimatedWeight) {
        List<Instance> ec2Instances = getRunningInstances();
        List<Pair<Instance, Double>> instances = new LinkedList<>();
        for (Instance i : ec2Instances) {
            Double cpu = currentCpu.get(i.getInstanceId());
            cpu = cpu == null ? 0 : cpu;
            if (cpu <= 85.0) {
                double load = getEstimatedLoad(i.getInstanceId());
                instances.add(new Pair<>(i, load));
            }
        }
        instances.sort(new InstancesByLoadReversedComparator());
        Instance target = null;
        for (Pair<Instance, Double> p : instances) {
            /* If estimated weight fits in instance */
            if ((1 - p.getValue()) >= estimatedWeight) {
                target = p.getKey();
                break;
            }
        }
        /* If there are no instances that could fit the request,
           return the first instance from the list as default */
        if (target == null) {
            target = ec2Instances.get(0);
        }
        return target;
    }

    public void createInstances(int min, int max) {
        try {
            RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
            runInstancesRequest.withImageId(AMI_ID)
                    .withInstanceType("t2.micro")
                    .withMinCount(min)
                    .withMaxCount(max)
                    .withKeyName(KEY_NAME)
                    .withSecurityGroupIds(SEC_GROUP_ID)
                    .withMonitoring(true);
            ec2.runInstances(runInstancesRequest);

            System.out.println("You have " + getActiveInstances().size() + " Amazon EC2 instance(s) running.");
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public void terminateInstance(String instanceID) {
        try {
            System.out.println("Terminating the instance.");
            TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
            termInstanceReq.withInstanceIds(instanceID);
            ec2.terminateInstances(termInstanceReq);

            System.out.println("You have " + getActiveInstances().size() + " Amazon EC2 instance(s) running.");
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public double measureCPU(Instance instance) {
        if (instance.getState().getCode() != RUNNING) {
            return 0;
        }
        double value = 0;
        HttpClient client = HttpClient.newHttpClient();
        String address = INSTANCE_PROTOCOL + instance.getPublicDnsName() + INSTANCE_PORT + "/healthz";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(address))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseJson = response.body();
            JsonNode responseObject = HealthzResponseParser.getData(responseJson);
            value = HealthzResponseParser.getCPU(responseObject);
            /* Updating info about requests that are currently executed by this instance */
            currentRequests.put(instance.getInstanceId(), HealthzResponseParser.getCurrentMetrics(responseObject));
            currentCpu.put(instance.getInstanceId(), value);
        } catch (HttpTimeoutException | ConnectException e) {
            InstanceRecord rec = requests.get(instance.getInstanceId());
            if (rec != null) {
                if (rec.isToTerminate()) {
                    removeInstanceRecord(instance.getInstanceId());
                    System.out.println("removed unresponsive instance: " + instance.getPublicIpAddress());
                } else {
                    rec.setToTerminate(true);
                    System.out.println("set to remove unresponsive instance: " + instance.getPublicIpAddress());
                }
            }
        } catch (IOException | InterruptedException e) {
            return 0;
        }
        return value;
    }

    private static class InstancesByLoadReversedComparator implements Comparator<Pair<Instance, Double>> {
        @Override
        public int compare(Pair<Instance, Double> x, Pair<Instance, Double> y) {
            return y.getValue().compareTo(x.getValue());
        }
    }
}

package pt.ulisboa.tecnico.cnv.lbas;

import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

public class InstanceRecord {

    private final Map<HttpRequest, Double> instanceRequests;
    private boolean toTerminate;

    public InstanceRecord(HttpRequest firstRequest, double weight) {
        this.instanceRequests = new HashMap<>();
        this.instanceRequests.put(firstRequest, weight);
        this.toTerminate = false;
    }

    public InstanceRecord() {
        this.instanceRequests = new HashMap<>();
        this.toTerminate = false;
    }

    public Map<HttpRequest, Double> getInstanceRequests() {
        return instanceRequests;
    }

    public void removeRequest(HttpRequest request) {
        this.instanceRequests.remove(request);
    }

    public void addInstanceRequest(HttpRequest request, double weight) {
        this.instanceRequests.put(request, weight);
    }


    public boolean isToTerminate() {
        return toTerminate;
    }

    public void setToTerminate(boolean toTerminate) {
        this.toTerminate = toTerminate;
    }
}

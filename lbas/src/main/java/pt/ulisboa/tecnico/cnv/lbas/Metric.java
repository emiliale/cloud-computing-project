package pt.ulisboa.tecnico.cnv.lbas;

public class Metric {

    private String instanceId;
    private double value;

    public Metric(String instanceId, double value) {
        this.instanceId = instanceId;
        this.value = value;
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }


    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}

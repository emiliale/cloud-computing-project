package pt.ulisboa.tecnico.cnv.javassist;

import java.awt.image.BufferedImage;

public class RequestMetricsRecord {

    private long nblocks;

    private long nmethods;

    private long ninsts;

    private BufferedImage bi;

    private String requestClass;

    public RequestMetricsRecord() {
        this(0, 0, 0, null, "");
    }

    public RequestMetricsRecord(long nblocks, long nmethods, long ninsts, BufferedImage bi, String requestClass) {
        this.nblocks = nblocks;
        this.nmethods = nmethods;
        this.ninsts = ninsts;
        this.bi = bi;
        this.requestClass = requestClass;
    }

    public long getNBlocks() {
        return nblocks;
    }

    public long getNMethods() {
        return nmethods;
    }

    public long getNInsts() {
        return ninsts;
    }

    public String getRequestClass() {
        return requestClass;
    }

    public int getImageSize() {
        if (bi != null) {
            return bi.getHeight() * bi.getWidth();
        }
        return 0;
    }

    public void incrementNBlocks() {
        this.nblocks++;
    }

    public void incrementNMethods() {
        this.nmethods++;
    }

    public void addNInstructions(int insts) {
        this.ninsts += insts;
    }

    public void setImage(BufferedImage bi) {
        this.bi = bi;
    }

    public void setRequestClass(String requestClass) {
        this.requestClass = requestClass;
    }
}

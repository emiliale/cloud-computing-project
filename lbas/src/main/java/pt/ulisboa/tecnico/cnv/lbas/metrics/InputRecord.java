package pt.ulisboa.tecnico.cnv.lbas.metrics;

public class InputRecord {

    private final long imageSize;
    private final String requestClass;

    public InputRecord(long imageSize, String requestClass) {
        this.imageSize = imageSize;
        this.requestClass = requestClass;
    }

    public long getImageSize() {
        return imageSize;
    }

    public String getRequestClass() {
        return requestClass;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (int) (prime * result + imageSize);
        result = prime * result + ((requestClass == null) ? 0 : requestClass.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InputRecord other = (InputRecord) obj;
        if (imageSize != other.imageSize)
            return false;
        if (requestClass == null) {
            return other.requestClass == null;
        } else {
            return requestClass.equals(other.requestClass);
        }
    }

}

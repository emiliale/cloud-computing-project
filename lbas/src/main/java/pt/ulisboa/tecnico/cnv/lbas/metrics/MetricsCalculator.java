package pt.ulisboa.tecnico.cnv.lbas.metrics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsCalculator implements Runnable {

    private static final int TIME_TO_SLEEP = 1000 * 10;

    private static final Map<InputRecord, Double> weights = new ConcurrentHashMap<>();

    private static final Map<String, Integer> heuristics = Map.of(
            "BlurImageHandler", 300,
            "EnhanceImageHandler", 300,
            "DetectQrCodeHandler", 500,
            "ImageClassificationHandler", 600);

    @Override
    public void run() {
        DynamoDBAdapter adapter = DynamoDBAdapter.getInstance();
        while (true) {
            try {
                List<CnvProjectMetrics> data = adapter.fetchData();
                recalculate(data);
                System.out.println(java.util.Arrays.toString(data.toArray()));
                Thread.sleep(TIME_TO_SLEEP);
            } catch (InterruptedException ie) {
                System.out.println(ie);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
            }
        }
    }

    public static Map<InputRecord, Double> getWeights() {
        return weights;
    }

    private void recalculate(List<CnvProjectMetrics> data) {
        for (CnvProjectMetrics dataPoint : data) {
            InputRecord input = new InputRecord(dataPoint.getImageSize(), dataPoint.getRequestClass());
            double weight = calculateWeight(dataPoint.getNBlocks(), dataPoint.getNInsts(), dataPoint.getNMethods(), dataPoint.getRequestClass());
            Double previous = weights.get(input);
            if (previous == null) {
                weights.put(input, weight);
            } else {
                weights.put(input, (weight + previous) / 2);
            }
        }
    }

    private static double calculateWeight(long nblocks, long ninsts, long nmethods, String requestClass) {
        Integer heuristicWeight = heuristics.get(requestClass);
        if (heuristicWeight == null) {
            heuristicWeight = 400;
        }
        double sum = nblocks + ninsts + nmethods + heuristicWeight;
        double methodWeight = (heuristicWeight / sum) * 0.4;
        return (nblocks / sum) * 0.4 + (ninsts / sum) * 0.15 + (nmethods / sum) * 0.05 + methodWeight;
    }

    public static double calculateWeight(CnvProjectMetrics metrics) {
        return calculateWeight(metrics.getNBlocks(), metrics.getNInsts(), metrics.getNMethods(), metrics.getRequestClass());
    }
}

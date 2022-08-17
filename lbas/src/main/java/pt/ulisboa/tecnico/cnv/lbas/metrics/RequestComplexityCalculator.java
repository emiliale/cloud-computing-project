package pt.ulisboa.tecnico.cnv.lbas.metrics;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.cnv.lbas.util.Pair;

public class RequestComplexityCalculator {

    public static double estimateWeight(long imageSize, String requestClass) {
        InputRecord input = new InputRecord(imageSize, requestClass);
        Map<InputRecord, Double> weights = MetricsCalculator.getWeights();
        Double result = weights.get(input);
        if (result == null) {
            List<Long> entries = weights.entrySet().stream()
                    .filter(e -> e.getKey().getRequestClass().equals(requestClass)).map(e -> e.getKey().getImageSize())
                    .collect(Collectors.toList());
            if (entries.size() < 2) {
                /* Default prediction if no information is available */
                return 0.45;
            } else {
                List<Long> closestSizes = findKClosest(entries, imageSize, 2);
                List<InputRecord> closestRecords = closestSizes.stream()
                        .map(size -> new InputRecord(size, requestClass)).collect(Collectors.toList());
                return interpolateLinear(imageSize, closestRecords, weights);
            }
        } else {
            return result;
        }
    }

    private static double interpolateLinear(long imageSize, List<InputRecord> closestRecords,
            Map<InputRecord, Double> weights) {
        long x = imageSize;
        long x0 = closestRecords.get(0).getImageSize();
        long x1 = closestRecords.get(1).getImageSize();
        double y0 = weights.get(closestRecords.get(0));
        double y1 = weights.get(closestRecords.get(1));
        return (y0 * (x1 - x) + y1 * (x - x0)) / (x1 - x0);
    }

    private static List<Long> findKClosest(List<Long> list, long x, int k) {

        // Make a max heap.
        PriorityQueue<Pair<Long, Long>> pq = new PriorityQueue<>(new Comparator<Pair<Long, Long>>() {
            public int compare(Pair<Long, Long> p1, Pair<Long, Long> p2) {
                return p2.getValue().compareTo(p1.getValue());
            }
        });

        // Build heap of difference with
        // first k elements
        for (int i = 0; i < k; i++) {
            pq.offer(new Pair<>(list.get(i), Math.abs(list.get(i) - x)));
        }

        // Now process remaining elements.
        for (int i = k; i < list.size(); i++) {
            long diff = Math.abs(list.get(i) - x);

            // If difference with current
            // element is more than root,
            // then ignore it.
            if (diff > pq.peek().getValue())
                continue;

            // Else remove root and insert
            pq.poll();
            pq.offer(new Pair<>(list.get(i), diff));
        }

        List<Long> result = new LinkedList<>();
        // Print contents of heap.
        while (!pq.isEmpty()) {
            result.add(pq.poll().getKey());
        }
        return result;
    }
}

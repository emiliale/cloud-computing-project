package pt.ulisboa.tecnico.cnv.lbas;

import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.lbas.metrics.MetricsCalculator;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new LoadBalancer());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(new AutoScaler());
        executorService.submit(new MetricsCalculator());
    }
}

package com.steitz.metrics.demo;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer.Context;
import com.codahale.metrics.riemann.Riemann;
import com.codahale.metrics.riemann.RiemannReporter;

import io.riemann.riemann.client.RiemannClient;

public class Driver {

    /** Metrics registry */
    private static final MetricRegistry metrics = new MetricRegistry();

    /** Processing timer */
    private static final com.codahale.metrics.Timer processingTimer = metrics
        .timer(name(Driver.class, "Processing time"));

    /** Error count */
    private static final Meter errorMeter = metrics
        .meter(name(Driver.class, "Error rate"));

    private static final Random random = new Random();

    public static void main(String[] args)
        throws InterruptedException,
        IOException {
        double errRate = 0.25;
        long itCount = 0;
        final RiemannClient riemannClient = RiemannClient
            .tcp("my.riemann.server", 5555);
        final Riemann riemann = new Riemann(riemannClient);
        final RiemannReporter reporter = RiemannReporter.forRegistry(metrics)
            .useSeparator("|").withTtl(500f)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .convertRatesTo(TimeUnit.SECONDS).build(riemann);

        while (true) {
            if (itCount % 100 == 0) {
                errRate = random.nextDouble() / 4;
            }
            final Context context = processingTimer.time();
            Thread.sleep(100 +
                         Math.round(Math.abs(random.nextGaussian()) * 1000));
            context.close();
            if (random.nextDouble() < errRate) {
                errorMeter.mark();
            }
            itCount++;
        }
    }
}

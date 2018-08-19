package com.steitz.metrics.demo;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.riemann.Riemann;
import com.codahale.metrics.riemann.RiemannReporter;
import com.codahale.metrics.riemann.ValueFilter;
import com.codahale.metrics.riemann.ValueFilterMap;

import io.riemann.riemann.client.RiemannClient;

public class Driver {

    /** Metrics registry */
    private static final MetricRegistry metrics = new MetricRegistry();

    /** Processing timer */
    private static final Timer processingTimer = metrics
        .timer(name(Driver.class, "Processing time"));

    /** Error count */
    private static final Meter errorMeter = metrics
        .meter(name(Driver.class, "Error rate"));

    private static final Random random = new Random();

    private static final String RIEMANN_HOST = "localhost";

    public static void main(String[] args)
        throws InterruptedException, IOException {
        double errRate = 0.25;
        long itCount = 0;
        final ValueFilterMap valueFilterMap = new ValueFilterMap();

        // Warn if 900 < mean latency < 1000
        valueFilterMap.addFilter(processingTimer, ValueFilterMap.MEAN,
                                 new ValueFilter.Builder("warn").withLower(900)
                                     .withUpper(1000).build());

        // Report critical if mean latency > 1000
        valueFilterMap.addFilter(processingTimer, ValueFilterMap.MEAN,
                                 new ValueFilter.Builder("critical")
                                     .withLowerExclusive(1000).build());

        // Warn if error rate is between 5 and 10 per minute
        valueFilterMap.addFilter(errorMeter, ValueFilterMap.M1_RATE,
                                 new ValueFilter.Builder("warn").withLower(5)
                                     .withUpper(10).build());

        // Report critical is greater than 10 per minute
        valueFilterMap.addFilter(errorMeter, ValueFilterMap.M1_RATE,
                                 new ValueFilter.Builder("warn")
                                     .withLowerExclusive(1).build());

        final RiemannClient riemannClient = RiemannClient.tcp(RIEMANN_HOST,
                                                              5555);
        final Riemann riemann = new Riemann(riemannClient);
        final RiemannReporter reporter = RiemannReporter.forRegistry(metrics)
            .useSeparator("|").withTtl(50f).withValueFilterMap(valueFilterMap)
            .convertDurationsTo(TimeUnit.MILLISECONDS).build(riemann);
        reporter.start(100, TimeUnit.MILLISECONDS);

        while (true) {
            if (itCount % 100 == 0) {
                errRate = random.nextDouble() / 3;
            }
            final Timer.Context context = processingTimer.time();
            Thread.sleep(100 +
                         Math.round(Math.abs(random.nextGaussian()) * 1000));
            context.stop();
            if (random.nextDouble() < errRate) {
                errorMeter.mark();
            }
            itCount++;
        }
    }
}

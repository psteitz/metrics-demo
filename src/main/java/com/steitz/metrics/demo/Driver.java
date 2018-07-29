package com.steitz.metrics.demo;

import static com.codahale.metrics.MetricRegistry.name;

import java.util.Random;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer.Context;

public class Driver {

    /** Metrics registry */
    static final MetricRegistry metrics = new MetricRegistry();

    /** Processing timer */
    private static final com.codahale.metrics.Timer processingTimer = metrics
        .timer(name(Driver.class, "Processing time"));

    /** Error count */
    private static final Meter errorMeter = metrics
        .meter(name(Driver.class, "Error rate"));

    private static final Random random = new Random();

    public static void main(String[] args)
        throws InterruptedException {
        double errRate = 0.25;
        long itCount = 0;
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

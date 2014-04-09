package com.gmmapowell.metrics;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

public class CodaHaleMetrics {
	
	public final static MetricRegistry metrics = new MetricRegistry();
	
	public static void configureReports(String metricsDirectory, int frequencyInSeconds) {
		final CsvReporter reporter = CsvReporter.forRegistry(metrics)
				.formatFor(Locale.US)
	            .convertRatesTo(TimeUnit.SECONDS)
	            .convertDurationsTo(TimeUnit.MILLISECONDS)
	            .build(new File(metricsDirectory));
		reporter.start(frequencyInSeconds, TimeUnit.SECONDS);
	}
	
	public static void configureGraphite(String name, String graphiteHost, int graphitePort, int metricsamplingfreq) {
		final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHost, graphitePort));
		final GraphiteReporter reporter = GraphiteReporter.forRegistry(metrics)
		                                                  .prefixedWith(name)
		                                                  .convertRatesTo(TimeUnit.SECONDS)
		                                                  .convertDurationsTo(TimeUnit.MILLISECONDS)
		                                                  .filter(MetricFilter.ALL)
		                                                  .build(graphite);
		reporter.start(metricsamplingfreq, TimeUnit.SECONDS);
	}

}

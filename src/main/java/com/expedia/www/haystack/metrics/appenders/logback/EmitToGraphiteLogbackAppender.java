/*
 *  Copyright 2017 Expedia, Inc.
 *
 *        Licensed under the Apache License, Version 2.0 (the "License");
 *        you may not use this file except in compliance with the License.
 *        You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *        Unless required by applicable law or agreed to in writing, software
 *        distributed under the License is distributed on an "AS IS" BASIS,
 *        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *        See the License for the specific language governing permissions and
 *        limitations under the License.
 */

package com.expedia.www.haystack.metrics.appenders.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.expedia.www.haystack.metrics.GraphiteConfig;
import com.expedia.www.haystack.metrics.GraphiteConfigImpl;
import com.expedia.www.haystack.metrics.MetricObjects;
import com.expedia.www.haystack.metrics.MetricPublishing;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.util.VisibleForTesting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ch.qos.logback.classic.Level.ERROR;

/**
 * A logback appender that sends an error count to a graphite endpoint.
 */
@SuppressWarnings("WeakerAccess") // for the setter methods that need to be public to be used by other packages
public class EmitToGraphiteLogbackAppender extends AppenderBase<ILoggingEvent> {
    @VisibleForTesting
    static Factory factory = new Factory();
    @VisibleForTesting
    static final String SUBSYSTEM = "errors";
    @VisibleForTesting
    static final Map<Integer, Counter> ERRORS_COUNTERS = new ConcurrentHashMap<>();
    @VisibleForTesting
    static MetricObjects metricObjects = new MetricObjects();

    private final MetricPublishing metricPublishing;

    private String host = "haystack.local"; // this is the value used by Minikube
    private int port = 2003;
    private int pollintervalseconds = 60;
    private int queuesize = 10;
    private boolean sendasrate = false;

    /**
     * The default constructor, used by logback. Logback configuration uses setters, but of the five values needed
     * (host, port, poll interval, send as rate, and queue size), all but host are set to sensible values and probably
     * don't need to be configured. The host should be set to the DNS name or IP host of the Graphite endpoint
     * you wish to receive counts of errors.
     */
    public EmitToGraphiteLogbackAppender() {
        this(new MetricPublishing());
    }

    @VisibleForTesting
    EmitToGraphiteLogbackAppender(MetricPublishing metricPublishing) {
        this.metricPublishing = metricPublishing;
    }

    // Setters are used by logback to configure the Appender.
    public void setHost(String host) {
        this.host = host;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setPollintervalseconds(int pollintervalseconds) {
        this.pollintervalseconds = pollintervalseconds;
    }
    public void setQueuesize(int queuesize) {
        this.queuesize = queuesize;
    }
    public void setSendasrate(boolean sendasrate) {
        this.sendasrate = sendasrate;
    }

    /**
     * Starts the appender by starting a background thread to poll the error counters and publish them to Graphite.
     * Multiple instances of this EmitToGraphiteLogbackAppender will only start one background thread.
     */
    @Override
    public void start() {
        startMetricPublishingBackgroundThread(
                host, port, pollintervalseconds, queuesize, sendasrate);
        super.start();
        factory.createStartUpMetric().emit(factory);
    }

    /**
     * Stops the appender, shutting down the background polling thread to ensure that the connection to the metrics
     * database is closed.
     */
    @Override
    public void stop() {
        super.stop();
        metricPublishing.stop();
    }

    @VisibleForTesting
    void startMetricPublishingBackgroundThread(
            String host, int port, int pollintervalseconds, int queuesize, boolean sendasrate) {
        final GraphiteConfig graphiteConfig = new GraphiteConfigImpl(
                host, port, pollintervalseconds, queuesize, sendasrate);
        metricPublishing.start(graphiteConfig);
    }

    @Override
    protected void append(ILoggingEvent logEvent) {
        final Level level = logEvent.getLevel();
        if (isLevelSevereEnoughToCount(level)) {
            final StackTraceElement[] stackTraceElements = logEvent.getCallerData();
            final StackTraceElement stackTraceElement = stackTraceElements[0];
            getCounter(level, stackTraceElement, stackTraceElement.hashCode()).increment();
        }
    }

    @VisibleForTesting
    boolean isLevelSevereEnoughToCount(Level level) {
        return level == ERROR;
    }

    private Counter getCounter(Level level, StackTraceElement stackTraceElement, int hashCode) {
        if (!ERRORS_COUNTERS.containsKey(hashCode)) {
            final String fullyQualifiedClassName = changePeriodsToDashes(stackTraceElement.getClassName());
            final String lineNumber = Integer.toString(stackTraceElement.getLineNumber());
            final Counter counter = factory.createCounter(fullyQualifiedClassName, lineNumber, level.toString());

            // It is possible but highly unlikely that two threads are in this if() block at the same time; if that
            // occurs, only one of the calls to ERRORS_COUNTERS.putIfAbsent(hashCode, counter) in the next line of code
            // will succeed, but the increment of the thread whose call did not succeed will not be lost, because the
            // value returned by this method will be the Counter put successfully by the other thread.
            ERRORS_COUNTERS.putIfAbsent(hashCode, counter);
        }
        return ERRORS_COUNTERS.get(hashCode);
    }

    static String changePeriodsToDashes(String fullyQualifiedClassName) {
        return fullyQualifiedClassName.replace('.', '-');
    }

    @VisibleForTesting
    static class Factory {

        Counter createCounter(String application, String className, String counterName) {
            return metricObjects.createAndRegisterResettingCounter(
                    SUBSYSTEM, application, className, counterName);
        }

        StartUpMetric createStartUpMetric() {
            return new StartUpMetric();
        }
    }

}

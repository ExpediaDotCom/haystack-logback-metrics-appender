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
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.expedia.www.haystack.metrics.GraphiteConfig;
import com.expedia.www.haystack.metrics.GraphiteConfigImpl;
import com.expedia.www.haystack.metrics.MetricObjects;
import com.expedia.www.haystack.metrics.MetricPublishing;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.util.VisibleForTesting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static ch.qos.logback.classic.Level.ERROR;

/**
 * A logback appender that sends an error count to a graphite endpoint.
 */
public class EmitToGraphiteLogbackAppender extends AppenderBase<ILoggingEvent> {
    @VisibleForTesting
    static Factory factory = new Factory();
    @VisibleForTesting
    static final String SUBSYSTEM = "errors";
    @VisibleForTesting
    static final Map<Integer, Counter> ERRORS_COUNTERS = new ConcurrentHashMap<>();
    @VisibleForTesting
    static final AtomicReference<MetricPublishing> METRIC_PUBLISHING = new AtomicReference<>(null);
    @VisibleForTesting
    static MetricObjects metricObjects = new MetricObjects();

    private String address = "localhost";
    private int port = 2003;
    private int pollintervalseconds = 60;
    private int queuesize = 10;

    /**
     * The default and only constructor. Logback configuration uses setters, but of the four values needed
     * (address, port, poll interval, and queue size), all but address are set to sensible values and probably
     * don't need to be configured. The address should be set to the DNS name or IP address of the Graphite endpoint
     * you wish to receive counts of errors.
     */
    public EmitToGraphiteLogbackAppender() {
        // Logback configuration uses setters
    }

    // Setters are used by logback to configure the Appender
    void setAddress(String address) {
        this.address = address;
    }
    void setPort(int port) {
        this.port = port;
    }
    void setPollintervalseconds(int pollintervalseconds) {
        this.pollintervalseconds = pollintervalseconds;
    }
    void setQueuesize(int queuesize) {
        this.queuesize = queuesize;
    }

    /**
     * Starts the appender by starting a background thread to poll the error counters and publish them to Graphite.
     * Multiple instances of this EmitToGraphiteLogbackAppender will only start one background thread.
     */
    @Override
    public void start() {
        startMetricPublishingBackgroundThreadIfNotAlreadyStarted(
                this.address, this.port, this.pollintervalseconds, this.queuesize);
        super.start();
    }

    @VisibleForTesting
    static void startMetricPublishingBackgroundThreadIfNotAlreadyStarted(
            String address, int port, int pollintervalseconds, int queuesize) {
        if (METRIC_PUBLISHING.compareAndSet(null, factory.createMetricPublishing())) {
            final GraphiteConfig graphiteConfig = new GraphiteConfigImpl(address, port, pollintervalseconds, queuesize);
            METRIC_PUBLISHING.get().start(graphiteConfig);
        }
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
            final String fullyQualifiedClassName = stackTraceElement.getClassName().replace('.', '-');
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

    @VisibleForTesting
    static class Factory {

        Counter createCounter(String application, String className, String counterName) {
            return metricObjects.createAndRegisterCounter(SUBSYSTEM, application, className, counterName);
        }

        MetricPublishing createMetricPublishing() {
            return new MetricPublishing();
        }
    }
}

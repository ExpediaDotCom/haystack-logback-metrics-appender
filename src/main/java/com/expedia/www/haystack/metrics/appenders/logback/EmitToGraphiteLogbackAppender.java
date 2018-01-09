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
import com.expedia.www.haystack.metrics.GraphiteConfigImpl;
import com.expedia.www.haystack.metrics.MetricObjects;
import com.expedia.www.haystack.metrics.MetricPublishing;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.util.VisibleForTesting;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import static ch.qos.logback.classic.Level.ERROR;

/**
 * A logback appender that sends an error count to a graphite endpoint.
 */
@SuppressWarnings("WeakerAccess") // for the setter methods that need to be public to be used by other packages
public class EmitToGraphiteLogbackAppender extends AppenderBase<ILoggingEvent> {
    @VisibleForTesting
    static final String ERRORS_METRIC_GROUP = "errors";
    @VisibleForTesting
    static final Map<String, Counter> ERRORS_COUNTERS = new ConcurrentHashMap<>();

    private final MetricPublishing metricPublishing;
    private final MetricObjects metricObjects;
    private final Factory factory;

    // These attributes need to be configured
    private String host = "haystack.local"; // this is the value used by Minikube
    private String subsystem;

    // These attributes have sensible default values and don't need to be configured
    private int port = 2003;
    private int pollintervalseconds = 60;
    private int queuesize = 10;
    private boolean sendasrate = false;

    // This attribute is not set until the appender starts
    private StartUpMetric startUpMetric;

    /**
     * The default constructor, used by logback. Logback configuration uses setters, but of the six values needed
     * (host, subsystem, port, poll interval, send as rate, and queue size), all but host and subsystem are set to
     * sensible values and probably don't need to be configured. The host should be set to the DNS name or IP host of
     * the Graphite endpoint you wish to receive counts of errors.
     */
    public EmitToGraphiteLogbackAppender() {
        this(new MetricPublishing(), new MetricObjects(), new Factory());
    }

    @VisibleForTesting
    EmitToGraphiteLogbackAppender(MetricPublishing metricPublishing, MetricObjects metricObjects, Factory factory) {
        this.metricPublishing = metricPublishing;
        this.metricObjects = metricObjects;
        this.factory = factory;
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
    public void setSubsystem(String subsystem) {
        this.subsystem = subsystem;
    }

    /**
     * Starts the appender by starting a background thread to poll the error counters and publish them to Graphite.
     * Multiple instances of this EmitToGraphiteLogbackAppender will only start one background thread.
     * This method also starts the heartbeat metric background thread.
     */
    @Override
    public void start() {
        super.start();
        metricPublishing.start(new GraphiteConfigImpl(host, port, pollintervalseconds, queuesize, sendasrate));
        this.startUpMetric = factory.createStartUpMetric(metricObjects, subsystem, new Timer());
        startUpMetric.start();
    }

    /**
     * Stops the appender, shutting down the background polling thread to ensure that the connection to the metrics
     * database is closed. This method also stops the heartbeat method background thread.
     */
    @Override
    public void stop() {
        metricPublishing.stop();
        if(startUpMetric != null) {
            startUpMetric.stop();
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent logEvent) {
        final Level level = logEvent.getLevel();
        if (isLevelSevereEnoughToCount(level)) {
            final StackTraceElement[] stackTraceElements = logEvent.getCallerData();
            final StackTraceElement stackTraceElement = stackTraceElements[0];
            getCounter(level, stackTraceElement).increment();
        }
    }

    @VisibleForTesting
    boolean isLevelSevereEnoughToCount(Level level) {
        return level == ERROR;
    }

    @VisibleForTesting
    Counter getCounter(Level level, StackTraceElement stackTraceElement) {
        final String fullyQualifiedClassName = changePeriodsToDashes(stackTraceElement.getClassName());
        final String lineNumber = Integer.toString(stackTraceElement.getLineNumber());
        final String key = fullyQualifiedClassName + ':' + lineNumber;
        if (!ERRORS_COUNTERS.containsKey(key)) {
            final Counter counter = factory.createCounter(
                    metricObjects, subsystem, fullyQualifiedClassName, lineNumber, level.toString());

            // It is possible but highly unlikely that two threads are in this if() block at the same time; if that
            // occurs, only one of the calls to ERRORS_COUNTERS.putIfAbsent(hashCode, counter) in the next line of code
            // will succeed, but the increment of the thread whose call did not succeed will not be lost, because the
            // value returned by this method will be the Counter put successfully by the other thread.
            ERRORS_COUNTERS.putIfAbsent(key, counter);
        }
        return ERRORS_COUNTERS.get(key);
    }

    static String changePeriodsToDashes(String fullyQualifiedClassName) {
        return fullyQualifiedClassName.replace('.', '-');
    }

    @VisibleForTesting
    static class Factory {
        Counter createCounter(MetricObjects metricObjects, String subsystem, String fullyQualifiedClassName,
                              String lineNumber, String counterName) {
            return metricObjects.createAndRegisterResettingCounter(
                    ERRORS_METRIC_GROUP, subsystem, fullyQualifiedClassName, lineNumber, counterName);
        }

        StartUpMetric createStartUpMetric(MetricObjects metricObjects, String subsystem, Timer timer) {
            return new StartUpMetric(timer, new StartUpMetric.Factory(), metricObjects, subsystem);
        }
    }
}

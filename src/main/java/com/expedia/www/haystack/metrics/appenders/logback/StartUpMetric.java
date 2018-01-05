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

import com.expedia.www.haystack.metrics.MetricObjects;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.util.VisibleForTesting;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static ch.qos.logback.classic.Level.ERROR;
import static com.expedia.www.haystack.metrics.appenders.logback.EmitToGraphiteLogbackAppender.ERRORS_SUBSYSTEM;

class StartUpMetric {
    private static final int METRIC_VALUE = 0;
    private static final long INITIAL_DELAY_MILLIS = 0L;
    private static final int INTERVAL_MINUTES = 1;
    private static final String FULLY_QUALIFIED_CLASS_NAME = EmitToGraphiteLogbackAppender.changePeriodsToDashes(
            StartUpMetric.class.getName());

    private final Timer timer;
    private final Counter counter;

    StartUpMetric(Timer timer, Factory factory, MetricObjects metricObjects, String subsystem) {
        this.timer = timer;
        this.counter = factory.createCounter(metricObjects, subsystem,
                LINE_NUMBER_OF_EMIT_METHOD_IN_START_UP_METRIC_CLASS);
    }

    void start() {
        timer.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        emit();
                    }
                },
                INITIAL_DELAY_MILLIS,
                TimeUnit.MINUTES.toMillis(INTERVAL_MINUTES));
    }

    void stop() {
        timer.cancel();
    }

    private static final String LINE_NUMBER_OF_EMIT_METHOD_IN_START_UP_METRIC_CLASS = Integer.toString(
            new Throwable().getStackTrace()[0].getLineNumber() + 2);
    private void emit() {
        counter.increment(METRIC_VALUE);
    }

    @VisibleForTesting
    static class Factory {
        Counter createCounter(MetricObjects metricObjects, String subsystem, String lineNumber) {
            final String subsystemAndFullyQualifiedClassName = subsystem + '-' + FULLY_QUALIFIED_CLASS_NAME;
            return metricObjects.createAndRegisterResettingCounter(
                    ERRORS_SUBSYSTEM, subsystemAndFullyQualifiedClassName, lineNumber, ERROR.toString());
        }
    }
}

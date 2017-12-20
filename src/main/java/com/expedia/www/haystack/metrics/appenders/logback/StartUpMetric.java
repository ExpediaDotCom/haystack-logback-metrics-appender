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
import com.expedia.www.haystack.metrics.MetricObjects;
import com.netflix.servo.monitor.Counter;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

class StartUpMetric {
    private static final int METRIC_VALUE = 0;
    private static final long INITIAL_DELAY_MILLIS = 0L;
    private static final int INTERVAL_MINUTES = 1;

    StartUpMetric(Timer timer, EmitToGraphiteLogbackAppender.Factory factory, MetricObjects metricObjects) {
        timer.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        emit(metricObjects, factory);
                    }
                },
                INITIAL_DELAY_MILLIS,
                TimeUnit.MINUTES.toMillis(INTERVAL_MINUTES));
    }

    static final String LINE_NUMBER_OF_EMIT_START_UP_METRIC_METHOD = Integer.toString(
            new Throwable().getStackTrace()[0].getLineNumber() + 3);

    void emit(MetricObjects metricObjects, EmitToGraphiteLogbackAppender.Factory factory) {
        final String fullyQualifiedClassName = EmitToGraphiteLogbackAppender.changePeriodsToDashes(
                StartUpMetric.class.getName());
        final Counter counter = factory.createCounter(metricObjects,
                fullyQualifiedClassName, LINE_NUMBER_OF_EMIT_START_UP_METRIC_METHOD, Level.ERROR.toString());
        counter.increment(METRIC_VALUE);
    }
}

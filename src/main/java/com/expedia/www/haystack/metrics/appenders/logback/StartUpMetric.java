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
import com.netflix.servo.monitor.Counter;

class StartUpMetric {
    static final int METRIC_VALUE = -1;
    static final String LINE_NUMBER_OF_EMIT_START_UP_METRIC_METHOD = Integer.toString(
            new Throwable().getStackTrace()[0].getLineNumber() + 2);
    void emit(EmitToGraphiteLogbackAppender.Factory factory) {
        final String fullyQualifiedClassName = EmitToGraphiteLogbackAppender.changePeriodsToDashes(
                StartUpMetric.class.getName());
        final Counter counter = factory.createCounter(
                fullyQualifiedClassName, LINE_NUMBER_OF_EMIT_START_UP_METRIC_METHOD, Level.ERROR.toString());
        counter.increment(METRIC_VALUE);
    }}

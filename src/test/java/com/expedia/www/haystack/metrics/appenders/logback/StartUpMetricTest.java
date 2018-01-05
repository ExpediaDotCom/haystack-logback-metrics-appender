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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StartUpMetricTest {
    static final String LINE_NUMBER_OF_EMIT_METHOD_IN_START_UP_METRIC_CLASS = "63";
    private static final Random RANDOM = new Random();
    private static final String SUBSYSTEM = RANDOM.nextLong() + "SUBSYSTEM";
    private static final String FULLY_QUALIFIED_CLASS_NAME = EmitToGraphiteLogbackAppender.changePeriodsToDashes(
            StartUpMetric.class.getName());

    @Mock
    private StartUpMetric.Factory mockFactory;

    @Mock
    private Counter mockCounter;

    @Mock
    private Timer mockTimer;

    @Mock
    private MetricObjects mockMetricObjects;

    private StartUpMetric startUpMetric;

    @Before
    public void setUp() {
        when(mockFactory.createCounter(any(MetricObjects.class), anyString(), anyString())).thenReturn(mockCounter);
        startUpMetric = new StartUpMetric(mockTimer, mockFactory, mockMetricObjects, SUBSYSTEM);
    }

    @After
    public void tearDown() {
        verify(mockFactory).createCounter(
                mockMetricObjects, SUBSYSTEM, LINE_NUMBER_OF_EMIT_METHOD_IN_START_UP_METRIC_CLASS);
        verifyNoMoreInteractions(mockFactory, mockCounter, mockTimer, mockMetricObjects);
    }

    @Test
    public void testStartAndEmit() {
        startUpMetric.start();

        final ArgumentCaptor<TimerTask> argumentCaptor = ArgumentCaptor.forClass(TimerTask.class);
        verify(mockTimer).scheduleAtFixedRate(argumentCaptor.capture(), eq(0L), eq(60000L));
        verifyThatTimerTaskRunCallsEmit(argumentCaptor);
    }

    private void verifyThatTimerTaskRunCallsEmit(ArgumentCaptor<TimerTask> argumentCaptor) {
        final TimerTask timerTask = argumentCaptor.getValue();
        timerTask.run();
        verify(mockCounter).increment(0);
    }

    @Test
    public void testStop() {
        startUpMetric.stop();

        verify(mockTimer).cancel();
    }
}

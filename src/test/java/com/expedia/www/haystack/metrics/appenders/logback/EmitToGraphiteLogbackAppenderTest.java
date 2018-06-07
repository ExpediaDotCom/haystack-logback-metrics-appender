/*
 * Copyright 2017 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.expedia.www.haystack.metrics.appenders.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.expedia.www.haystack.metrics.GraphiteConfig;
import com.expedia.www.haystack.metrics.GraphiteConfigImpl;
import com.expedia.www.haystack.metrics.MetricObjects;
import com.expedia.www.haystack.metrics.MetricPublishing;
import com.expedia.www.haystack.metrics.appenders.logback.EmitToGraphiteLogbackAppender.Factory;
import com.google.common.collect.Sets;
import com.netflix.servo.monitor.Counter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Random;
import java.util.Set;
import java.util.Timer;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;
import static ch.qos.logback.classic.Level.WARN;
import static com.expedia.www.haystack.metrics.appenders.logback.EmitToGraphiteLogbackAppender.ERRORS_COUNTERS;
import static com.expedia.www.haystack.metrics.appenders.logback.EmitToGraphiteLogbackAppender.ERRORS_METRIC_GROUP;
import static com.expedia.www.haystack.metrics.appenders.logback.StartUpMetricTest.LINE_NUMBER_OF_EMIT_METHOD_IN_START_UP_METRIC_CLASS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmitToGraphiteLogbackAppenderTest {
    private static final Random RANDOM = new Random();
    private static final boolean ENABLED = true;
    private static final String SUBSYSTEM = RANDOM.nextLong() + "SUBSYSTEM";
    private static final String HOST = RANDOM.nextLong() + "HOST";
    private static final String METHOD_NAME = RANDOM.nextLong() + "METHOD_NAME";
    private static final String FILE_NAME = RANDOM.nextLong() + "FILE_NAME";
    private static final int PORT = RANDOM.nextInt(Character.MAX_VALUE);
    private static final int POLL_INTERVAL_SECONDS = RANDOM.nextInt(Byte.MAX_VALUE);
    private static final int QUEUE_SIZE = RANDOM.nextInt(Byte.MAX_VALUE);
    private static final int LINE_NUMBER = RANDOM.nextInt(Integer.MAX_VALUE);
    private static final String S_LINE_NUMBER = Integer.toString(LINE_NUMBER);
    private static final boolean SEND_AS_RATE = RANDOM.nextBoolean();
    private static final Class<StartUpMetric> START_UP_METRIC_CLASS = StartUpMetric.class;
    private static final String START_UP_METRIC_FULLY_QUALIFIED_CLASS_NAME = START_UP_METRIC_CLASS.getName().replace('.', '-');
    private static final Class<EmitToGraphiteLogbackAppenderTest> TEST_CLASS = EmitToGraphiteLogbackAppenderTest.class;
    private static final String TEST_CLASS_NAME = TEST_CLASS.getName().replace('.', '-');
    private static final String COUNTER_NAME = ERROR.toString();
    private static final GraphiteConfig GRAPHITE_CONFIG = new GraphiteConfigImpl(
            HOST, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE, SEND_AS_RATE);

    @Mock
    private Factory mockFactory;

    @Mock
    private MetricObjects mockMetricObjects;

    @Mock
    private Counter mockCounter;

    @Mock
    private MetricPublishing mockMetricPublishing;

    @Mock
    private ILoggingEvent mockLoggingEvent;

    @Mock
    private StartUpMetric mockStartUpMetric;

    @Mock
    private Timer mockTimer;

    private Factory factory;
    private EmitToGraphiteLogbackAppender emitToGraphiteLogbackAppender;

    @Before
    public void setUp() {
        factory = new Factory();
        emitToGraphiteLogbackAppender = new EmitToGraphiteLogbackAppender(
                mockMetricPublishing, mockMetricObjects, mockFactory);
        emitToGraphiteLogbackAppender.setEnabled(ENABLED);
        emitToGraphiteLogbackAppender.setHost(HOST);
        emitToGraphiteLogbackAppender.setSubsystem(SUBSYSTEM);
        emitToGraphiteLogbackAppender.setPort(PORT);
        emitToGraphiteLogbackAppender.setPollintervalseconds(POLL_INTERVAL_SECONDS);
        emitToGraphiteLogbackAppender.setQueuesize(QUEUE_SIZE);
        emitToGraphiteLogbackAppender.setSendasrate(SEND_AS_RATE);
    }

    @After
    public void tearDown() {
        ERRORS_COUNTERS.clear();
        verifyNoMoreInteractions(mockFactory, mockCounter, mockMetricObjects, mockMetricPublishing, mockLoggingEvent,
                mockStartUpMetric, mockTimer);
    }

    @Test
    public void testDefaultConstructor() {
        new EmitToGraphiteLogbackAppender();
    }

    @Test
    public void testFactoryCreateCounter() {
        when(mockMetricObjects.createAndRegisterResettingCounter(
                anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockCounter);

        final Counter counter = factory.createCounter(
                mockMetricObjects, SUBSYSTEM, START_UP_METRIC_FULLY_QUALIFIED_CLASS_NAME, S_LINE_NUMBER, COUNTER_NAME);

        assertSame(mockCounter, counter);
        verify(mockMetricObjects).createAndRegisterResettingCounter(ERRORS_METRIC_GROUP,
                SUBSYSTEM, START_UP_METRIC_FULLY_QUALIFIED_CLASS_NAME, S_LINE_NUMBER, COUNTER_NAME);
    }

    @Test
    public void testFactoryCreateStartUpMetric() {
        when(mockMetricObjects.createAndRegisterResettingCounter(
                anyString(), anyString(), anyString(), anyString())).thenReturn(mockCounter);

        final StartUpMetric startUpMetric = factory.createStartUpMetric(mockMetricObjects, SUBSYSTEM, mockTimer);

        assertNotNull(startUpMetric);
        verify(mockMetricObjects).createAndRegisterResettingCounter(ERRORS_METRIC_GROUP,
                SUBSYSTEM, START_UP_METRIC_FULLY_QUALIFIED_CLASS_NAME,
                LINE_NUMBER_OF_EMIT_METHOD_IN_START_UP_METRIC_CLASS, COUNTER_NAME);
    }

    @Test
    public void testIsLevelSevereEnoughToCount() {
        final Set<Level> levelsThatAreSevereEnoughToCount = Sets.newHashSet(ERROR);
        final Level[] allLevels = {ERROR, WARN, INFO, DEBUG, TRACE};
        for (final Level level : allLevels) {
            assertEquals(levelsThatAreSevereEnoughToCount.contains(level),
                    emitToGraphiteLogbackAppender.isLevelSevereEnoughToCount(level));
        }
    }

    @Test
    public void testAppendLevelNotSevereEnoughToCount() {
        when(mockLoggingEvent.getLevel()).thenReturn(INFO);

        emitToGraphiteLogbackAppender.append(mockLoggingEvent);

        verify(mockLoggingEvent).getLevel();
    }

    @Test
    public void testAppendLevelIsSevereEnoughToCount() {
        when(mockLoggingEvent.getLevel()).thenReturn(ERROR);
        final StackTraceElement[] stackTraceElements = new Exception().getStackTrace();
        when(mockLoggingEvent.getCallerData()).thenReturn(stackTraceElements);
        when(mockFactory.createCounter(any(MetricObjects.class), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockCounter);

        emitToGraphiteLogbackAppender.append(mockLoggingEvent);

        verify(mockLoggingEvent).getLevel();
        verify(mockLoggingEvent).getCallerData();
        final String lineNumber = Integer.toString(stackTraceElements[0].getLineNumber());
        verify(mockFactory).createCounter(
                mockMetricObjects, SUBSYSTEM, TEST_CLASS_NAME, lineNumber, COUNTER_NAME);
        verify(mockCounter).increment();
    }

    @Test
    public void testStart() {
        commonWhensForStart();

        emitToGraphiteLogbackAppender.start();

        assertTrue(emitToGraphiteLogbackAppender.isStarted());
        commonVerifiesForStart();
    }

    @Test
    public void testStartWhenDisabled() {
        commonWhensForStart();

        emitToGraphiteLogbackAppender.setEnabled(false);

        emitToGraphiteLogbackAppender.start();

        assertTrue(emitToGraphiteLogbackAppender.isStarted());
        verify(mockMetricPublishing, never()).start(any(GraphiteConfig.class));
        verify(mockFactory).createStartUpMetric(eq(mockMetricObjects), eq(SUBSYSTEM), any(Timer.class));
        verify(mockStartUpMetric).start();
    }

    @Test
    public void testStopStartUpMetricIsNotNull() {
        commonWhensForStart();

        emitToGraphiteLogbackAppender.start();
        emitToGraphiteLogbackAppender.stop();

        assertFalse(emitToGraphiteLogbackAppender.isStarted());
        commonVerifiesForStart();
        verify(mockStartUpMetric).stop();
        verify(mockMetricPublishing).stop();
    }

    private void commonWhensForStart() {
        when(mockFactory.createCounter(any(MetricObjects.class), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockCounter);
        when(mockFactory.createStartUpMetric(any(MetricObjects.class), anyString(), any(Timer.class)))
                .thenReturn(mockStartUpMetric);
    }

    private void commonVerifiesForStart() {
        verify(mockMetricPublishing).start(GRAPHITE_CONFIG);
        verify(mockFactory).createStartUpMetric(eq(mockMetricObjects), eq(SUBSYSTEM), any(Timer.class));
        verify(mockStartUpMetric).start();
    }

    @Test
    public void testStopStartUpMetricIsNull() {
        emitToGraphiteLogbackAppender.stop();

        assertFalse(emitToGraphiteLogbackAppender.isStarted());
        verify(mockMetricPublishing).stop();
    }

    @Test
    public void testGetCounter() {
        final StackTraceElement stackTraceElement = new StackTraceElement(
                START_UP_METRIC_CLASS.getName(), METHOD_NAME, FILE_NAME, LINE_NUMBER);
        when(mockFactory.createCounter(any(MetricObjects.class), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockCounter);

        final Counter counter1 = emitToGraphiteLogbackAppender.getCounter(Level.ERROR, stackTraceElement);
        final Counter counter2 = emitToGraphiteLogbackAppender.getCounter(Level.ERROR, stackTraceElement);

        assertSame(counter1, counter2);
        verify(mockFactory).createCounter(mockMetricObjects, SUBSYSTEM, START_UP_METRIC_FULLY_QUALIFIED_CLASS_NAME,
                Integer.toString(LINE_NUMBER), Level.ERROR.toString());
    }
 }

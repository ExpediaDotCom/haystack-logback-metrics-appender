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
import com.google.common.collect.Sets;
import com.netflix.servo.monitor.Counter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.Set;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;
import static ch.qos.logback.classic.Level.WARN;
import static com.expedia.www.haystack.metrics.appenders.logback.EmitToGraphiteLogbackAppender.ERRORS_COUNTERS;
import static com.expedia.www.haystack.metrics.appenders.logback.EmitToGraphiteLogbackAppender.METRIC_PUBLISHING;
import static com.expedia.www.haystack.metrics.appenders.logback.EmitToGraphiteLogbackAppender.SUBSYSTEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmitToGraphiteLogbackAppenderTest {
    private static final Random RANDOM = new Random();
    private static final String APPLICATION = RANDOM.nextLong() + "APPLICATION";
    private static final String HOST = RANDOM.nextLong() + "HOST";
    private static final int PORT = RANDOM.nextInt(Character.MAX_VALUE);
    private static final int POLL_INTERVAL_SECONDS = RANDOM.nextInt(Byte.MAX_VALUE);
    private static final int QUEUE_SIZE = RANDOM.nextInt(Byte.MAX_VALUE);
    private static final boolean SEND_AS_RATE = RANDOM.nextBoolean();
    private static final int NUMBER_OF_ITERATIONS_IN_TESTS = 2;
    private static final Class<EmitToGraphiteLogbackAppenderTest> CLASS = EmitToGraphiteLogbackAppenderTest.class;
    private static final String FULLY_QUALIFIED_CLASS_NAME = CLASS.getName().replace('.', '-');
    private static final String COUNTER_NAME = ERROR.toString();

    @Mock
    private EmitToGraphiteLogbackAppender.Factory mockFactory;
    private EmitToGraphiteLogbackAppender.Factory realFactory;

    @Mock
    private Counter mockCounter;

    @Mock
    private MetricObjects mockMetricObjects;
    private MetricObjects realMetricObjects;

    @Mock
    private MetricPublishing mockMetricPublishing;

    @Mock
    private ILoggingEvent mockLoggingEvent;

    private EmitToGraphiteLogbackAppender emitToGraphiteLogbackAppender;

    @Before
    public void setUp() {
        stubOutStaticDependencies();
        emitToGraphiteLogbackAppender = new EmitToGraphiteLogbackAppender();
        emitToGraphiteLogbackAppender.setHost(HOST);
        emitToGraphiteLogbackAppender.setPort(PORT);
        emitToGraphiteLogbackAppender.setPollintervalseconds(POLL_INTERVAL_SECONDS);
        emitToGraphiteLogbackAppender.setQueuesize(QUEUE_SIZE);
        emitToGraphiteLogbackAppender.setSendasrate(SEND_AS_RATE);
        METRIC_PUBLISHING.set(null);
        ERRORS_COUNTERS.clear();
    }

    private void stubOutStaticDependencies() {
        realFactory = EmitToGraphiteLogbackAppender.factory;
        EmitToGraphiteLogbackAppender.factory = mockFactory;

        realMetricObjects = EmitToGraphiteLogbackAppender.metricObjects;
        EmitToGraphiteLogbackAppender.metricObjects = mockMetricObjects;
    }

    @After
    public void tearDown() {
        restoreStaticDependencies();
        verifyNoMoreInteractions(mockFactory, mockCounter, mockMetricObjects, mockMetricPublishing, mockLoggingEvent);
    }

    private void restoreStaticDependencies() {
        EmitToGraphiteLogbackAppender.factory = realFactory;
        EmitToGraphiteLogbackAppender.metricObjects = realMetricObjects;
    }

    @Test
    public void testFactoryCreateCounter() {
        when(mockMetricObjects.createAndRegisterResettingCounter(
                anyString(), anyString(), anyString(), anyString())).thenReturn(mockCounter);

        final Counter counter = realFactory.createCounter(APPLICATION, FULLY_QUALIFIED_CLASS_NAME, COUNTER_NAME);

        assertSame(mockCounter, counter);
        verify(mockMetricObjects).createAndRegisterResettingCounter(
                SUBSYSTEM, APPLICATION, FULLY_QUALIFIED_CLASS_NAME, COUNTER_NAME);
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
    public void testStartMetricPublishingBackgroundThreadIfNotAlreadyStartedWhenAlreadyStarted() {
        final GraphiteConfig graphiteConfig = new GraphiteConfigImpl(
                HOST, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE, SEND_AS_RATE);
        when(mockFactory.createMetricPublishing()).thenReturn(mockMetricPublishing);

        for (int i = 0; i < NUMBER_OF_ITERATIONS_IN_TESTS; i++) {
            EmitToGraphiteLogbackAppender.startMetricPublishingBackgroundThreadIfNotAlreadyStarted(
                    HOST, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE, SEND_AS_RATE);
        }

        verify(mockFactory, times(NUMBER_OF_ITERATIONS_IN_TESTS)).createMetricPublishing();
        verify(mockMetricPublishing).start(graphiteConfig);
    }

    @Test
    public void testAppendLevelNotSevereEnoughToCount() {
        when(mockLoggingEvent.getLevel()).thenReturn(INFO);

        emitToGraphiteLogbackAppender.append(mockLoggingEvent);

        verify(mockLoggingEvent).getLevel();
    }

    @Test
    public void testEndToEndFunctionalBehavior() {
        emitToGraphiteLogbackAppender = new EmitToGraphiteLogbackAppender();
        when(mockFactory.createCounter(anyString(), anyString(), anyString())).thenReturn(mockCounter);

        final int lineNumberOfThisNewThrowable = new Throwable().getStackTrace()[0].getLineNumber();
        final String lineNumberOfTheLoggerDotErrorCall = Integer.toString(lineNumberOfThisNewThrowable + 3);
        for (int i = 0; i < NUMBER_OF_ITERATIONS_IN_TESTS; i++) {
            LoggerFactory.getLogger(EmitToGraphiteLogbackAppenderTest.class).error("Test");
        }

        verify(mockFactory).createCounter(
                FULLY_QUALIFIED_CLASS_NAME, lineNumberOfTheLoggerDotErrorCall, ERROR.toString());
        verify(mockCounter, times(NUMBER_OF_ITERATIONS_IN_TESTS)).increment();
    }

}

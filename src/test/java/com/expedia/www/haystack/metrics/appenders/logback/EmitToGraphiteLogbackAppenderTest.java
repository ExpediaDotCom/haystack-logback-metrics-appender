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
    private static final String ADDRESS = RANDOM.nextLong() + "ADDRESS";
    private static final int PORT = RANDOM.nextInt(Character.MAX_VALUE);
    private static final int POLL_INTERVAL_SECONDS = RANDOM.nextInt(Byte.MAX_VALUE);
    private static final int QUEUE_SIZE = RANDOM.nextInt(Byte.MAX_VALUE);
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
        emitToGraphiteLogbackAppender.setAddress(ADDRESS);
        emitToGraphiteLogbackAppender.setPort(PORT);
        emitToGraphiteLogbackAppender.setPollintervalseconds(POLL_INTERVAL_SECONDS);
        emitToGraphiteLogbackAppender.setQueuesize(QUEUE_SIZE);
        METRIC_PUBLISHING.set(null);
        ERRORS_COUNTERS.clear();
        when(mockFactory.createMetricPublishing()).thenReturn(mockMetricPublishing);
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
        when(mockMetricObjects.createAndRegisterCounter(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockCounter);

        final Counter counter = realFactory.createCounter(APPLICATION, FULLY_QUALIFIED_CLASS_NAME, COUNTER_NAME);

        assertSame(mockCounter, counter);
        verify(mockMetricObjects).createAndRegisterCounter(
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
        final GraphiteConfig graphiteConfig = new GraphiteConfigImpl(ADDRESS, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE);
        when(mockFactory.createMetricPublishing()).thenReturn(mockMetricPublishing);

        for (int i = 0; i < NUMBER_OF_ITERATIONS_IN_TESTS; i++) {
            EmitToGraphiteLogbackAppender.startMetricPublishingBackgroundThreadIfNotAlreadyStarted(
                    ADDRESS, PORT, POLL_INTERVAL_SECONDS, QUEUE_SIZE);
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

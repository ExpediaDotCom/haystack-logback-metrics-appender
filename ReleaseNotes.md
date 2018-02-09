# Release Notes

## 0.1.11 / 2018-02-08 Use new haystack-metrics 0.6.0
This new version of haystack-metrics ignores IllegalStateException when shutting down the poller.
(the IllegalStateException can happen if the poller has not been started.)

## 0.1.10 / 2018-01-09 Use new haystack-metrics API for error counter
so that the individual applications' error metrics can be identified as specific to each application

## 0.1.9 / 2018-01-05 Include subsystem in fqName of error metric
so that the heartbeat metric is different for each subsystem. This permits writing a dashboard with two metrics (the
first based on the fqName tag matching the fully qualified name in the heartbeat metric and the second based on the
fqName tag NOT matching the fully qualified name in the heartbeat metric) before any non-heartbeat errors have occurred.

## 0.1.8 / 2018-01-05 Shutdown heartbeat metric when appender stops

## 0.1.7 / 2017-12-20 Emit an ERROR metric, with a count of 0, every minute
The writing of a metric to show that the appender is working now occurs in a background thread every minute;
the value of the metric thus emitted will be 0. When an error occurs, the value of the metric will be greater than 0,
and the tags of the metric will be different than what was emitted by the background thread.

## 0.1.6 / 2017-12-18 Separate polling thread per appender; emit start up metric; call close() when shutting down
As part of an effort to be sure that the connection to the metrics database is closed when the appender is
no longer being used, each appender will have its own polling thread, and close() will be called appropriately.
This version also includes the writing of a start up metric to show that the appender is working, since it
doesn't emit any metrics until an error occurs.

## 0.1.5 / 2017-12-15 Upgrade haystack-metrics to 0.4.0, use new ResettingCounter
0.1.4 didn't work properly because more changes to haystack-metrics were needed.

## 0.1.4 / 2017-12-01 Upgrade haystack-metrics to 0.2.9, use new ResettingNonRateCounter
This is so that the metric produced when an error occurs will be a count, not a rate.

## 0.1.3 / 2017-12-01 Upgrade haystack-metrics to 0.2.7
This resulted in the renaming of variables and methods to refer to "host" instead of "address"
and is done to have a consistent name across all of Haystack.

## 0.1.2 / 2017-12-01 Upgrade haystack-metrics to 0.2.6

## 0.1.1 / 2017-11-20 Make setters public
The four setters in EmitToGraphiteLogbackAppender were package-private because it appeared that such access was
adequate, but using them in another package appears to show that they need to be public.

## 0.1.0 / 2017-11-20 Initial release to SonaType Nexus Repository
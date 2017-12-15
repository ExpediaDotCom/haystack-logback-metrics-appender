# Release Notes

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
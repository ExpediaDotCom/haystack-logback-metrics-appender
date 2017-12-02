# Release Notes

## 0.1.2 / 2017-12-01 Upgrade haystack-metrics to 0.2.6

## 0.1.1 / 2017-11-20 Make setters public
The four setters in EmitToGraphiteLogbackAppender were package-private because it appeared that such access was
adequate, but using them in another package appears to show that they need to be public.

## 0.1.0 / 2017-11-20 Initial release to SonaType Nexus Repository
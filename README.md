# oereb2-iconizer

## Anleitung


## Entwicklung

### Dummy WMS
```
docker run -p 8083:80 sogis/oereb2-wms:3.10
```
```
http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetCapabilities
```

```
http://localhost:8083/wms/oereb-symbols?&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetStyles&LAYERS=ch.SO.NutzungsplanungGrundnutzung&STYLE=default&SLD_VERSION=1.1.0
```

```
http://localhost:8083/wms/oereb-symbols?&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetLegendGraphic&LAYER=ch.SO.NutzungsplanungGrundnutzung&FORMAT=image/png&STYLE=default&SLD_VERSION=1.1.0
```

### Native image
```
native-image --no-fallback --no-server --verbose --report-unsupported-elements-at-runtime --native-image-info -cp build/libs/oereb2-iconizer-2.0.LOCALBUILD-all.jar -H:+ReportExceptionStackTraces
```

## TODO
- Anleitung
- JDK-only für File herunterladen?
- Factory pattern (creators)
- https://github.com/oracle/graal/issues/1163 (native image)
- Braucht es proxy-config.json? (native image)

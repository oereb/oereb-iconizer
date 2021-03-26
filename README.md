# oereb-iconizer

## Anleitung
todo

## Entwicklung

### Dummy WMS
```
docker run -p 8083:80 sogis/oereb2-wms:3.10
```

Es handelt sich dabei nicht um das "Original-Oereb-WMS-Image", sondern um ein Image, welches für die Test der zweiten Version des Rahmenmodells enstanden ist (https://github.com/oereb/oereb2-wms). Im Rahmen der Realisierung/Einführung der Version 2 des Rahmenmodells muss es offizialisiert werden oder das zu diesem Zeitpunkt richtige Image verwendet werden.

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

Funktioniert noch nicht.

## TODO
- Anleitung
    - Anwendungszweck?
    - Zwei selbständige Prozesse: Herunterladen und Import in DB (plus ggf Speichern in Verzeichnis)
    - Einfacher Ansatz gewählt. Was sind die Rahmenbedingungen?
    - Erklärung Substring-Modus.
- Können JDK-only Klassen für den Dateidownload verwendet werden? Mit Java 11 wäre das wegen des besseren Redirect-Supports wohl möglich. Unser GRETL-Image verlangt aber noch Java 8.
- Rename LegendEntry? Sind im Prinzip keine Legendeneinträge, sondern Symbole und Artcodes. -> "Symbol"?
- https://github.com/oracle/graal/issues/1163 (native image). Aber lieber keine weitere externe Abhängigkeit, falls es mit dieser Variante gehen würde. Also sein lassen.
- Braucht es proxy-config.json? (native image)

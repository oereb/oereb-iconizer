# oereb2-iconizer

## Dummy Symbol WMS
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
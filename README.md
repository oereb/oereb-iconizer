# oereb-iconizer

**Achtung:** Die Tests werden verschiedentlich failen, weil im _oereb-wms_ die Layernamen angepasst werden.

## Funktionsweise
_Oereb-iconizer_ hat folgende Funktionen:

1. Erstellung von Einzelsymbolen (`BufferedImage`) anhand einer "GetStyles"- und "GetLegendGraphic"-Antwort.
2. Speicherung der Einzelsymbole auf dem Filesystem.
3. Erstellung einer INTERLIS-Transferdatei (Modell: https://geo.so.ch/models/AGI/SO_AGI_OeREB_Legendeneintraege_20211020.ili) mit den Symbolen als `BLACKBOX BINARY`.

Implementiert ist die Erstellung der Symbole (Schritt 1) für QGIS-Server 3.x. Andere Implementierungen müssen sich vor allem um den unterschiedlichen Aufruf des Einzelsymbole-GetLegenendGraphic-Requests und die Parse-Logik der GetStyles-Antwort kümmern. 

Damit der gesamte Prozess relativ simpel und durchschaubar bleibt, gibt es verschiedene harte und einschränkende Rahmenbedingungen: Die Definition der Style in QGIS muss so gemacht werden, dass es pro Artcode (in einem Thema) eine Rule gibt. Entscheidend ist jedoch das resultierende SLD (aus dem GetStyles-Request):

```
<se:Rule>
    <se:Name>Wohnzone 1 G</se:Name>
    <se:Description>
        <se:Title>Wohnzone 1 G</se:Title>
    </se:Description>
    <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
        <ogc:PropertyIsEqualTo>
            <ogc:Function name="substr">
                <ogc:PropertyName>artcode</ogc:PropertyName>
                <ogc:Literal>1</ogc:Literal>
                <ogc:Literal>3</ogc:Literal>
            </ogc:Function>
            <ogc:Literal>110</ogc:Literal>
        </ogc:PropertyIsEqualTo>
    </ogc:Filter>
    <se:PolygonSymbolizer>
        <se:Fill>
            <se:SvgParameter name="fill">#ffff33</se:SvgParameter>
        </se:Fill>
    </se:PolygonSymbolizer>
</se:Rule>
```

Eine `Rule` muss ein `Name`- und ein `Filter`-Element haben. Der `Filter` muss ein `PropertyIsEqualTo`-Element haben. `PropertyIsEqualTo` muss ein `Literal`-Element haben, dessen Wert dem ÖREB-Artcode entspricht. Die Funktion kann beliebig sein. Anhand des Namens wird anschliessend ein spezielle "GetLegendGraphic"-Request gemacht und die Antwort (das einzelne Symbol) zusammen mit dem Artcode gespeichert.

Werden die Symbole auf dem Filesystem gespeichert, erhalten die Symbole als Namen den Wert des Artcodes. 

Bei der Erstellung der INTERLIS-Transferdatei müssen zwingend das Thema und die Artcodeliste mitgeliefert werden, damit ein spätere Zuweisung in der Datenbank mit SQL sichergestellt ist. Je nach Arbeitsweise und Thema wären wahrscheinlich beide Informationen nicht wirklich zwingend. 

## Anleitung (SO!GIS)

Falls Style verändert werden müssen oder neue dazu kommen, muss zuerst das QGIS-Projekt "oereb-symbols.qgs" nachgeführt werden. Dies wird im Git-Repository https://github.com/sogis-oereb/oereb-wms gemacht. Die Pipeline erstellt nach dem Pushen ein neues `sogis/oereb-wms` Docker Image. Anschliessend kann der Docker Container gestartet werden:

```
docker run -e QGIS_FCGI_MIN_PROCESSES=2 -e QGIS_FCGI_MAX_PROCESSES=2 -p 8083:80 sogis/oereb-wms:2
```

_Oereb-iconizer_ kann als Fatjar hier https://github.com/sogis-oereb/oereb-iconizer/releases/latest heruntergeladen werden.

Erstellen der Symbole und der INTERLIS-Transferdatei:

Bemerkung: Für einige Themen (Grundwasserschutz, Planungszonen, ... Legenden mit Randlinie) wird die SYMBOLWIDTH auf 5.8 gesetzt. Sonst schneidet QGIS den rechten Rand ab. Eine andere Variante wäre die WIDTH=73 wählen.

Planerischer Gewässerschutz:
```
java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.Grundwasserschutzzonen&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.Grundwasserschutzzonen&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=5.8&DPI=300" --fileName=ch.so.afu.oereb_grundwasserschutzzonen.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.zone --theme=ch.Grundwasserschutzzonen --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:Typ_Kanton_Grundwasserschutzzonen

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.Grundwasserschutzareale&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.Grundwasserschutzareale&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=5.8&DPI=300" --fileName=ch.so.afu.oereb_grundwasserschutzareale.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.areal --theme=ch.Grundwasserschutzareale --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:Typ_Kanton_Grundwasserschutzareale
```

Naturreservate (Einzelschutz + Nutzungsplanung):
```
java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.Einzelschutz.Flaeche&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.Einzelschutz.Flaeche&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_einzelschutz_naturreservat_einzelschutz.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.einzelschutz --theme=ch.SO.Einzelschutz --typeCodeList=urn:fdc:ilismeta.interlis.ch:2019:Typ_Naturschutzgebiete_Flaeche

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.NutzungsplanungUeberlagernd.Flaeche&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.NutzungsplanungUeberlagernd.Flaeche&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_einzelschutz_naturreservat_nutzungsplanung.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.nutzungsplanung --theme=ch.Nutzungsplanung --subtheme=ch.SO.NutzungsplanungUeberlagernd --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:NP_Typ_Kanton_Ueberlagernd_Flaeche.Naturreservat_Flaeche
```

Geotope (Einzelschutz):
```
java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.Einzelschutz.Flaeche&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.Einzelschutz.Flaeche&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.afu.oereb_einzelschutz_geotop_flaeche.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.einzelschutz.flaeche --theme=ch.SO.Einzelschutz --typeCodeList=urn:fdc:ilismeta.interlis.ch:2020:Typ_geschuetztes_Geotop_Flaeche

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.Einzelschutz.Punkt&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.Einzelschutz.Punkt&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.afu.oereb_einzelschutz_geotop_punkt.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.einzelschutz.punkt --theme=ch.SO.Einzelschutz --typeCodeList=urn:fdc:ilismeta.interlis.ch:2020:Typ_geschuetztes_Geotop_Punkt
```

Denkmalschutz (Einzelschutz):
```
java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.Einzelschutz.Flaeche&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.Einzelschutz.Flaeche&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.ada.oereb_einzelschutz_denkmal_flaeche.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.einzelschutz.flaeche --theme=ch.SO.Einzelschutz --typeCodeList=urn:fdc:ilismeta.interlis.ch:2019:Typ_geschuetztes_historisches_Kulturdenkmal_Flaeche

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.Einzelschutz.Punkt&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.Einzelschutz.Punkt&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.ada.oereb_einzelschutz_denkmal_punkt.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.einzelschutz.punkt --theme=ch.SO.Einzelschutz --typeCodeList=urn:fdc:ilismeta.interlis.ch:2019:Typ_geschuetztes_historisches_Kulturdenkmal_Punkt
```

Waldgrenzen:
```
java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.StatischeWaldgrenzen&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.StatischeWaldgrenzen&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.awjf.oereb_statische_waldgrenzen.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege --theme=ch.StatischeWaldgrenzen --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:Typ_Kanton_Waldgrenzen
```

Planungszonen:
```
java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.Planungszonen&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.Planungszonen&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=5.8&DPI=300" --fileName=ch.so.arp.oereb_planungszonen.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege --theme=ch.Planungszonen --typeCodeList=urn:fdc:ilismeta.interlis.ch:2022:Typ_Kanton_Planungszonen
```

Gewässerraum:
```
java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.Gewaesserraum.Flaeche&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.Gewaesserraum.Flaeche&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.afu.oereb_gewaesserraum_flaeche.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.gewaesserraum.flaeche --theme=ch.Gewaesserraum --typeCodeList=urn:fdc:ilismeta.interlis.ch:2022:Typ_Kanton_Gewaesserraum_Flaeche

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.Gewaesserraum.Linie&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.Gewaesserraum.Linie&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.afu.oereb_gewaesserraum_linie.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.gewaesserraum.linie --theme=ch.Gewaesserraum --typeCodeList=urn:fdc:ilismeta.interlis.ch:2022:Typ_Kanton_Gewaesserraum_Linie
```

Nutzungsplanung:
```
java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.NutzungsplanungGrundnutzung&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.NutzungsplanungGrundnutzung&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_nutzungsplanung_grundnutzung.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.nutzungsplanung_grundnutzung --theme=ch.Nutzungsplanung --subtheme=ch.SO.NutzungsplanungGrundnutzung --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:NP_Typ_Kanton_Grundnutzung

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.NutzungsplanungUeberlagernd.Flaeche&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.NutzungsplanungUeberlagernd.Flaeche&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_nutzungsplanung_ueberlagernd_flaeche.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.nutzungsplanung_ueberlagernd.flaeche --theme=ch.Nutzungsplanung --subtheme=ch.SO.NutzungsplanungUeberlagernd --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:NP_Typ_Kanton_Ueberlagernd_Flaeche

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.NutzungsplanungUeberlagernd.Linie&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.NutzungsplanungUeberlagernd.Linie&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_nutzungsplanung_ueberlagernd_linie.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.nutzungsplanung_ueberlagernd.linie --theme=ch.Nutzungsplanung --subtheme=ch.SO.NutzungsplanungUeberlagernd --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:NP_Typ_Kanton_Ueberlagernd_Linie

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.NutzungsplanungUeberlagernd.Punkt&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.NutzungsplanungUeberlagernd.Punkt&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_nutzungsplanung_ueberlagernd_punkt.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.nutzungsplanung_ueberlagernd.punkt --theme=ch.Nutzungsplanung --subtheme=ch.SO.NutzungsplanungUeberlagernd --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:NP_Typ_Kanton_Ueberlagernd_Punkt

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.Baulinien&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.Baulinien&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_baulinien.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.baulinien --theme=ch.Nutzungsplanung --subtheme=ch.SO.Baulinien --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:NP_Typ_Kanton_Erschliessung_Linienobjekt

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.NutzungsplanungSondernutzungsplaene&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.NutzungsplanungSondernutzungsplaene&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_nutzungsplanung_sondernutzungsplaene.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.nutzungsplanung_sondernutzungsplaene --theme=ch.Nutzungsplanung --subtheme=ch.SO.NutzungsplanungSondernutzungsplaene --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:NP_Typ_Kanton_Ueberlagernd_Flaeche

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.Laermempfindlichkeitsstufen&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.Laermempfindlichkeitsstufen&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_laermempfindlichkeitsstufen.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.laermempfindlichkeitsstufen --theme=ch.Laermempfindlichkeitsstufen --typeCodeList=urn:fdc:ilismeta.interlis.ch:2020:Typ_Kanton_Empfindlichkeitsstufe

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.Waldabstandslinien&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.Waldabstandslinien&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_waldabstandslinien.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.waldabstandslinien --theme=ch.Waldabstandslinien --typeCodeList=urn:fdc:ilismeta.interlis.ch:2017:NP_Typ_Kanton_Erschliessung_Linienobjekt
```

Nutzungsplanung (kantonal):
```
java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.NutzungsplanungUeberlagernd.Flaeche&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.NutzungsplanungUeberlagernd.Flaeche&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_nutzungsplanung_ueberlagernd_flaeche.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.nutzungsplanung_ueberlagernd.flaeche --theme=ch.Nutzungsplanung --subtheme=ch.SO.NutzungsplanungUeberlagernd --typeCodeList=urn:fdc:ilismeta.interlis.ch:2022:Nutzungsplanung_kantonal_Ueberlagernd_Flaeche

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.NutzungsplanungSondernutzungsplaene&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.NutzungsplanungSondernutzungsplaene&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_nutzungsplanung_sondernutzungsplaene.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.nutzungsplanung_sondernutzungsplaene --theme=ch.Nutzungsplanung --subtheme=ch.SO.NutzungsplanungSondernutzungsplaene --typeCodeList=urn:fdc:ilismeta.interlis.ch:2022:Nutzungsplanung_kantonal_Ueberlagernd_Flaeche

java -jar oereb-iconizer-2.0.10-all.jar --sldUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.Baulinien&SLD_VERSION=1.1.0" --legendGraphicUrl="http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.Baulinien&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300" --fileName=ch.so.arp.oereb_baulinien.symbole.xtf --basketId=ch.so.agi.oereb.legendeneintraege.baulinien --theme=ch.Nutzungsplanung --subtheme=ch.SO.Baulinien --typeCodeList=urn:fdc:ilismeta.interlis.ch:2022:Nutzungsplanung_kantonal_Erschliessung_Linienobjekt
```

Die Datei muss zum dazugehörigen OEREB-GRETL-Job kopiert werden (und ins Repo eingecheckt werden).

## Testrequests

Testrequest GetCapabilites:
```
http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetCapabilities
```

Testrequest GetStyles (Statische Waldgrenzen):
```
http://localhost:8083/wms/oereb-symbols?&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetStyles&LAYERS=ch.StatischeWaldgrenzen&STYLE=default&SLD_VERSION=1.1.0
```

Testrequest GetLegendGraphic (Statische Waldgrenzen):
```
http://localhost:8083/wms/oereb-symbols?&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetLegendGraphic&LAYER=ch.StatischeWaldgrenzen&FORMAT=image/png&STYLE=default&SLD_VERSION=1.1.0
```

Testrequest GetLegendGrapic (Einzelnes Symbol):
```
http://localhost:8083/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.StatischeWaldgrenzen&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300&RULE=in+Bauzonen
```


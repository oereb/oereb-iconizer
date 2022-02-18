package ch.so.agi.oereb.iconizer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Testcontainers
public class Qgis3SymbolTypeCodeCreatorTest {
    Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Container
    private static GenericContainer qgis = new GenericContainer("sogis/oereb-wms:2.0")
            .withExposedPorts(80).waitingFor(Wait.forHttp("/"));

    @Test
    public void getTypeCodeSymbols_Ok() throws Exception {
        String ipAddress = qgis.getContainerIpAddress();
        String port = String.valueOf(qgis.getFirstMappedPort());

        String getStylesRequest = "http://" + ipAddress + ":" + port + "/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetStyles&LAYERS=ch.SO.NutzungsplanungGrundnutzung&SLD_VERSION=1.1.0";
        log.info(getStylesRequest);

        String getLegendGraphicRequest = "http://" + ipAddress + ":" + port + "/wms/oereb-symbols?SERVICE=WMS&REQUEST=GetLegendGraphic&LAYER=ch.SO.NutzungsplanungGrundnutzung&FORMAT=image/png&RULELABEL=false&LAYERTITLE=false&HEIGHT=35&WIDTH=70&SYMBOLHEIGHT=3&SYMBOLWIDTH=6&DPI=300";
        log.info(getLegendGraphicRequest);

        OerebIconizer iconizer = new OerebIconizer();
        List<LegendEntry> legendEntries = iconizer.getSymbols("QGIS3", getStylesRequest, getLegendGraphicRequest);

        assertEquals(39, legendEntries.size());
        
        for (LegendEntry legendEntry : legendEntries) {
            assertEquals(35, legendEntry.getSymbol().getHeight());
            assertEquals(70, legendEntry.getSymbol().getWidth());
            assertEquals(false, legendEntry.getSymbol().isAlphaPremultiplied());
        }
    }
}

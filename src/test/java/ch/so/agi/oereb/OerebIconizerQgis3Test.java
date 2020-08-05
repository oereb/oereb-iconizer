package ch.so.agi.oereb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class OerebIconizerQgis3Test {
    Logger log = LoggerFactory.getLogger(this.getClass());

    private static String WAIT_PATTERN = ".*database system is ready to accept connections.*\\s";

    private static String dbusr = "ddluser";
    private static String dbpwd = "ddluser";
    private static String dbdatabase = "oereb";
    
    @Container
    private static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgisContainerProvider().newInstance()
            .withDatabaseName(dbdatabase).withUsername(dbusr).withPassword(dbpwd).withInitScript("init_postgresql.sql")
            .waitingFor(Wait.forLogMessage(WAIT_PATTERN, 2));
    
    @Container
    private static GenericContainer qgis = new GenericContainer("sogis/oereb2-wms:3.10")
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
        List<LegendEntry> legendEntries = iconizer.getSymbolsQgis3(getStylesRequest, getLegendGraphicRequest);

        assertEquals(39, legendEntries.size());
        
        // Ohne Referenz-Image Grösse etc. prüfen.
        // Sind alle 39 auch images etc.?
    }
}

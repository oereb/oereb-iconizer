package ch.so.agi.oereb;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OerebIconizer {
    Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Gets all the symbols and the according type code from a QGIS v3 WMS server.
     * 
     * @param stylesUrl        GetStyles request url (= SLD file).
     * @param legendGraphicUrl GetLegendGraphic request url with the vendor specific parameters for single symbol support. 
     *                         The RULE parameter is added dynamically. The LAYER parameter must be included.
     * @return                 The symbol and the type code.                
     * @throws Exception
     */
    public List<LegendEntry> getSymbolsQgis3(String stylesUrl, String legendGraphicUrl) throws Exception {
        SymbolTypeCodeCreator styleConfigCreator = new Qgis3SymbolTypeCodeCreator(stylesUrl, legendGraphicUrl);
        List<LegendEntry> legendEntries = styleConfigCreator.create();
        return legendEntries;
    }

}

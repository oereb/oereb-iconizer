package ch.so.agi.oereb.iconizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxWriter;
import ch.interlis.ilirepository.IliManager;
import ch.interlis.iom_j.Iom_jObject;
import ch.interlis.iom_j.xtf.XtfWriter;
import ch.interlis.ili2c.metamodel.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OerebIconizer {
    Logger log = LoggerFactory.getLogger(this.getClass());
    
    /**
     * Gets all the symbols and the according type code from a WMS server.
     * 
     * @param creatorType      SymbolTypeCodeCreator type
     * @param stylesUrl        GetStyles request url (= SLD file).
     * @param legendGraphicUrl GetLegendGraphic request url with the vendor specific parameters for single symbol support. 
     *                         The RULE parameter is added dynamically. The LAYER parameter must be included.
     * @return                 The symbol and the type code.                
     * @throws Exception
     */
    public List<LegendEntry> getSymbols(String creatorType, String stylesUrl, String legendGraphicUrl) throws Exception {
        SymbolTypeCodeCreator symbolTypeCodeCreator = SymbolTypeCodeCreatorFactory.getSymbolTypeCodeCreator(creatorType, stylesUrl, legendGraphicUrl);
        
        List<LegendEntry> legendEntries = symbolTypeCodeCreator.createLegendEntries();
        return legendEntries;
    }

    /**
     * Saves symbols to disk. The type code is the file name.
     * 
     * @param legendEntries List with legend entries (type code, legend text and symbol).
     * @param directory     Directory to save the symbols.
     * @throws IOException 
     * @throws Exception
     */
    public void saveSymbolsToDisk(List<LegendEntry> legendEntries, String directory) throws Exception {
        for (LegendEntry entry : legendEntries) {
            String typeCode = entry.getTypeCode();
            File symbolFile = Paths.get(directory, typeCode + ".png").toFile();
            ImageIO.write(entry.getSymbol(), "png", symbolFile);
        }
    }
    
    /**
     * Creates an INTERLIS transfer file from the legend entries.
     *
     * @param legendEntries List with legend entries (type code and symbol)
     * @param fileName      INTERLIS transfer file which will be created.
     * @param basketId      Basket Id
     * @throws Exception
     */
    public void createXtf(List<LegendEntry> legendEntries, String fileName, String basketId, String typeCodeList, String theme, String subtheme) throws Exception {        
        String ILI_TOPIC = "SO_AGI_OeREB_Legendeneintraege_20211020.Legendeneintraege";
        String ILI_CLASS = ILI_TOPIC+".Legendeneintrag";

        TransferDescription td = null;
        IoxWriter ioxWriter = null;

        td = getTransferDescriptionFromModelName("SO_AGI_OeREB_Legendeneintraege_20211020");
                
        File outputFile = new File(fileName);
        ioxWriter = new XtfWriter(outputFile, td);
        ioxWriter.write(new ch.interlis.iox_j.StartTransferEvent("SOGIS", "", null));
        ioxWriter.write(new ch.interlis.iox_j.StartBasketEvent(ILI_TOPIC, basketId));

        for (int i=0; i<legendEntries.size(); i++) {
            LegendEntry entry = legendEntries.get(i);
            
            Iom_jObject iomObj = new Iom_jObject(ILI_CLASS, String.valueOf(i+1));
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(entry.getSymbol(), "png", baos);
            byte[] symbolInByte = baos.toByteArray();
            String base64Encoded = Base64.getEncoder().encodeToString(symbolInByte);
            iomObj.setattrvalue("Symbol", base64Encoded);
            iomObj.setattrvalue("LegendeTextAggregiert", entry.getLegendText());
            iomObj.setattrvalue("ArtCode", entry.getTypeCode());
            iomObj.setattrvalue("ArtCodeliste", typeCodeList);
            iomObj.setattrvalue("Thema", theme);
            if (subtheme != null) {
                iomObj.setattrvalue("SubThema", subtheme);
            }

            ioxWriter.write(new ch.interlis.iox_j.ObjectEvent(iomObj));            
        }
        
        ioxWriter.write(new ch.interlis.iox_j.EndBasketEvent());
        ioxWriter.write(new ch.interlis.iox_j.EndTransferEvent());
        ioxWriter.flush();
        ioxWriter.close();
    }
    
    private TransferDescription getTransferDescriptionFromModelName(String iliModelName) throws Ili2cException {
        IliManager manager = new IliManager();
        String repositories[] = new String[] { "http://models.interlis.ch/", "http://models.geo.admin.ch/", "https://geo.so.ch/models" };
        manager.setRepositories(repositories);
        ArrayList<String> modelNames = new ArrayList<String>();
        modelNames.add(iliModelName);
        Configuration config = manager.getConfig(modelNames, 2.3);
        ch.interlis.ili2c.metamodel.TransferDescription iliTd = Ili2c.runCompiler(config);

        if (iliTd == null) {
            throw new IllegalArgumentException("INTERLIS compiler failed"); // TODO: can this be tested?
        }
        
        return iliTd;
    }
}

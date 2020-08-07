package ch.so.agi.oereb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

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
    public List<LegendEntry> getSymbols(String creator, String stylesUrl, String legendGraphicUrl) throws Exception {
        SymbolTypeCodeCreator styleConfigCreator = null;
        
        try {
            SymbolTypeCodeCreators.valueOf(creator);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }        
        
        if (SymbolTypeCodeCreators.valueOf(creator) == SymbolTypeCodeCreators.QGIS3) {
            styleConfigCreator = new Qgis3SymbolTypeCodeCreator(stylesUrl, legendGraphicUrl);
        } 
        
        List<LegendEntry> legendEntries = styleConfigCreator.create();
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
     * Updates the symbols in a database table of the according type code.
     * 
     * @param typeCodeSymbols      Map with the type code and the symbols.
     * @param jdbcUrl              JDBC url     
     * @param dbUsr                User name
     * @param dbPwd                Password
     * @param dbSchema             Database schema name.
     * @param dbTable              Database table name.
     * @param typeCodeAttrName     Name of the type code attribute in the database.
     * @param typeCodeListAttrName Name of the type code list attribute in the database.
     * @param typeCodeListValue    Name of the type code list.
     * @param symbolAttrName       Name of the symbol attribute in the database.
     * @param substringMode        If true, the update query will substring(1,3) the type code in the where clause.
     * @throws Exception
     */
    public int updateSymbols(List<LegendEntry> legendEntries, String jdbcUrl, String dbUsr, String dbPwd, String dbSchema, 
            String dbTable, String typeCodeAttrName, String typeCodeListAttrName, String typeCodeListValue, String symbolAttrName, 
            boolean substringMode) throws Exception {
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUsr, dbPwd)) {
            int count = 0;
            for (LegendEntry entry : legendEntries) {
                log.info("TypeCode: " + entry.getTypeCode());
                log.info("LegendText: " + entry.getLegendText());
                log.info("Symbol: " + entry.getSymbol().toString());
                log.info("GeometryType: " + entry.getGeometryType());
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(entry.getSymbol(), "png", baos);
                byte[] symbolInByte = baos.toByteArray();
                String base64Encoded = Base64.getEncoder().encodeToString(symbolInByte);

                Statement stmt = conn.createStatement();
                String sql = "";
                
                if (substringMode) {
                    sql = "UPDATE " + dbSchema + "." + dbTable + " SET " + symbolAttrName + " = decode('"+base64Encoded+"', 'base64') WHERE substring(" + typeCodeAttrName + ", 1, 3) = '"+entry.getTypeCode()+"' AND "+typeCodeListAttrName+" LIKE '"+typeCodeListValue+"%';"; 

                } else {
                    sql = "UPDATE " + dbSchema + "." + dbTable + " SET " + symbolAttrName + " = decode('"+base64Encoded+"', 'base64') WHERE " + typeCodeAttrName + " = '"+entry.getTypeCode()+"' AND "+typeCodeListAttrName+" LIKE '"+typeCodeListValue+"%';"; 
                }
                log.info(sql);
                
                int c = stmt.executeUpdate(sql);
                count = count + c;
            }
            log.info("Number of updated records: " + String.valueOf(count));
            return count;
        } catch (SQLException e) {
            e.printStackTrace();
            log.error(e.getMessage());
            throw new Exception(e);
        }
    }   
}

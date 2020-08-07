package ch.so.agi.oereb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class UpdateSymbolsTest {
    Logger log = LoggerFactory.getLogger(this.getClass());

    private static String WAIT_PATTERN = ".*database system is ready to accept connections.*\\s";

    private static String dbusr = "ddluser";
    private static String dbpwd = "ddluser";
    private static String dbdatabase = "oereb";

    @Container
    private static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgisContainerProvider().newInstance()
            .withDatabaseName(dbdatabase).withUsername(dbusr).withPassword(dbpwd).withInitScript("init_postgresql.sql")
            .waitingFor(Wait.forLogMessage(WAIT_PATTERN, 2));

    @Test
    public void updateSymbol_Ok() throws Exception {
        String schemaName = "insertsymbols".toLowerCase();
        String tableName = "test".toLowerCase();
        String typeCodeAttrName = "artcode";
        String symbolAttrName = "symbol";
        
        Connection con = null;
        
        String typeCode = "N390";
        File symbolFile = new File("src/test/data/weitere_schutzzone_ausserhalb_bauzone.png");
  
        try {
            // Prepare database: create table.
            con = connect(postgres);
            createOrReplaceSchema(con, schemaName);
            
            Statement s1 = con.createStatement();
            s1.execute("CREATE TABLE " + schemaName + "." + tableName + "(t_id SERIAL, artcode TEXT, artcodeliste TEXT, symbol BYTEA, legendetext TEXT);");
            s1.execute("INSERT INTO " + schemaName + "." + tableName + "(artcode, artcodeliste) VALUES('" + typeCode +"', 'GrundnutzungListe.2601');");
            s1.close();
            con.commit();
            closeConnection(con);
                        
            // Insert/update typecode and symbol with the iconizer.
            List<LegendEntry> legendEntries = new ArrayList<LegendEntry>();
            LegendEntry entry = new LegendEntry();
            entry.setTypeCode(typeCode);
            entry.setSymbol(ImageIO.read(symbolFile));
            legendEntries.add(entry);
                        
            OerebIconizer iconizer = new OerebIconizer();
            int count = iconizer.updateSymbols(legendEntries, postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), schemaName, tableName, typeCodeAttrName, "artcodeliste", "GrundnutzungListe", symbolAttrName, false);

            // Check if everything is ok.
            con = connect(postgres);
            Statement s2 = con.createStatement();
            ResultSet rs = s2.executeQuery("SELECT artcode, symbol FROM " + schemaName + "." + tableName);
            
            if(!rs.next()) {
                fail();
            }
            
            assertEquals(1, count);
            assertEquals(typeCode, rs.getString(1));
                        
            ByteArrayInputStream bis = new ByteArrayInputStream(rs.getBytes(2));
            BufferedImage bim = ImageIO.read(bis);            
            assertEquals(ImageIO.read(symbolFile).getHeight(), bim.getHeight());
            assertEquals(ImageIO.read(symbolFile).getWidth(), bim.getWidth());
            assertEquals(ImageIO.read(symbolFile).isAlphaPremultiplied(), bim.isAlphaPremultiplied());
                        
            if(rs.next()) {
                fail();
            }
            
            rs.close();
            s2.close();
        } finally {
            closeConnection(con);
        }
    }
    
    /*
     * Tries to update a symbol which does not exist in the database.
     */
    @Test
    public void updateNonExistingSymbol_Ok() throws Exception {
        String schemaName = "insertsymbols".toLowerCase();
        String tableName = "test".toLowerCase();
        String typeCodeAttrName = "artcode";
        String symbolAttrName = "symbol";
        
        Connection con = null;
        
        String typeCode = "N390";
        File symbolFile = new File("src/test/data/weitere_schutzzone_ausserhalb_bauzone.png");
        String legendText = "weitere Schutzzonen ausserhalb Bauzonen";
  
        try {
            // Prepare database: create table.
            con = connect(postgres);
            createOrReplaceSchema(con, schemaName);
            
            Statement s1 = con.createStatement();
            s1.execute("CREATE TABLE " + schemaName + "." + tableName + "(t_id SERIAL, artcode TEXT, artcodeliste TEXT, symbol BYTEA);");
            s1.execute("INSERT INTO " + schemaName + "." + tableName + "(artcode, artcodeliste) VALUES('" + typeCode +"', 'GrundnutzungListe.2601');");
            s1.close();
            con.commit();
            closeConnection(con);
                        
            // Insert typecode and symbol with the iconizer.
            List<LegendEntry> legendEntries = new ArrayList<LegendEntry>();
            LegendEntry entryNonExisting = new LegendEntry();
            entryNonExisting.setTypeCode("N999");
            entryNonExisting.setSymbol(ImageIO.read(symbolFile));
            entryNonExisting.setLegendText(legendText);
            legendEntries.add(entryNonExisting);
            
            LegendEntry entryExisting = new LegendEntry();
            entryExisting.setTypeCode(typeCode);
            entryExisting.setSymbol(ImageIO.read(symbolFile));
            entryExisting.setLegendText(legendText);
            legendEntries.add(entryExisting);

            OerebIconizer iconizer = new OerebIconizer();
            int count = iconizer.updateSymbols(legendEntries, postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), schemaName, tableName, typeCodeAttrName, "artcodeliste", "GrundnutzungListe", symbolAttrName, false);

            assertEquals(1, count);
        } finally {
            closeConnection(con);
        }
    }    


    private Connection connect(PostgreSQLContainer postgres) {
        Connection con = null;
        try {
            String url = postgres.getJdbcUrl();
            String user = postgres.getUsername();
            String password = postgres.getPassword();

            con = DriverManager.getConnection(url, user, password);

            con.setAutoCommit(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return con;
    }

    private void createOrReplaceSchema(Connection con, String schemaName) {

        try {
            Statement s = con.createStatement();
            s.addBatch(String.format("DROP SCHEMA IF EXISTS %s CASCADE", schemaName));
            s.addBatch("CREATE SCHEMA " + schemaName);
            s.addBatch(String.format("GRANT USAGE ON SCHEMA %s TO dmluser", schemaName));
            s.addBatch(String.format("GRANT USAGE ON SCHEMA %s TO readeruser", schemaName));
            s.executeBatch();
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void closeConnection(Connection con) {
        try {
            if (con != null)
                con.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

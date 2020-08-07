package ch.so.agi.oereb;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.concurrent.Callable;

//@Command(name = "checksum", mixinStandardHelpOptions = true, version = "checksum 4.0", description = "Prints the checksum (MD5 by default) of a file to STDOUT.")
//class Main implements Callable<Integer> {
//
//    @Parameters(index = "0", arity = "1", description = "The file whose checksum to calculate.")
//    private File file;
//
//    @Option(names = { "-a", "--algorithm" }, description = "MD5, SHA-1, SHA-256, ...")
//    private String algorithm = "MD5";
//
//    // this example implements Callable, so parsing, error handling
//    // and handling user requests for usage help or version help
//    // can be done with one line of code.
//    public static void main(String... args) {
//        int exitCode = new CommandLine(new Main()).execute(args);
//        System.exit(exitCode);
//    }
//
//    @Override
//    public Integer call() throws Exception { // the business logic...
//        byte[] data = Files.readAllBytes(file.toPath());
//        byte[] digest = MessageDigest.getInstance(algorithm).digest(data);
//        String format = "%0" + (digest.length * 2) + "x%n";
//        System.out.printf(format, new BigInteger(1, digest));
//        return 0;
//    }
//}



import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "oereb-iconizer", 
    mixinStandardHelpOptions = false,
    description = "Creates symbols from a WMS GetLegendGraphics response and saves them in a database table."
)
public class Main implements Callable<Integer> {

    @Option(names = "--sldUrl", paramLabel = "url", required = true, description = "GetStyles request url.")
    String sldUrl = null;
    
    @Option(names = "--legendGraphicUrl", paramLabel = "url", required = true, description = "GetLegendGraphics request url.")
    String legendGraphicUrl = null;
    
    @Option(names = "--downloadDir", paramLabel = "dir", required = false, description = "Download directoy.")
    String downloadDir = System.getProperty("java.io.tmpdir");

    @ArgGroup(exclusive = false, multiplicity = "0..1")
    Database database;

    static class Database {
        @Option(names = "--dbhost", paramLabel = "host", required = true, description = "Database host.") 
        String dbhost = null;
        
        @Option(names = "--dbdatabase", paramLabel = "name", required = true, description = "Database name.")
        String dbdatabase = null;
        
        @Option(names = "--dbport", paramLabel = "port", required = true, description = "Database port.")
        String dbport = null;
        
        @Option(names = "--dbusr", paramLabel = "user", required = true, description = "Database user name.")
        String dbusr = null;
        
        @Option(names = "--dbpwd", paramLabel = "password", required = true, description = "Database password.")
        String dbpwd = null;

        @Option(names = "--dbschema", paramLabel = "name", required = true, description = "Database schema.")
        String dbschema = null;

        @Option(names = "--dbtable", paramLabel = "name", required = true, description = "Database table.")
        String dbtable = null;
        
        @Option(names = "--typeCodeAttrName", paramLabel = "attribute name", required = true, description = "Name of type code attribute in the table.")
        String typeCodeAttrName = null;
        
        @Option(names = "--typeCodeListAttrName", paramLabel = "attribute name", required = true, description = "Name of the type code list attribute in the table.")
        String typeCodeListAttrName = null;
        
        @Option(names = "--typeCodeListValue", paramLabel = "attribute value", required = true, description = "Name of the type code list.")
        String typeCodeListValue = null;
        
        @Option(names = "--symbolAttrName", paramLabel = "attribute name", required = true, description = "Name of symbol attribute in table.")
        String symbolAttrName = null;
        
        @Option(names = "--substringMode", required = false, description = "Use substring mode.")
        boolean substringMode = false;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        
        if (database != null) {            
            String jdbcUrl = "jdbc:postgresql://" + database.dbhost + ":" + database.dbport + "/" + database.dbdatabase;
            OerebIconizer iconizer = new OerebIconizer();
            List<LegendEntry> legendEntries =  iconizer.getSymbols("QGIS3", sldUrl, legendGraphicUrl);
            iconizer.updateSymbols(legendEntries, jdbcUrl, database.dbusr, database.dbpwd, database.dbschema, database.dbtable, 
                    database.typeCodeAttrName, database.typeCodeListAttrName, database.typeCodeListValue, database.symbolAttrName, 
                    database.substringMode);
        } else {
            OerebIconizer iconizer = new OerebIconizer();
            List<LegendEntry> legendEntries =  iconizer.getSymbols("QGIS3", sldUrl, legendGraphicUrl);
            iconizer.saveSymbolsToDisk(legendEntries, downloadDir);
            return 0;
        }
        return 0;
    }
}

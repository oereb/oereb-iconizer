package ch.so.agi.oereb.iconizer;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

import java.util.List;

import picocli.CommandLine.ArgGroup;

@Command(
    name = "oereb-iconizer", 
    mixinStandardHelpOptions = true,
    description = "Creates symbols from a WMS GetLegendGraphics response, saves them on the file system and creates INTERLIS transfer file."
)
public class App implements Callable<Integer> {

    @Option(names = "--sldUrl", paramLabel = "url", required = true, description = "GetStyles request url.")
    String sldUrl = null;
    
    @Option(names = "--legendGraphicUrl", paramLabel = "url", required = true, description = "GetLegendGraphics request url.")
    String legendGraphicUrl = null;
        
    @Option(names = "--downloadDir", paramLabel = "dir", required = false, description = "Download directoy.")
    String downloadDir = System.getProperty("java.io.tmpdir");

    @ArgGroup(exclusive = false, multiplicity = "0..1")
    Xtf xtf;

    static class Xtf {
        @Option(names = "--fileName", paramLabel = "name", required = true, description = "File name.") 
        String fileName = null;

        @Option(names = "--basketId", paramLabel = "bid", required = false, description = "Basket id.") 
        String basketId = "ch.so.agi.oereb.legendeneintraege";
        
        @Option(names = "--typeCodeList", paramLabel = "name", required = true, description = "Type code list.")
        String typeCodeList = null;
        
        @Option(names = "--theme", paramLabel = "name", required = true, description = "Theme.")
        String theme = null;
        
        @Option(names = "--subtheme", paramLabel = "name", required = false, description = "Subtheme.")
        String subtheme = null;        
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (xtf != null) {
            OerebIconizer iconizer = new OerebIconizer();
            List<LegendEntry> legendEntries =  iconizer.getSymbols("QGIS3", sldUrl, legendGraphicUrl);
            iconizer.createXtf(legendEntries, xtf.fileName, xtf.basketId, xtf.typeCodeList, xtf.theme, xtf.subtheme);
        } else {
            OerebIconizer iconizer = new OerebIconizer();
            List<LegendEntry> legendEntries = iconizer.getSymbols("QGIS3", sldUrl, legendGraphicUrl);
            iconizer.saveSymbolsToDisk(legendEntries, downloadDir);
        }
        
        return 0;
    }
}
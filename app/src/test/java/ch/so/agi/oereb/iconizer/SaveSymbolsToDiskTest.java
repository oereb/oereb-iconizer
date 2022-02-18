package ch.so.agi.oereb.iconizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SaveSymbolsToDiskTest {
    
    @Test
    public void saveSymbolsToDisk_Ok(@TempDir Path tempDir) throws Exception {
        String directory = tempDir.toFile().getAbsolutePath();
        String typeCode = "N111".toLowerCase();
        File symbolFile = new File("src/test/data/gruen_und_freihaltezone_innerhalb_bauzone.png");

        LegendEntry entry = new LegendEntry();
        entry.setTypeCode(typeCode);
        entry.setSymbol(ImageIO.read(symbolFile));
        
        List<LegendEntry> legendEntries = new ArrayList<LegendEntry>();
        legendEntries.add(entry);
                
        OerebIconizer iconizer = new OerebIconizer();
        iconizer.saveSymbolsToDisk(legendEntries, directory);

        File resultFile = Paths.get(tempDir.toFile().getAbsolutePath(), "N111.png".toLowerCase()).toFile();
        BufferedImage resultImage = ImageIO.read(resultFile);

        assertEquals(ImageIO.read(symbolFile).getHeight(), resultImage.getHeight());
        assertEquals(ImageIO.read(symbolFile).getWidth(), resultImage.getWidth());
        assertEquals(ImageIO.read(symbolFile).isAlphaPremultiplied(), resultImage.isAlphaPremultiplied());
    }

    // see: https://bugs.openjdk.java.net/browse/JDK-8196123 and https://stackoverflow.com/questions/11153200/with-imageio-write-api-call-i-get-nullpointerexception
    // ImageIO.write throws a NPE if it cannot write the file and not a FileNotFoundException.
    @Test
    public void saveSymbolsToDisk_Permission_Fail() throws Exception {
        String directory = "/";
        String typeCode = "N111".toLowerCase();
        File symbolFile = new File("src/test/data/gruen_und_freihaltezone_innerhalb_bauzone.png");

        LegendEntry entry = new LegendEntry();
        entry.setTypeCode(typeCode);
        entry.setSymbol(ImageIO.read(symbolFile));
        
        List<LegendEntry> legendEntries = new ArrayList<LegendEntry>();
        legendEntries.add(entry);
        
        try {
            OerebIconizer iconizer = new OerebIconizer();
            iconizer.saveSymbolsToDisk(legendEntries, directory);

        } catch (Exception e) {            
            // do nothing
        }
    }
}

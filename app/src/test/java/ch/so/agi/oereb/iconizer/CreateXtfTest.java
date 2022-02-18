package ch.so.agi.oereb.iconizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.ehi.basics.settings.Settings;
import org.interlis2.validator.Validator;

public class CreateXtfTest {
    @Test
    public void saveSymbolsToDisk_Ok(@TempDir Path tempDir) throws Exception {
        String directory = tempDir.toFile().getAbsolutePath();
        String fileName = Paths.get(directory, "ch.so.agi.oereb.legendeneintraege.xtf").toFile().getAbsolutePath();
        //String fileName = Paths.get("/Users/stefan/tmp/", "ch.so.agi.oereb.legendeneintraege.xtf").toFile().getAbsolutePath();
        
        String typeCode = "N111";
        File symbolFile = new File("src/test/data/gruen_und_freihaltezone_innerhalb_bauzone.png");

        LegendEntry entry = new LegendEntry();
        entry.setTypeCode(typeCode);
        entry.setLegendText("Grün- und Freihaltezone (innerhalb Bauzone)");
        entry.setSymbol(ImageIO.read(symbolFile));
        
        List<LegendEntry> legendEntries = new ArrayList<LegendEntry>();
        legendEntries.add(entry);
                
        OerebIconizer iconizer = new OerebIconizer();
        iconizer.createXtf(legendEntries, fileName, "ch.so.agi.oereb.legendeneintraege", "urn:fdc:ilismeta.interlis.ch:2017:Typ_Kanton_Waldgrenzen", "ch.StatischeWaldgrenzen", null);

        
        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_ILIDIRS, Validator.SETTING_DEFAULT_ILIDIRS);
        boolean valid = Validator.runValidation(fileName, settings);
        assertTrue(valid);

        String content = new String(Files.readAllBytes(Paths.get(fileName)));
        assertTrue(content.contains("iVBORw0KGgoAAAANSUhEUgAAAEYAAA"));
        assertTrue(content.contains("Grün- und Freihaltezone (innerhalb Bauzone)"));
        assertTrue(content.contains("N111"));
        assertTrue(content.contains("urn:fdc:ilismeta.interlis.ch:2017:Typ_Kanton_Waldgrenzen"));
        assertTrue(content.contains("ch.StatischeWaldgrenzen"));
    }
}

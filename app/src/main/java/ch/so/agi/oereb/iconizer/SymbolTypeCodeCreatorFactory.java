package ch.so.agi.oereb.iconizer;

public class SymbolTypeCodeCreatorFactory {

    public static SymbolTypeCodeCreator getSymbolTypeCodeCreator(String creatorType, String stylesUrl, String legendGraphicUrl) {
        if (creatorType == null) {
            return null;
        }
        
        if(creatorType.equalsIgnoreCase("QGIS3")){
            return new Qgis3SymbolTypeCodeCreator(stylesUrl, legendGraphicUrl);
            
        }
        
        return null;
    }
}

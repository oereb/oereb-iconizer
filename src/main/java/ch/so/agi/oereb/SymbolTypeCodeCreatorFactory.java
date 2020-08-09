package ch.so.agi.oereb;

public class SymbolTypeCodeCreatorFactory {

    public static SymbolTypeCodeCreator getSymbolTypeCodeCreatory(String creatorType, String stylesUrl, String legendGraphicUrl) {
        if (creatorType == null) {
            return null;
        }
        
        if(creatorType.equalsIgnoreCase("QGIS3")){
            return new Qgis3SymbolTypeCodeCreator(stylesUrl, legendGraphicUrl);
            
        }
        
        return null;
    }
}

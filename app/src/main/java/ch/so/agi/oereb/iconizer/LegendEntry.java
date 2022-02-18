package ch.so.agi.oereb.iconizer;

import java.awt.image.BufferedImage;

public class LegendEntry {    
    private BufferedImage symbol;
    private String typeCode;
    private String typeCodeList;
    private String legendText;
    // TODO: Braucht es das?
    private String geometryType;

    public BufferedImage getSymbol() {
        return symbol;
    }
    public void setSymbol(BufferedImage symbol) {
        this.symbol = symbol;
    }
    public String getTypeCode() {
        return typeCode;
    }
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }
    public String getTypeCodeList() {
        return typeCodeList;
    }
    public void setTypeCodeList(String typeCodeList) {
        this.typeCodeList = typeCodeList;
    }
    public String getLegendText() {
        return legendText;
    }
    public void setLegendText(String legendText) {
        this.legendText = legendText;
    }
    public String getGeometryType() {
        return geometryType;
    }
    public void setGeometryType(String geometryType) {
        this.geometryType = geometryType;
    }    
}

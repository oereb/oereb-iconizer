package ch.so.agi.oereb;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Qgis3SymbolTypeCodeCreator implements SymbolTypeCodeCreator {
    Logger log = LoggerFactory.getLogger(this.getClass());
    
    private String geometryType;

    private String stylesUrl = null;
    private String legendGraphicUrl = null;
    
    public Qgis3SymbolTypeCodeCreator(String stylesUrl, String legendGraphicUrl) {
        this.stylesUrl = stylesUrl;
        this.legendGraphicUrl = legendGraphicUrl;
    }
    
    @Override
    public List<LegendEntry> create() throws Exception {
        List<LegendEntry> legendEntries = new ArrayList<LegendEntry>();
        
        // Save GetStyles response to a file.
        File sldFile = null;
        try {
            String decodedRequest = java.net.URLDecoder.decode(stylesUrl, "UTF-8");
            CloseableHttpClient httpclient = HttpClients.custom()
                    .setRedirectStrategy(new LaxRedirectStrategy()) // adds HTTP REDIRECT support to GET and POST methods 
                    .build();
            HttpGet get = new HttpGet(new URL(decodedRequest).toURI()); 
            CloseableHttpResponse response = httpclient.execute(get);
            
            Path tempDir = Files.createTempDirectory("oereb2_iconizer_");
            sldFile = Paths.get(tempDir.toAbsolutePath().toFile().getAbsolutePath(), "getstyles.sld").toFile();
            
            InputStream source = response.getEntity().getContent();
            FileUtils.copyInputStreamToFile(source, sldFile);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new Exception("configuration file is null");
        }
               
        // Zuerst werden mit xpath alle se:Rule-Elemente gesucht. Anschliessend
        // wird jedes se:Rule-Element prozessiert und der Rule-Name (se:Name)
        // und der TypeCode-Wert werden gelesen.
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        Document document = builder.parse(sldFile);
        
        log.info(document.getDocumentURI());

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        HashMap<String, String> prefMap = new HashMap<String, String>() {{
            put("se", "http://www.opengis.net/se");
            put("ogc", "http://www.opengis.net/ogc");
            put("sld", "http://www.opengis.net/sld");
        }};
        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
        xpath.setNamespaceContext(namespaces);

        XPathExpression expr = xpath.compile("//se:FeatureTypeStyle/se:Rule");

        Object result = expr.evaluate(document, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        
        for (int i = 0; i < nodes.getLength(); i++) {            
            Rule rule = evaluateRule(nodes.item(i));
            if (rule != null) {
                String typeCodeValue = rule.getTypeCodeValue();
                String ruleName = URLEncoder.encode(rule.getRuleName(), "UTF-8");

                String requestUrl = legendGraphicUrl + "&RULE=" + ruleName;
                log.info(requestUrl);

                BufferedImage symbol = getRemoteImage(requestUrl);

                LegendEntry legendEntry = new LegendEntry();
                legendEntry.setTypeCode(typeCodeValue);
                legendEntry.setLegendText(rule.getRuleName()); // Darf in OEREB v2 nicht mehr verwendet werden beim Update der DB-Records.
                legendEntry.setSymbol(symbol);
                legendEntry.setGeometryType(geometryType);

                legendEntries.add(legendEntry);
            }
        }
        return legendEntries;
    }
    
    /*
     * Ein <se:Rule> Element muss ein <se:Name> Element und ein <ogc:Filter> Element vom Typ <ogc:PropertyIsEqualTo> haben.
     */
    private Rule evaluateRule(Node node) throws Exception {
        String ruleName = null;
        String typeCodeValue = null;

        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node childNode = nodes.item(i);              
            if (childNode.getLocalName() != null && childNode.getLocalName().equalsIgnoreCase("Name")) {
                ruleName = childNode.getTextContent();
            }
            
            if (childNode.getLocalName() != null && childNode.getLocalName().equalsIgnoreCase("Filter")) {
                typeCodeValue = evaluateFilter(childNode);
            }
        }
        
        if (ruleName == null || typeCodeValue == null) {
            return null;
        }
        log.debug(ruleName + " " + typeCodeValue);
        return new Rule(ruleName, typeCodeValue);
    }
    
    /*
     * Hier folgt der sehr spezifische Teil: Der TypeCode/Artcode des OEREB-Rahmenmodells ist der Wert
     * des <Literal> Elements im <PropertyIsEqualTo> Filter. Die <ogc:Function> kann im Prinzip 
     * beliebig kompliziert sein. 
     * Ausnahme ist der "Substring"-Modus: In diesem Fall entspricht der Wert des <Literal> Elements 
     * nur einem Substring des TypeCodes/Artcodes. Man muss aber zum jetzigen Zeitpunkt noch nicht 
     * wissen, dass später der Substring-Modus gilt. Hier und jetzt werden bloss "dumm" alle Symbole eines 
     * WMS-Layers hergestellt.
     */
    private String evaluateFilter(Node node) {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node childNode = nodes.item(i);
            if (childNode.getLocalName() != null && childNode.getLocalName().equalsIgnoreCase("PropertyIsEqualTo")) {
                NodeList filterChildNodes = childNode.getChildNodes();
                for (int j = 0; j < filterChildNodes.getLength(); j++) {
                    Node filterChildNode = filterChildNodes.item(j);
                    if (filterChildNode.getLocalName() != null && filterChildNode.getLocalName().equalsIgnoreCase("Literal")) {
                        String typeCodeValue = filterChildNode.getTextContent();
                        log.debug(typeCodeValue);
                        return typeCodeValue;
                    } 
                }
            }            
        }
        return null;
    }
    
    /*
     * Saves a remote image (the symbol) as BufferedImage.
     */
    private BufferedImage getRemoteImage(String url) throws Exception {
        try {
            CloseableHttpClient httpclient = HttpClients.custom()
                    .setRedirectStrategy(new LaxRedirectStrategy()) // adds HTTP REDIRECT support to GET and POST methods 
                    .build();
            HttpGet get = new HttpGet(new URL(url).toURI()); 
            CloseableHttpResponse response = httpclient.execute(get);
            
            InputStream inputStream = response.getEntity().getContent();
            BufferedImage image = ImageIO.read(inputStream);

            // force 3 band image
            BufferedImage fixedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = (Graphics2D) fixedImage.getGraphics();
            g.setBackground(Color.WHITE);
            g.clearRect(0, 0, image.getWidth(), image.getHeight());   
            g.drawImage(image, 0, 0, null);               
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(fixedImage, "png", baos); 
            baos.flush();
            baos.close();            
            return fixedImage;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }
}

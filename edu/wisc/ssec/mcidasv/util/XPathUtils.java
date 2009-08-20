package edu.wisc.ssec.mcidasv.util;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.IdvResourceManager.XmlIdvResource;
import ucar.unidata.util.ResourceCollection.Resource;
import ucar.unidata.xml.XmlResourceCollection;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.util.Contract;

/**
 * Documentation is still forthcoming, but remember that <b>no methods accept 
 * {@code null} parameters!</b>
 */
public final class XPathUtils {

    /** Maps (and caches) the XPath {@link String} to its compiled {@link XPathExpression}. */
    private static final Map<String, XPathExpression> pathMap = new ConcurrentHashMap<String, XPathExpression>();

    /**
     * Thou shalt not create an instantiation of this class!
     */
    private XPathUtils() {}

    public static XPathExpression expr(String xPath) {
        Contract.notNull(xPath, "Cannot compile a null string");

        XPathExpression expr = pathMap.get(xPath);
        if (expr == null) {
            try {
                expr = XPathFactory.newInstance().newXPath().compile(xPath);
                pathMap.put(xPath, expr);
            } catch (XPathExpressionException e) {
                throw new RuntimeException("Error compiling xpath", e);
            }
        }
        return expr;
    }

    public static List<Node> eval(final XmlResourceCollection collection, final String xPath) {
        Contract.notNull(collection, "Cannot search a null resource collection");
        Contract.notNull(xPath, "Cannot search using a null XPath query");

        try {
            List<Node> nodeList = new ArrayList<Node>();
            XPathExpression expression = expr(xPath);

            // Resources are the only things added to the list returned by 
            // getResources().
            @SuppressWarnings("unchecked")
            List<Resource> files = collection.getResources();

            for (int i = 0; i < files.size(); i++) {
                if (!collection.isValid(i))
                    continue;

                File f = new File(XPathUtils.class.getResource(files.get(i).toString()).toURI());
                if (!f.exists() || !f.canRead())
                    continue;

                NodeList tmpList = (NodeList)expression.evaluate(loadXml(f), XPathConstants.NODESET);
                for (int j = 0; j < tmpList.getLength(); j++) {
                    nodeList.add(tmpList.item(j));
                }
            }
            return nodeList;
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluating xpath", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error loading file", e);
        }
    }

    public static NodeList eval(final String xmlFile, final String xPath) {
        Contract.notNull(xmlFile, "Null path to a XML file");
        Contract.notNull(xPath, "Cannot search using a null XPath query");

        try {
            return (NodeList)expr(xPath).evaluate(loadXml(xmlFile), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluation xpath", e);
        }
    }

    public static NodeList eval(final Node root, final String xPath) {
        Contract.notNull(root, "Cannot search a null root node");
        Contract.notNull(xPath, "Cannot search using a null XPath query");

        try {
            return (NodeList)expr(xPath).evaluate(root, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluation xpath", e);
        }
    }

    public static List<Node> nodes(final XmlIdvResource collectionId, final String xPath) {
        Contract.notNull(collectionId);
        Contract.notNull(xPath);

        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv == null)
            throw new AssertionError("Could not get a valid reference to McIDASV!");

        XmlResourceCollection collection = mcv.getResourceManager().getXmlResources(collectionId);
        return nodes(collection, xPath);
    }
    
    public static List<Node> nodes(final XmlResourceCollection collection, final String xPath) {
        Contract.notNull(collection);
        Contract.notNull(xPath);
        return eval(collection, xPath);
    }

    public static NodeListIterator nodes(final String xmlFile, final String xPath) {
        Contract.notNull(xmlFile);
        Contract.notNull(xPath);
        return new NodeListIterator(eval(xmlFile, xPath));
    }

    public static NodeListIterator nodes(final Node root, final String xPath) {
        Contract.notNull(root);
        Contract.notNull(xPath);
        return new NodeListIterator(eval(root, xPath));
    }

    public static NodeListIterator nodes(final Node root) {
        Contract.notNull(root);
        return nodes(root, "//*");
    }

    public static List<Element> elements(final IntegratedDataViewer idv, final XmlIdvResource collectionId, final String xPath) {
        Contract.notNull(idv);
        Contract.notNull(collectionId);
        Contract.notNull(xPath);

        XmlResourceCollection collection = idv.getResourceManager().getXmlResources(collectionId);
        return elements(collection, xPath);
    }

    public static List<Element> elements(final XmlResourceCollection collection, final String xPath) {
        Contract.notNull(collection);
        Contract.notNull(xPath);
        List<Element> elements = new ArrayList<Element>();
        for (Node n : eval(collection, xPath))
            elements.add((Element)n);
        return elements;
    }

    public static ElementListIterator elements(final String xmlFile, final String xPath) {
        Contract.notNull(xmlFile);
        Contract.notNull(xPath);
        return new ElementListIterator(eval(xmlFile, xPath));
    }

    public static ElementListIterator elements(final Node root) {
        Contract.notNull(root);
        return elements(root, "//*");
    }

    public static ElementListIterator elements(final Node root, final String xPath) {
        Contract.notNull(root);
        Contract.notNull(xPath);
        return new ElementListIterator(eval(root, xPath));
    }

    public static Document loadXml(final String xmlFile) {
        Contract.notNull(xmlFile);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(xmlFile);
        } catch (Exception e) {
            throw new RuntimeException("Error loading XML file", e);
        }
    }

    public static Document loadXml(final File xmlFile) {
        Contract.notNull(xmlFile);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(xmlFile);
        } catch (Exception e) {
            throw new RuntimeException("Error loading XML file: "+e.getMessage(), e);
        }
    }

    public static class NodeListIterator implements Iterable<Node>, Iterator<Node> {
        private final NodeList nodeList;
        private int index = 0;

        public NodeListIterator(final NodeList nodeList) {
            Contract.notNull(nodeList);
            this.nodeList = nodeList;
        }

        public Iterator<Node> iterator() {
            return this;
        }

        public boolean hasNext() {
            return (index < nodeList.getLength());
        }

        public Node next() {
            return nodeList.item(index++);
        }

        public void remove() {
            throw new UnsupportedOperationException("not implemented");
        }
    }

    public static class ElementListIterator implements Iterable<Element>, Iterator<Element> {
        private final NodeList nodeList;
        private int index = 0;

        public ElementListIterator(final NodeList nodeList) {
            Contract.notNull(nodeList);
            this.nodeList = nodeList;
        }

        public Iterator<Element> iterator() {
            return this;
        }

        public boolean hasNext() {
            return (index < nodeList.getLength());
        }

        public Element next() {
            return (Element)nodeList.item(index++);
        }

        public void remove() {
            throw new UnsupportedOperationException("not implemented");
        }
    }
}

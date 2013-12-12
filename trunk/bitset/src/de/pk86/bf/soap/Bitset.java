
package de.pk86.bf.soap;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.8
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "bitset", targetNamespace = "http://pk86.de/bitset", wsdlLocation = "http://pk86.de/bitdemo/soap?wsdl")
public class Bitset
    extends Service
{

    private final static URL BITSET_WSDL_LOCATION;
    private final static WebServiceException BITSET_EXCEPTION;
    private final static QName BITSET_QNAME = new QName("http://pk86.de/bitset", "bitset");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://pk86.de/bitdemo/soap?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        BITSET_WSDL_LOCATION = url;
        BITSET_EXCEPTION = e;
    }

    public Bitset() {
        super(__getWsdlLocation(), BITSET_QNAME);
    }

    public Bitset(WebServiceFeature... features) {
        super(__getWsdlLocation(), BITSET_QNAME, features);
    }

    public Bitset(URL wsdlLocation) {
        super(wsdlLocation, BITSET_QNAME);
    }

    public Bitset(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, BITSET_QNAME, features);
    }

    public Bitset(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public Bitset(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns ObjectItemSOAPService
     */
    @WebEndpoint(name = "bitset")
    public de.pk86.bf.ObjectItemSOAPService getBitset() {
        return super.getPort(new QName("http://pk86.de/bitset", "bitset"), de.pk86.bf.ObjectItemSOAPService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ObjectItemSOAPService
     */
    @WebEndpoint(name = "bitset")
    public ObjectItemSOAPService getBitset(WebServiceFeature... features) {
        return super.getPort(new QName("http://pk86.de/bitset", "bitset"), ObjectItemSOAPService.class, features);
    }

    private static URL __getWsdlLocation() {
        if (BITSET_EXCEPTION!= null) {
            throw BITSET_EXCEPTION;
        }
        return BITSET_WSDL_LOCATION;
    }

}

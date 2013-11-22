
package de.pk86.bf.soap;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the de.pk86.bf.soap package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetPrevPageString_QNAME = new QName("http://pk86.de/bitset", "getPrevPageString");
    private final static QName _HasNext_QNAME = new QName("http://pk86.de/bitset", "hasNext");
    private final static QName _EndSession_QNAME = new QName("http://pk86.de/bitset", "endSession");
    private final static QName _GetNextPageString_QNAME = new QName("http://pk86.de/bitset", "getNextPageString");
    private final static QName _CreateSession_QNAME = new QName("http://pk86.de/bitset", "createSession");
    private final static QName _GetPrevPageStringResponse_QNAME = new QName("http://pk86.de/bitset", "getPrevPageStringResponse");
    private final static QName _HasSession_QNAME = new QName("http://pk86.de/bitset", "hasSession");
    private final static QName _GetNextPageStringResponse_QNAME = new QName("http://pk86.de/bitset", "getNextPageStringResponse");
    private final static QName _CreateSessionResponse_QNAME = new QName("http://pk86.de/bitset", "createSessionResponse");
    private final static QName _HasNextResponse_QNAME = new QName("http://pk86.de/bitset", "hasNextResponse");
    private final static QName _EndSessionResponse_QNAME = new QName("http://pk86.de/bitset", "endSessionResponse");
    private final static QName _HasSessionResponse_QNAME = new QName("http://pk86.de/bitset", "hasSessionResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: de.pk86.bf.soap
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetNextPageStringResponse }
     * 
     */
    public GetNextPageStringResponse createGetNextPageStringResponse() {
        return new GetNextPageStringResponse();
    }

    /**
     * Create an instance of {@link HasSession }
     * 
     */
    public HasSession createHasSession() {
        return new HasSession();
    }

    /**
     * Create an instance of {@link GetPrevPageStringResponse }
     * 
     */
    public GetPrevPageStringResponse createGetPrevPageStringResponse() {
        return new GetPrevPageStringResponse();
    }

    /**
     * Create an instance of {@link CreateSession }
     * 
     */
    public CreateSession createCreateSession() {
        return new CreateSession();
    }

    /**
     * Create an instance of {@link HasNext }
     * 
     */
    public HasNext createHasNext() {
        return new HasNext();
    }

    /**
     * Create an instance of {@link GetPrevPageString }
     * 
     */
    public GetPrevPageString createGetPrevPageString() {
        return new GetPrevPageString();
    }

    /**
     * Create an instance of {@link GetNextPageString }
     * 
     */
    public GetNextPageString createGetNextPageString() {
        return new GetNextPageString();
    }

    /**
     * Create an instance of {@link EndSession }
     * 
     */
    public EndSession createEndSession() {
        return new EndSession();
    }

    /**
     * Create an instance of {@link EndSessionResponse }
     * 
     */
    public EndSessionResponse createEndSessionResponse() {
        return new EndSessionResponse();
    }

    /**
     * Create an instance of {@link HasSessionResponse }
     * 
     */
    public HasSessionResponse createHasSessionResponse() {
        return new HasSessionResponse();
    }

    /**
     * Create an instance of {@link HasNextResponse }
     * 
     */
    public HasNextResponse createHasNextResponse() {
        return new HasNextResponse();
    }

    /**
     * Create an instance of {@link CreateSessionResponse }
     * 
     */
    public CreateSessionResponse createCreateSessionResponse() {
        return new CreateSessionResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPrevPageString }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "getPrevPageString")
    public JAXBElement<GetPrevPageString> createGetPrevPageString(GetPrevPageString value) {
        return new JAXBElement<GetPrevPageString>(_GetPrevPageString_QNAME, GetPrevPageString.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HasNext }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "hasNext")
    public JAXBElement<HasNext> createHasNext(HasNext value) {
        return new JAXBElement<HasNext>(_HasNext_QNAME, HasNext.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndSession }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "endSession")
    public JAXBElement<EndSession> createEndSession(EndSession value) {
        return new JAXBElement<EndSession>(_EndSession_QNAME, EndSession.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNextPageString }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "getNextPageString")
    public JAXBElement<GetNextPageString> createGetNextPageString(GetNextPageString value) {
        return new JAXBElement<GetNextPageString>(_GetNextPageString_QNAME, GetNextPageString.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateSession }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "createSession")
    public JAXBElement<CreateSession> createCreateSession(CreateSession value) {
        return new JAXBElement<CreateSession>(_CreateSession_QNAME, CreateSession.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPrevPageStringResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "getPrevPageStringResponse")
    public JAXBElement<GetPrevPageStringResponse> createGetPrevPageStringResponse(GetPrevPageStringResponse value) {
        return new JAXBElement<GetPrevPageStringResponse>(_GetPrevPageStringResponse_QNAME, GetPrevPageStringResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HasSession }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "hasSession")
    public JAXBElement<HasSession> createHasSession(HasSession value) {
        return new JAXBElement<HasSession>(_HasSession_QNAME, HasSession.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNextPageStringResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "getNextPageStringResponse")
    public JAXBElement<GetNextPageStringResponse> createGetNextPageStringResponse(GetNextPageStringResponse value) {
        return new JAXBElement<GetNextPageStringResponse>(_GetNextPageStringResponse_QNAME, GetNextPageStringResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateSessionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "createSessionResponse")
    public JAXBElement<CreateSessionResponse> createCreateSessionResponse(CreateSessionResponse value) {
        return new JAXBElement<CreateSessionResponse>(_CreateSessionResponse_QNAME, CreateSessionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HasNextResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "hasNextResponse")
    public JAXBElement<HasNextResponse> createHasNextResponse(HasNextResponse value) {
        return new JAXBElement<HasNextResponse>(_HasNextResponse_QNAME, HasNextResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndSessionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "endSessionResponse")
    public JAXBElement<EndSessionResponse> createEndSessionResponse(EndSessionResponse value) {
        return new JAXBElement<EndSessionResponse>(_EndSessionResponse_QNAME, EndSessionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HasSessionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pk86.de/bitset", name = "hasSessionResponse")
    public JAXBElement<HasSessionResponse> createHasSessionResponse(HasSessionResponse value) {
        return new JAXBElement<HasSessionResponse>(_HasSessionResponse_QNAME, HasSessionResponse.class, null, value);
    }

}

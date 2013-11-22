package de.pk86.bf;

import java.rmi.RemoteException;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
@WebService
(     serviceName = "bitset",
		portName = "bitset", 
		targetNamespace = "http://pk86.de/bitset"
) 
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
public interface ObjectItemSOAPService {
	/**
	 * Erzeugt eine Session mit einer Ergebnismenge zu dem angegebenen Ausdruck.
	 * Anschließend kann mit der gelieferten sessionId die Ergebnismenge angerufen werden.
	 * @param expression
	 * @return
	 * @throws RemoteException
	 */
	public int createSession(String expression) throws RemoteException;
	public String getNextPageString(int sessionId);
	public String getPrevPageString(int sessionId);
	public boolean hasNext(int sessionId);
	/**
	 * Liefert true, wenn eine Session mit der angegebenen Id existiert
	 * vor {@link #endSession(int)} aufrufen.
	 * @param sessionId
	 * @return
	 */
	public boolean hasSession(int sessionId);
	/**
	 * Beendet die angegebene Session; die von ihr gehaltenen Ressourcen werden freigegeben.
	 * @param sessionId
	 * @return false, wenn die angegebene Session nicht (mehr) existiert.
	 */
	public boolean endSession(int sessionId);
}

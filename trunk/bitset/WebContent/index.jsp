<%@ page import = "de.pk86.bf.client.*, de.pk86.bf.*" %>
<jsp:useBean id="bitdemo" class="de.pk86.bf.client.BitdemoBean" scope="session"/>
<jsp:setProperty name="bitdemo" property="*"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="de" lang="de">
<head><title>Address Search Engine Demo</title></head>
<body bgcolor="white">
<!--Local IP: <%=request.getLocalAddr() %> Remote IP: <%=request.getRemoteAddr() %> --> 
<% bitdemo.processRequest(request); %>
<h2>Personen Suchanfrage</h2>
  <form method="post">
  Suchbegriffe: <sup>[1]</sup><br/> 
  <textarea cols="100" rows="5" name="searchPattern" maxlength="4000"><%= bitdemo.getSearchPattern() %></textarea> <p/>
  <input type="submit" name="action" value="Suchen"/> Anzahl Treffer: <%= bitdemo.getResultSetSize() %> Dauer: <%= bitdemo.getDuration() %><p/>    
  Suchergebnis: <br/> 
  <textarea cols="100" rows="20" name="searchResult" readonly="readonly"><%= bitdemo.getSearchResult() %></textarea> <p/>
  </form>
  
  <form method="post" style="display:inline"><input type="submit" name="action" value="zur&uuml;ck"/> <%=bitdemo.getPointer() %></form> 
  <form method="post" style="display:inline"><input type="submit" name="action" value="weiter"/></form>
  <form method="post" style="display:inline"><input type="submit" name="action" value="statistic"/></form>
  <p/>
  <sup>[1]</sup> Operatoren: | ! ^ ( )<p/>
  <hr />
  <a href="edit.jsp">Daten editieren</a>  <a href="import.jsp">Daten importieren</a>
</body>
</html>

<%@ page import = "de.pk86.bf.client.*, de.pk86.bf.*" contentType="text/html; charset=UTF-8" %>
<jsp:useBean id="importb" class="de.pk86.bf.client.ImportBean" scope="session"/>
<jsp:setProperty name="importb" property="*"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="de" lang="de">
<head><title>Address Search Engine Demo</title></head>
<body bgcolor="white">
<% importb.processRequest(request); %>
<h2>Daten importieren</h2>
  <form method="post">
  Datensätze zeilenweise angeben; Attribute getrennt durch Leerzeichen sowie: ;.,()/-<br/> 
  <textarea cols="100" rows="20" name="importData" maxlength="4000"><%= importb.getImportData() %></textarea> <p/>
  <input type="submit" name="action" value="importieren"/> Anzahl Datensätze importiert: <%= importb.getDataSize() %> Dauer: <%= importb.getDuration() %><p/>    
  </form>
  <p/>
  <hr />
  <a href="index.jsp"> Daten suchen </a>
</body>
</html>

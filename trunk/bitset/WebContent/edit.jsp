<%@ page import = "de.pk86.bf.client.*, de.pk86.bf.*" %>
<jsp:useBean id="edit" class="de.pk86.bf.client.EditBean" scope="session"/>
<jsp:setProperty name="edit" property="*"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="de" lang="de">
<head>
<style type="text/css">
table.myTable { border-collapse:collapse; }
table.myTable td, table.myTable th { border:1px solid black; }
</style>
<title>Address Search Engine Demo</title></head>
<body bgcolor="white">
<% edit.processRequest(request); %>
<h2>Daten editieren</h2>
  <form method="get">
	<table class="myTable">		
		<tr>
			<th>löschen</th>
			<th>ID</th>
			<th>Content</th>
		</tr>
		<% for(int i = 0; i < edit.getPage().getRowCount(); i++) { %>
		<tr>
			<td align="center"> <input type="checkbox" name="checked<%="["+ i +"]"%>"/> </td>
			<td align="center"> <%= edit.getPage().getChildRow(i).getValue("obid")+"["+ i +"]" %> </td>
			<td><input type ="text" name="content<%="["+ i +"]"%>" size="100" value='<%= edit.getPage().getChildRow(i).getValue("content") %>' /></td>
		</tr>
		<% } %>
	</table>
	<p/>
	<input type="submit" name="action" value="speichern"/> <%= edit.getSubmitResult() %>
  </form>
  <p/>
  <hr />
  <a href="index.jsp"> Daten suchen </a>
</body>
</html>

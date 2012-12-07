<%@ page import = "de.pk86.client.*, de.pk86.bf.*" %>

<jsp:useBean id="bitdemo" class="de.pk86.bf.client.BitdemoBean" scope="session"/>
<jsp:setProperty name="bitdemo" property="*"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="de" lang="de">
<head><title>Address Search Engine Demo</title></head>
<body bgcolor="white">

<h2>Personen Suchanfrage</h2>

  <form method="get">
  Suchbegriffe: <p/> 
  <textarea cols="100" rows="5" name="searchPattern"><%= bitdemo.getSearchPattern() %></textarea> <p/>
  Anzahl Treffer: <%= bitdemo.getResultSetSize() %> Dauer: <%= bitdemo.getDuration() %>    
  <input type="submit" value="Suchen"/><p/>
  </form>

  <form method="post">
  Suchergebnis: <p/> 
  <textarea cols="100" rows="20" name="searchResult"><%= bitdemo.getSearchResult() %></textarea> <p/>
  <input type="submit" name="prev" value="zur&uuml;ck"/>
  <input type="submit" name="more" value="weiter"/>
  </p>
  </form>

</body>
</html>

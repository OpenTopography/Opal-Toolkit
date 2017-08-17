<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">


<head>
	<title>Opal2 Server Connections</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
 	<link rel="stylesheet" type="text/css" media="all" href="css/style.css"/> 
    <link rel="stylesheet" type="text/css" media="all" href="css/style-maintag.css"/>
</head>

<% 
   String systemServerHostname = (String) request.getAttribute("systemServerHostname"); 
   //String opalDocumentation = (String) request.getAttribute("opalDocumentation");
//   response.sendRedirect("http://" + systemServerHostname + "/phpSysInfo");
%>

<body > 
<div class="mainBody">

<!-- [headerInclude] -->
<%@ include file="header.jsp" %>

<!-- [/headerInclude] -->

<!-- BEGIN Body -->
<table border="0" cellpadding="0" cellspacing="0" width="950" class="boxContainer" align="center">
<tr>
<td width="15" height="15" class="boxTopLeft colColor"></td>
<td class="leftCol boxTop colColor"></td>
<td class="boxTop colColor"></td><td class="rightCol boxTop colColor"></td>
<td width="15" class="boxTopRight colColor"></td>
</tr>
<tr>
<td class="boxLeft colColor"><br /></td>
<td colspan="3" class="colColor">

<h2>server connections by ip address:</h2>
<a href="testchart?type=ip&unit=days&duration=30&width=1200&height=600"> 
  <img src="testchart?bgcolor=%23fcf2d7&type=ip&unit=hours&duration=24&width=900&height=400" width="900"/>
</a>

<br/>

</td>

<td class="boxRight colColor"><br /></td>
</tr>
<tr>
<td width="15" height="15" class="boxBottomLeft colColor"></td>
<td class="leftCol boxBottom colColor"><br /></td>
<td class="boxBottom colColor"><br /></td><td class="rightCol boxBottom colColor"><br /></td>
<td width="15" class="boxBottomRight colColor"></td>
</tr>
</table>
<!-- END Body -->

<br />

<!-- BEGIN Footer -->

<!-- END Footer -->
</div>
</body>
</html>

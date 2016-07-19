<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">


<head>
	<title>Opal2 Server Deployment Information</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
 	<link rel="stylesheet" type="text/css" media="all" href="css/style.css"/> 
    <link rel="stylesheet" type="text/css" media="all" href="css/style-maintag.css" />
    <script src="scripts/jquery.js" language="javascript" type="text/javascript" ></script>
    <script src="scripts/jquery.corner.js" language="javascript" type="text/javascript" ></script>
    <script type="text/javascript">
        $(document).ready(function() {
            $("#list-nav ul li.left a").corner("tl bl 10px  cc:#fff");
            $("#list-nav ul li.right a").corner("tr br 10px cc:#fff");
        });
    </script>

</head>

<% 
   String systemServerHostname = request.getServerName(); 
   String systemIPAddress = (String) request.getAttribute("systemIPAddress"); 
   String systemUptime = (String) request.getAttribute("systemUptime"); 
   String systemBuildDate = (String) request.getAttribute("systemBuildDate"); 
   String opalVersion = (String) request.getAttribute("opalVersion");  
/* not userd anymore!
   String dbUrl = (String) request.getAttribute("dbURL");
   String dbUsername = (String) request.getAttribute("dbUsername"); */
   String dbDriver = (String) request.getAttribute("dbDriver");
   String opalWebsite = (String) request.getAttribute("opalWebsite");
   String opalDocumentation = (String) request.getAttribute("opalDocumentation");

   String submissionSystem = (String) request.getAttribute("submissionSystem");
   
   String opalDataLifetime = (String) request.getAttribute("opalDataLifetime");
   
%>

<body> 
<div class="mainBody">
<%@ include file="header.jsp" %>
<!-- Navigation Menu Bar -->
<table border="0" class="mainnav" cellpadding="0" cellspacing="0">
<tr>
  <td>
    <div id="list-nav">
    <ul>
      <li class="left"><a href="dashboard">Home</a></li>
      <li><a href="dashboard?command=serverInfo"class="active">Server Info</a></li>
      <li><a href="dashboard?command=serviceList">List of applications</a></li>
      <li><a href="dashboard?command=statistics">Usage Statistics</a></li>
      <li class="right"><a href="dashboard?command=docs">Documentation</a></li>
    </ul>
    </div>
  </td>
</tr>
</table> 
<br>

<!-- BEGIN Body -->
<table border="0" cellpadding="0" cellspacing="0" width="950" class="boxContainer" align="center">
<tr>
  <td width="15" height="15" class="boxTopLeft colColor"></td>
  <td class="leftCol boxTop colColor"></td>
  <td class="boxTop colColor"></td>
  <td class="rightCol boxTop colColor"></td>
  <td width="15" class="boxTopRight colColor"></td>
</tr>

<tr>
<td class="boxLeft colColor"><br /></td>
<td colspan="3" class="colColor">
  <table border="0" width="100%">
	<tr>
	 	<td colspan="2" class="leftCol boxBody colColor">
	    	<h3 class="Title">Opal Server Host Info</h3>
        </td>
 	</tr>
	<tr>
		<td width=25% class="boxBodyRight colColor infoTitle">Hostname:</td>
		<td class="boxBody colColor">&nbsp;<%= systemServerHostname %></td>
	</tr>
<%
if ( (systemIPAddress != null) && (! systemIPAddress.equals("null")) ) {
%>
	<tr>
		<td class="boxBodyRight colColor infoTitle">IP Address:</td>
		<td class="boxBody colColor"><%= systemIPAddress %></td>
	</tr>
<% 
}
%>
	<tr>
		<td class="boxBodyRight colColor infoTitle">Build Date:</td>
		<td class="boxBody colColor"><%= systemBuildDate %></td>
	</tr>
	<tr>
		<td class="boxBodyRight colColor infoTitle">Uptime:</td>
		<td class="boxBody colColor"><%= systemUptime %></td>
	</tr>
	<tr/>
	<tr>
	 	<td colspan="2" class="leftCol boxBody colColor"> &nbsp; </td>
 	</tr>
	<tr>
	 	<td colspan="2" class="leftCol boxBody colColor">
		    <h3 class="Title">Opal Server Configuration</h3>
		</td>
 	</tr>
    <tr>
        <td class="boxBodyRight colColor infoTitle">Opal Version:</td>
        <td class="boxBody colColor"><%= opalVersion %></td>
    </tr>   
 <!--  unsafe to display this infomation  <tr>
        <td class="boxBodyRight colColor infoTitle">Data base URL:</td>
        <td class="boxBody colColor"></td>
    </tr>
   <tr>
        <td class="boxBodyRight colColor infoTitle">Data base username:</td>
        <td class="boxBody colColor"></td>
    </tr>   -->
    <tr>
        <td class="boxBodyRight colColor infoTitle">Data base driver:</td>
        <td class="boxBody colColor"><%= dbDriver %></td>
    </tr>
    
    <tr>
        <td class="boxBodyRight colColor infoTitle">Submission system:</td>
        <td class="boxBody colColor"><%= submissionSystem %></td>
    </tr>

<% if ( (opalDataLifetime != null) && (opalDataLifetime.length() > 1) ) { %>
    <tr>
        <td class="boxBodyRight colColor infoTitle">User data lifetime:</td>
        <td class="boxBody colColor"><%= opalDataLifetime %></td>
    </tr>
    <% } %>
  </table>
</td>
<td class="boxRight colColor"><br /></td>
</tr>
<%@ include file="footer.jsp" %>
</table> <!-- END Body -->
<%@ include file="copyright.jsp" %>

</div>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <title>Opal2: A Toolkit for Scientific Software as a Service.</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="keywords" content="enter, keywords, here" />
    <meta name="description" content="enter keywords here" />
    <link rel="stylesheet" type="text/css" media="all" href="css/style.css"/> 
    <link rel="stylesheet" type="text/css" media="all"href="css/style-maintag.css"/>
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
   String systemServerHostname =  request.getServerName();
   String opalWebsite = (String) request.getAttribute("opalWebsite");
   String opalDocumentation = (String) request.getAttribute("opalDocumentation");

%>

<body >

<div class="mainBody">

<!-- [headerInclude] -->
<%@ include file="header.jsp" %>
<!-- Navigation Menu Bar -->
<table border="0" class="mainnav" cellpadding="0" cellspacing="0">
<tr>
  <td>
    <div id="list-nav" >
    <ul>
      <li class="left"><a href="dashboard" class="active">Home</a></li>
      <li><a href="dashboard?command=serverInfo">Server Info</a></li>
      <li><a href="dashboard?command=serviceList">List of applications</a></li>
      <li><a href="dashboard?command=statistics">Usage Statistics</a></li>
      <li class="right"><a href="dashboard?command=docs">Documentation</a></li>
    </ul>
	</div>
  </td>
</tr>
</table>
<br>

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
    <!-- add content here... -->
    <table border="0" width="100%" cellpadding="2px" >
      <tr>
        <td colspan="3" class="boxBody colColor">
		<span class="infoTitle"> Opal2</span> is a toolkit for wrapping scientific applications as Web 
       services.  It leverages open standards and toolkits, such as DRMAA, Condor and the Globus toolkit,
       for cluster job management, standards-based Grid security and data management, in an easy to use 
		and highly configurable manner.  Opal is released under the 
		<a href="http://www.nbcr.net/data/docs/opal/LICENSE.txt"><span class="PubTitle">BSD License</span></a>
		<br>
		<hr>
		</td>
      </tr>
      <tr>
        <td valign="top" width="38%" class="boxBody colColor"><span class="infoTitle">The
		Opal Dashboard </span>
		provides a simple interface for job submission and monitoring. The key features at a glance: 
		   <ul class="menu1">
		     <li><span class="Content">Automatically generated web interfaces for scientific applications</span> </li>
		     <li><span class="Content">Registry and keyword-based searches for deployed applications</span> </li>
		     <li><span class="Content">Monitoring  and reporting of usage statistics</span> </li>
		     <li><span class="Content">Tracking job progress and retrieval of results</span> </li>
		   </ul>
		<br>
		</td>
		<td class="colColor rightColVertBar">
        <td valign="top" class="boxBody colColor"> 
		<span class="infoTitle">How to use </span>
		Click on the tabs in the navigation bar at the top of the page. 
		   <ul class="menu2">
		     <li><a href="./dashboard"><span class="PubTitle">Home</span></a> - this page </li>
		     <li><a href="./dashboard?command=serverInfo">
			     <span class="PubTitle">Server Info</span></a> - information about the server hosting the Opal services</li>

		     <li><a href="./dashboard?command=serviceList">
			     <span class="PubTitle">List of Applications</span></a> 
				 - a registry of applications available to the user</li>
		     <li><a href="./dashboard?command=statistics">
			     <span class="PubTitle">Usage Statistics</span></a> -
			     statistics for the applications deployed on this server</li>
		     <li><a href="./dashboard?command=docs">
			     <span class="PubTitle">Documentation</span></a> - Opal system documentation, tutorials, support</li>
		   </ul>
		<br>
		</td>
      </tr>

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

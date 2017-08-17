<%--
 copy the license here
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-nested" prefix="nested" %>

<%@page import="java.util.Enumeration"%>
<%@page import="edu.sdsc.nbcr.opal.JobSubOutputType"%>
<%@page import="edu.sdsc.nbcr.opal.StatusOutputType"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Opal2 job submission result</title>
    <link href="css/style.css" media="all" rel="stylesheet" type="text/css" /> 
    <script src="scritps/scripts.js" language="javascript" type="text/javascript" ></script>
	<script src="scripts/jquery.js" language="javascript" type="text/javascript" ></script>
	<script src="scripts/jquery.corner.js" language="javascript" type="text/javascript" ></script>
    <script type="text/javascript">
        $(document).ready(function() {
            $("#list-nav ul li.left a").corner("tl bl 10px  cc:#fff");
            $("#list-nav ul li.right a").corner("tr br 10px cc:#fff");
        });
    </script>
<%
StatusOutputType status =  (StatusOutputType) request.getAttribute("status");
String serviceID = (String)request.getAttribute("serviceID");
String jobId = (String)request.getAttribute("jobId");
if (status.getCode() != 8  && status.getCode() != 4) { 
%>
  <script language="JavaScript"> 
    function refresh() { window.location.reload( true ); }
    setTimeout("refresh()", 30*1000); 
  </script> 
<% } %>

</head>

<body>
<div class="mainBody">

<jsp:include page="../dashboard-jsp/header.jsp"/>
<!-- Navigation Menu Bar -->
<table border="0" class="mainnav" cellpadding="0" cellspacing="0">
<tr>
  <td>
    <div id="list-nav">
    <ul>
      <li class="left"><a href="dashboard">Home</a></li>
      <li><a href="dashboard?command=serverInfo">Server Info</a></li>
      <li><a href="dashboard?command=serviceList"  class="active">List of applications</a></li>
      <li><a href="dashboard?command=statistics">Usage Statistics</a></li>
      <li class="right"><a href="dashboard?command=docs">Documentation</a></li>
    </ul>
    </div>
  </td>
</tr>
</table>
<br>

<jsp:include page="header.jsp"/>

    <h2>Submission results for <%= serviceID %></h2>

<table border="0" cellspacing="10">
    <tr>
	  <td>Date and time :</td>
	  <td><script type="text/javascript"> 
	          document.write('<b>'+(new Date).toLocaleString()+'</b>'); 
		  </script> 
	  </td>
	</tr>
    <tr>
	  <td>JobId :</td>
	  <td> <%= jobId %></td>
	</tr>
    <tr>
	  <td>Status code:</td>
	  <td><%= status.getCode() %></td>
    </tr>
    <tr>
	  <td>Message:</td>
	  <td><%= status.getMessage() %></td>
	</tr>
    <tr>
	  <td>Output Base URL:</td>
	  <td><a href="<%= status.getBaseURL() %>"  target="_blank" ><%= status.getBaseURL() %></a></td>
    </tr> 
</table>

<br/>
<jsp:include page="footer.jsp"/>


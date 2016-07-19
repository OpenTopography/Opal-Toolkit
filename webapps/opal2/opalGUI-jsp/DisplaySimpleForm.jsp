<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-nested" prefix="nested" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@page import="java.util.Enumeration"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@page import="edu.sdsc.nbcr.opal.gui.common.AppMetadata"%>
<%@page import="org.apache.struts.upload.FormFile"%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<title>Opal2 <bean:write name="appMetadata" property="serviceName" /> submission form </title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link href="css/style.css" media="all" rel="stylesheet" type="text/css" /> 
    <script src="scripts/scripts.js" language="javascript" type="text/javascript" ></script> 
    <script src="scripts/jquery.js" language="javascript" type="text/javascript" ></script>
    <script src="scripts/jquery.corner.js" language="javascript" type="text/javascript" ></script>
    <script type="text/javascript">
        $(document).ready(function() {
            $("#list-nav ul li.left a").corner("tl bl 10px  cc:#fff");
            $("#list-nav ul li.right a").corner("tr br 10px cc:#fff");
        });
    </script>

<% 
// for coloring rows
int rowN=0;
String rowVal[] = new String[2];
rowVal[0] = "odd"; 
rowVal[1] = "even";

// for uploading files
AppMetadata app = (AppMetadata) request.getSession(false).getAttribute("appMetadata");
FormFile [] files = app.getFiles();
String index = "" + (files.length - 1);
%>

<script language="javascript">
<!--
var count = 1;
function addFileInput() {
   var newfile = document.createElement("input");
   newfile.type = "file";
   newfile.name = "inputFile[" + count + "]";
   newfile.size = "40";
   var d = document.createElement("div");
   d.appendChild(newfile);
   fplace = document.getElementById("moreUploads").appendChild(d);
   count++;
}

var state = 'none';
function makeTrue(){ }

function showHide(layer_ref) {
    if (state == 'block') {
        state = 'none';
    }
    else {
        state = 'block';
    }
    if (document.all) { //IS IE 4 or 5 (or 6 beta)
        eval( "document.all." + layer_ref + ".style.display = state");
    }
    if (document.layers) { //IS NETSCAPE 4 or below
        document.layers[layer_ref].display = state;
    }
    if (document.getElementById &&!document.all) {
        hza = document.getElementById(layer_ref);
        hza.style.display = state;
    }
}
//-->
</script> 
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

<h2 id="centered">
   <span class="Label"><bean:write name="appMetadata" property="serviceName" /> submission form </span> 
</h2>
<p><span class="Require">*</span> Required parameters.</p>

<html:form action="LaunchJob.do" enctype="multipart/form-data" >
<table class="groupings" border="0" cellspacing="0" cellpadding="5">

    <tr class="<%=rowVal[rowN%2]%>"> <% rowN += 1; %>
	   <td>Insert command line here:</td>
	   <td><html:text property="cmdLine" size="50"/></td>
	</tr>
    <tr class="<%=rowVal[rowN%2]%>"> <% rowN += 1; %>
	   <td>Insert user email for status notification:</td>
	   <td><html:text property="userEmail" size="50"/></td>
	</tr>
    <logic:equal name="appMetadata" property="parallel" value="true">
      <tr class="<%=rowVal[rowN%2]%>"> <% rowN += 1; %>
	  <td>Insert number of CPU for parallel application:</td><td><html:text property="numCpu" size="50"/></td>
	  </tr>
    </logic:equal>
    <tr class="<%=rowVal[rowN%2]%>"> <% rowN += 1; %>
       <td>Should input files be unzipped on server?</td><td><nested:checkbox property="extractInputs"/></td>
    </tr> 
    <tr class="<%=rowVal[rowN%2]%>"> 
	   <td>Choose input file:</td>
	   <td>
	<div id="moreUploads"> 
	       <input type="file" name="inputFile[0]" value="" size="40"
	       onchange="document.getElementById('moreUploadsLink').style.display = 'block';" />
	</div>
	<div id="moreUploadsLink" style="display:none;">
	     <a href="javascript:addFileInput();"><span class="Title">Attach another File</span></a>
	</div>
	   </td>
	</tr>
    <html:hidden property="addFile" value="false" />
    <tr class="<%=rowVal[rowN%2]%>"> 
	   <td> </td> 
	   <td> 
	   </td> 
	</tr>

	<!-- submit and reset button  -->
	<tr>
	  <td align="right"> <button class="Submit" type="submit">  
	                 <Img src="images/tick.png" alt="" />&nbsp;Submit</button> </td>
	  <td align="left"> <button class="Reset" type="reset" onClick="window.location.reload()">
					 <Img src="images/cross.png" alt="" />&nbsp;Reset</button> </td>
	</tr>

</table>
</html:form>

<span class="Hide" onclick="showHide('help')">Show/Hide help (click)</span>
<div id="help" style="display: none;">
    <p class="manual" >Application Description: <bean:write name="appMetadata" property="usage"/> </p>
    <logic:notEmpty name="appMetadata" property="info">
    <p class="manual" >Usage Info:</br>
        <logic:iterate id="infoString" name="appMetadata" property="info"> 
            <pre> <c:out value="<%= infoString %>"> </c:out> </pre> 
        </logic:iterate>
    </p>
    </logic:notEmpty>
</div>
</br>


<br/>
<jsp:include page="footer.jsp"/>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@page import="java.net.InetAddress" %>
<%@page import="java.net.URLEncoder" %>
<%@page import="edu.sdsc.nbcr.opal.dashboard.util.DateHelper" %>
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
	<title>Opal2 Server Dashboard Usage Statistics</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

 	<link rel="stylesheet" type="text/css" media="all" href="css/style.css"/> 
    <link rel="stylesheet" type="text/css" media="all" href="css/style-maintag.css" />

    <script src="scripts/jquery.js" language="javascript" type="text/javascript" ></script>
    <script src="scripts/jquery.corner.js" language="javascript" type="text/javascript" ></script>
	<script language="javascript" type="text/javascript" >

        function uncheckAll(){
            var selectedElem = document.getElementsByName("servicesName");
            for (i=0; i < selectedElem.length; i++){
            selectedElem[i].checked=false;
            }//for
        }

        function checkAll(){
            var selectedElem = document.getElementsByName("servicesName");
            for (i=0; i < selectedElem.length; i++){
                selectedElem[i].checked=true;
            }//for  
        }	
	</script>

    <script type="text/javascript">
        $(document).ready(function() {
            $("#list-nav ul li.left a").corner("tl bl 10px  cc:#fff");
            $("#list-nav ul li.right a").corner("tr br 10px cc:#fff");
        });
	</script>

</head>

<% 
   String systemServerHostname =  request.getServerName();
   String startDate = (String) request.getAttribute("startDate");
   String endDate = (String) request.getAttribute("endDate");
   //String opalDocumentation = (String) request.getAttribute("opalDocumentation");

   String [] servicesName = (String []) request.getAttribute("servicesName");
   String [] servicesNameSelected = (String []) request.getAttribute("servicesNameSelected");
   String serviceNameURL = "";
   //let create the URL for the selected services...
   for ( int i = 0; i < servicesNameSelected.length; i++ ) {
       serviceNameURL += "&servicesName=" + servicesNameSelected[i];
   }
%>

<body > 
<div class="mainBody">

<!-- [headerInclude] -->
<%@ include file="header.jsp" %>
<!-- Navigation Menu Bar -->
<table border="0" class="mainnav" cellpadding="0" cellspacing="0">
<tr>
  <td>
    <div id="list-nav">
    <ul>
      <li class="left"><a href="dashboard" >Home</a></li>
      <li><a href="dashboard?command=serverInfo" >Server Info</a></li>
      <li><a href="dashboard?command=serviceList">List of applications</a></li>
      <li><a href="dashboard?command=statistics"class="active">Usage Statistics</a></li>
      <li class="right"><a href="dashboard?command=docs">Documentation</a></li>
    </ul>
    </div>
  </td>
</tr>
</table> 
<br>

<!-- [/headerInclude] -->

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

<h2><center>Server usage statistics</center></h2>
<form action="dashboard" method="get">

<table width="100%" border="0">
    <input type="hidden" name="command" value="statistics"/>
    <tr>
        <td width="25%"> </td>
        <td width="25%"> </td>
        <td width="25%"> </td>
        <td width="25%"> </td>
    </tr>
    <tr>
        <td colspan="2" ><b>Select services to display:</b>
           <input type=button name="CheckAll" value="Check All" onClick="checkAll()"/> 
            &nbsp;
           <input type=button name="UnCheckAll" value="Uncheck All" onClick="uncheckAll()"> 
        </td>
        <td width="25%"><b>Start date:</b> <input type="text" name="startDate" value="<%= startDate %>"/></td>
        <td width="25%"><b>End date:</b> <input type="text" name="endDate" value="<%= endDate %>"/></td>
    </tr>

<%
   for ( int i = 0; i < servicesName.length; i++ ) {
%>
        <%if ( i % 4  == 0 ) { %> <tr> <%}%>

        <td>
        <% if ( DateHelper.containsString(servicesNameSelected, servicesName[i]) ) {%>
        <input checked="checked"  type="checkbox" name="servicesName" value="<%= servicesName[i] %>"  />
        <%} else {%>
        <input type="checkbox" name="servicesName" value="<%= servicesName[i] %>"  />
        <%}%>
        <%= servicesName[i] %>
        </td>

        <%if ( i % 4  == 3 ) { %> </tr> <%}%>
<% 
   }
%>
    <tr>
        <td colspan="4"><input type="submit"  value="Update Charts"/></td>
        <br>
    </tr>
    <tr> </tr>
    
</table>
</form>
</td>

<td class="boxRight colColor"></td>
</tr>

<!-- running jobs  -->
<tr>
<td class="boxLeft colColor"></td>
<td colspan="3" class="colColor">
   <% String href = "plotchart?type=runningjobs&width=800&height=400" + serviceNameURL; %>
   <center><a href="<%= href %>"><img src="<%= href %>"/></a></center>
   <br/> 
</td>
<td class="boxRight colColor"></></td>
</tr>

<!-- hits chart -->
<tr>
<td class="boxLeft colColor"></td>
<td colspan="3" class="colColor">
   <% href = "plotchart?type=hits&width=800&height=400&startDate=" + 
             URLEncoder.encode(startDate, "UTF-8") + "&endDate=" + 
             URLEncoder.encode(endDate, "UTF-8") +  serviceNameURL; %>
   <center><a href="<%= href %>"><img src="<%= href %>"/></a></center>
   <br/>
</td>
<td class="boxRight colColor"></td>
</tr>

<!-- average execution time chart -->
<tr>
<td class="boxLeft colColor"></td>
<td colspan="3" class="colColor">
   <% href = "plotchart?type=exectime&width=800&height=400&startDate=" + 
             URLEncoder.encode(startDate, "UTF-8") + "&endDate=" + 
             URLEncoder.encode(endDate, "UTF-8") +  serviceNameURL; %>
   <center><a href="<%= href %>"><img src="<%= href %>"/></a></center>
   <br/>
</td>
<td class="boxRight colColor"></td>
</tr>

<!-- number of errors  -->
<tr>
<td class="boxLeft colColor"></td>
<td colspan="3" class="colColor">
   <% href = "plotchart?type=error&width=800&height=400&startDate=" + 
             URLEncoder.encode(startDate, "UTF-8") + "&endDate=" + 
             URLEncoder.encode(endDate, "UTF-8") +  serviceNameURL; %>
   <center><a href="<%= href %>"><img src="<%= href %>"/></a></center>
   <br/>
</td>
<td class="boxRight colColor"></td>
</tr>

<%@ include file="footer.jsp" %>
</table> <!-- END Body -->
<%@ include file="copyright.jsp" %>
</div>
</body>
</html>

<%--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at
   
         http://www.apache.org/licenses/LICENSE-2.0
   
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
String tomcatUrl = (String) request.getAttribute("tomcatUrl");
String opalUrl = (String) request.getAttribute("opalUrl");
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Opal2 Server Available Applications</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link rel="stylesheet" type="text/css" media="all" href="css/ext-all.css" />
    <link rel="stylesheet" type="text/css" media="all" href="css/feed-viewer.css" /> 
	<link rel="stylesheet" type="text/css" media="all" href="css/style-maintag.css"/>
    <link rel="stylesheet" type="text/css" media="all" href="css/style.css"/>  
    <script src="scripts/ext-base.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/ext-all.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/RowExpander.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/jquery.js" language="javascript" type="text/javascript" ></script>
    <script src="scripts/jquery.corner.js" language="javascript" type="text/javascript" ></script>
    <script type="text/javascript">
        $(document).ready(function() {
            $("#list-nav ul li.left a").corner("tl bl 10px  cc:#fff");
            $("#list-nav ul li.right a").corner("tr br 10px cc:#fff");
        });
    </script>

</head>

<body>
<script type="text/javascript" >
   <%@ include file="/scripts/services-panel.js" %>
</script>

<div class="mainBody">
<jsp:include page="header.jsp"></jsp:include>
<!-- Navigation Menu Bar -->
<table border="0" class="mainnav" cellpadding="0" cellspacing="0">
<tr>
  <td>
    <div id="list-nav">
    <ul>
      <li class="left"><a href="dashboard" >Home</a></li>
      <li><a href="dashboard?command=serverInfo">Server Info</a></li>
      <li><a href="dashboard?command=serviceList"class="active">List of applications</a></li>
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
  <td class="boxTop colColor"></td><td class="rightCol boxTop colColor"></td>
  <td width="15" class="boxTopRight colColor"></td>
</tr>
<tr>
  <td class="boxLeft colColor"><br /></td>
  <td colspan="3" class="colColor"> 
    <div id="containing-div" >
      <div id="feed-viewer" style="margin-left: auto; margin-right: auto; width: 900px"></div>
    </div>
<!--
    <br><span class="Require">*</span> a customized submission form is avaiable for this application 
-->
	<br>Atom feed for available services<a href="opalServices.xml"> <img src="images/feed-icon.png"/> </a>
  </td>
  <td class="boxRight colColor"><br /></td>
</tr>
<%@ include file="footer.jsp" %>
</table> <!-- END Body -->
<br>
<%@ include file="copyright.jsp" %>
</div>
</body>
</html>


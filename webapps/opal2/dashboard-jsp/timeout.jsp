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
		<span class="Require"> Your session has timed out ...</span><br>
		Please click on <a href="dashboard?command=serviceList">List of applications</a>
		and choose the service to resubmit your job. <br>
		Any data you entered previously will need to be re-entered. 
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

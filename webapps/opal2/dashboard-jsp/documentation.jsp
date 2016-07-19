<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <title>Opal2 Toolkit Documentation</title>
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
    <div id="list-nav">
    <ul>
      <li class="left"><a href="dashboard">Home</a></li>
      <li><a href="dashboard?command=serverInfo">Server Info</a></li>
      <li><a href="dashboard?command=serviceList">List of applications</a></li>
      <li><a href="dashboard?command=statistics">Usage Statistics</a></li>
      <li class="right"><a href="dashboard?command=docs"  class="active">Documentation</a></li>
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
    <table border="0" width="100%">
      <tr>
        <td width=20% class="boxBodyRight colColor infoTitle">Opal Contributors: </td>
       <td class="boxBody colColor">Sriram Krishnan, Luca Clementi, Wes Goodman, 
       Jane Ren, Nadya Williams,<br> Anthony Bretaudeau, Yuan Luo, Karan Bhatia, Wilfred Li, Peter Arzberger </td>
      </tr>
      <tr>
        <td class="boxBodyRight colColor infoTitle">Websites: </td>
        <td class="boxBody colColor"><a href="<%= opalWebsite %>">
       <span class="PubTitle">Opal Web Site</span></a>
                  <a href="http://sourceforge.net/projects/opaltoolkit/">
				  <span class="PubTitle">Opal at SourceForge</span></a></td>
      </tr>

      <tr>
        <td class="boxBodyRight colColor infoTitle">Documentation:</td>
        <td class="boxBody colColor"><a href="<%= opalDocumentation %>/documentation.html">
		          <span class="PubTitle">Opal Documentation and Tutorials</span></a>
	</td> 
      </tr>

      <tr>
        <td class="boxBodyRight colColor infoTitle">Support: </td>
        <td class="boxBody colColor">
            <a href="mailto:support@nbcr.net"> <span class="PubTitle">NBCR Support Mailing List</span></a>
            <a href="http://sourceforge.net/mail/?group_id=211778"> <span class="PubTitle">Opal Mailing List</span></a>
        </td>
      </tr>
      <tr>
        <td class="boxBodyRight colColor infoTitle">Opal Use Cases:</td>
        <td class="boxBody colColor"><a href="http://meme.nbcr.net/">
		          <span class="PubTitle">MEME</span></a>
                  <a href="http://www.nbcr.net/pdb2pqr"><span class="PubTitle">PDB2PQR</span></a>
       <a href="http://www.opentopography.org/"><span class="PubTitle">OpenTopography</span></a>
      </tr>
      <tr>
        <td valign="top" class="boxBodyRight colColor infoTitle">Publications:</td> 
        <td class="boxBody colColor"> 
          <ul>
		    <li>
                <a href="<%= opalDocumentation %>/publications/opal2_icws.pdf">
			    <span class="PubTitle">Design and Evaluation of Opal2: A Toolkit for Scientific 
			    Software as a Service.</span></a><br>
			    <span class="PubAuth">Sriram Krishnan, Luca Clementi, Jingyuan Ren, 
				Philip Papadopoulos and Wilfred Li. </span>
				<span class="PubPub"> In proceedings of the 2009 IEEE Congress on Services (SERVICES-1 2009), July, 2009.</span>
			</li>
            <li>
                <a class="externalLink" href="http://www.collab-ogce.org/gce07/index.php/Main_Page">
			    <span class="PubTitle">Providing Dynamic Virtualized Access to Grid Resources via 
				the Web 2.0 Paradigm</span></a><br>
                <span class="PubAuth"> Luca Clementi, Zhaohui Ding, Sriram Krishnan, Xiaohui Wei, 
				Peter W. Arzberger, and Wilfred Li. </span>
                <span class="PubPub">GCE07, Grid Computing Environments
				Workshop (Supercomputing 2007), November 2007 </span>
            </li>
            <li>
                <a href="<%= opalDocumentation %>/publications/SDSC-TR-2006-5-opal.pdf">
			    <span class="PubTitle">Opal: Simple Web Services Wrappers for Scientific 
				Applications.</span></a><br>
                <span class="PubAuth">Sriram Krishnan, Brent Stearn, Karan Bhatia, Kim K. Baldridge, 
				Wilfred Li, and Peter Arzberger. </span>
                <span class="PubPub">In proceedings of ICWS 2006, IEEE International Conference 
				on Web Services, September 2006 </span>
            </li>
            <li>
                <a href="<%= opalDocumentation %>/publications/grid2005.pdf">
  		        <span class="PubTitle">An End-to-end Web Services-based Infrastructure for 
				Biomedical Applications.</span></a><br>
                <span class="PubAuth">Sriram Krishnan, Kim Baldridge, Jerry Greenberg, 
				Brent Stearn, and Karan Bhatia. </span>
                <span class="PubPub">In proceedings of Grid 2005, 6th IEEE/ACM International 
				Workshop on Grid Computing, November 2005 </span>
            </li>
          </ul>
        </td>
      </tr>
    </table>
    <hr>
    <div id="centered">
    <a href="http://www.nsf.gov/" ><img src="images/nsf-logo.jpg" align="middle"></img></a>
    <a href="http://www.moore.org/" ><img align="middle" src="images/moore-logo.gif"></img></a>
    <a href="http://www.nih.gov/" ><img align="middle" height="50" src="images/nih-logo.jpg" ></img></a>
    <a href="http://www.ncrr.nih.gov/"> <img align="middle" src="images/ncrr_logo.jpg"></img></a>
    <a href="http://camera.calit2.net/"> <img align="middle" height="50" src="images/Camera-Logo.jpg"></img></a>
    <a href="http://www.calit2.net"> <img align="middle" src="images/calit_logo.jpg"></img></a>
    <a href="http://www.sdsc.edu"> <img align="middle" src="images/SDSC-logo-red.gif"></img></a>
    </div>
  </td>
  <td class="boxRight colColor"><br /></td>
</tr>
<%@ include file="footer.jsp" %>
</table> <!-- END Body -->
<%@ include file="copyright.jsp" %>
</div>
</body>
</html>

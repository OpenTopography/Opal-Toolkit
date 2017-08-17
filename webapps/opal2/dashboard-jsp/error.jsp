<%--

	This page is not used anymore!!!
	it could be removed

	LC

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
String error = (String)request.getAttribute("error");

%>
<html>
    <head>
        <title>Opal2 Service Unexpected Error</title>
        <link rel="stylesheet" type="text/css" media="all" href="css/style.css" />
		<link rel="stylesheet" type="text/css" media="all" href="css/style-maintag.css"/>

    </head>
    <body>
    
    <h1>An unexpected error has occured here</h1>
    <%if (error != null) %>
    <h3>Error: <%=error %> </h3>
    

    </body>
</html>

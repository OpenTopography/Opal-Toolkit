<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/" xmlns:tns1="http://nbcr.sdsc.edu/opal/types" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:opal="http://nbcr.sdsc.edu/opal/types" version="2.0">
  <xsl:output method = "xml" indent = "yes" />
  
  <!-- Import data from Opal program definition file -->
  <xsl:param name="configPath"/>
  <xsl:variable name="config" select="document($configPath)"/>
  
  <!-- Add correct service name -->
  <xsl:template match="/wsdl:definitions/wsdl:service[@name='AppService']/@name">
    <xsl:attribute name="name">
      <xsl:value-of select="$config/opal:appConfig/opal:metadata/@appName"/>
    </xsl:attribute>
  </xsl:template>
  
  <!-- Copy general documentation -->
  <xsl:template match="/wsdl:definitions">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
	    <wsdl:documentation>
	      <xsl:for-each select="$config/opal:appConfig/opal:metadata/opal:info">
	        <xsl:value-of select="."/>
	      </xsl:for-each>
	    </wsdl:documentation>
      <xsl:apply-templates select="node()"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- Copy operations documentation -->
  <xsl:template match="/wsdl:definitions/wsdl:portType">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="node()" mode="operationsDocumentation"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- Copy operations documentation -->
  <xsl:template match="wsdl:operation" mode="operationsDocumentation">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*"/>
      <xsl:if test="@name='getAppMetadata' or @name='launchJob' or @name='queryStatus' or @name='getOutputs' or @name='getOutputAsBase64ByName' or @name='destroy' or @name='getJobStatistics' or @name='launchJobBlocking'">
	      <wsdl:documentation>
	        <xsl:choose>
	          <xsl:when test="@name='getAppMetadata'">Returns metadata about the application. This includes usage information, along with any number of arbitrary application-specific metadata specified as an array of info elements, e.g. description of the various options that are passed to the application binary.</xsl:when>
	          <xsl:when test="@name='launchJob'">Requires all the parameters with correct type, and structures representing the input files. The structure contains the name of the input file and either the contents in Base64 encoded binary form or a MIME attachment, or simply a location (URL) to the associated file. The operation returns a Job ID that can be used to retrieve job status and outputs.</xsl:when>
	          <xsl:when test="@name='queryStatus'">Expects a Job ID to query the status of a running job. A status code, message, and URL of the working directory for the job are returned.</xsl:when>
	          <xsl:when test="@name='getOutputs'">returns the outputs from a job that is identified by a Job ID. The output consists of the urls for the standard output and error, and an array of structures representing the output files. The structure contains the name of the output file and the url from where it can be downloaded.</xsl:when>
	          <xsl:when test="@name='getOutputAsBase64ByName'">Returns the contents of an output file as Base64 binary. The input is a data structure that contains the Job ID for a particular job, and the name of the file to be retrieved.</xsl:when>
	          <xsl:when test="@name='destroy'">Destroys a running job identified by a Job ID.</xsl:when>
	          <xsl:when test="@name='getJobStatistics'">Returns basic job statistics including start time, activation time and completion time for a given Job ID.</xsl:when>
	          <xsl:when test="@name='launchJobBlocking'">Same as launchJob but blocks until the remote execution is complete, and returns job outputs (as described above) as the response. This operation may only be appropriate for jobs that are not long running.</xsl:when>
	          <xsl:when test="@name='getAppConfig'">Returns Opal XML description of the program.</xsl:when>
	        </xsl:choose>
	      </wsdl:documentation>
      </xsl:if>
    </xsl:copy>
  </xsl:template>
  
  <!-- Replace content of JobInputType in WSDL -->
  <xsl:template match="//xsd:complexType[@name='JobInputType']/xsd:sequence">
    <xsd:sequence>
      <!-- Import flags -->
	    <xsl:for-each select="$config/opal:appConfig/opal:metadata/opal:types/opal:flags/opal:flag">
	      <xsd:element maxOccurs="1" minOccurs="0" name="{opal:id}" type="xsd:boolean">
          <xsl:if test="opal:default">
            <xsl:attribute name="default"><xsl:value-of select="opal:default"/></xsl:attribute>
          </xsl:if>
	       <xsd:annotation>
		       <xsd:documentation>
		         <xsl:value-of select="opal:textDesc"/>
		       </xsd:documentation>
	       </xsd:annotation>
	      </xsd:element>
	    </xsl:for-each>
	    
	    <!-- Import tagged params -->
      <xsl:for-each select="$config/opal:appConfig/opal:metadata/opal:types/opal:taggedParams/opal:param | $config/opal:appConfig/opal:metadata/opal:types/opal:untaggedParams/opal:param">
        <xsd:element maxOccurs="1" name="{opal:id}">
          <xsl:if test="opal:default">
            <xsl:attribute name="default"><xsl:value-of select="opal:default"/></xsl:attribute>
          </xsl:if>
          <xsl:attribute name="minOccurs">
            <xsl:choose>
              <xsl:when test="opal:required='true'">1</xsl:when>
              <xsl:otherwise>0</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:attribute name="type">
	          <xsl:choose>
	            <xsl:when test="opal:paramType='INT'">xsd:integer</xsl:when>
	            <xsl:when test="opal:paramType='BOOL'">xsd:boolean</xsl:when>
	            <xsl:when test="opal:paramType='FLOAT'">xsd:float</xsl:when>
	            <xsl:when test="opal:paramType='STRING'">xsd:string</xsl:when>
	            <xsl:when test="opal:paramType='FILE' and (opal:ioType='INPUT' or opal:ioType='INOUT')">tns1:InputFileType</xsl:when>
              <xsl:when test="opal:paramType='FILE' and opal:ioType='OUTPUT'">xsd:string</xsl:when>
	            <xsl:when test="opal:paramType='URL'">xsd:anyURI</xsl:when>
	          </xsl:choose>
          </xsl:attribute>
         <xsd:annotation>
	         <xsd:documentation>
	           <xsl:value-of select="opal:textDesc"/>
	         </xsd:documentation>
	       </xsd:annotation>
        </xsd:element>
      </xsl:for-each>
      
	    <xsd:element maxOccurs="1" minOccurs="0" name="numProcs" type="xsd:int"/>
	    <xsd:element maxOccurs="1" minOccurs="0" name="wallClockTime" type="xsd:nonNegativeInteger"/>
	    <xsd:element maxOccurs="1" minOccurs="0" name="userEmail" type="xsd:string"/>
	    <xsd:element maxOccurs="1" minOccurs="0" name="password" type="xsd:string"/>
	    <xsd:element maxOccurs="1" minOccurs="0" name="sendNotification" type="xsd:boolean"/>
    </xsd:sequence>
  </xsl:template>
  
  <xsl:template match="node() | @*">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>

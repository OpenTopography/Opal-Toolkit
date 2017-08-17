<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:opal="http://nbcr.sdsc.edu/opal/types" version="2.0">
  <xsl:output method = "xml" indent = "yes" />
  
  <!-- Import data from Opal program definition file -->
  <xsl:param name="configPath"/>
  <xsl:variable name="config" select="document($configPath)"/>

  <!-- Construct new SOAP content -->
  <xsl:template match="/soapenv:Envelope/soapenv:Body/opal:launchJobInput[not(argList) and not(inputFile) and node()] | /soapenv:Envelope/soapenv:Body/opal:launchJobBlockingInput[not(argList) and not(inputFile) and node()]">
    
    <xsl:variable name="taggedParamSep" select="$config/opal:appConfig/opal:metadata/opal:types/opal:taggedParams/opal:separator"/>
    <xsl:variable name="inputNodeValue" select="/soapenv:Envelope/soapenv:Body/opal:launchJobInput | /soapenv:Envelope/soapenv:Body/opal:launchJobBlockingInput"/>
    <xsl:variable name="inputTagName">
      <xsl:for-each select="$inputNodeValue"> <!-- There's only one element but couldn't find another way -->
        <xsl:value-of select="local-name()"/>
      </xsl:for-each>
    </xsl:variable>
    
    <!-- Return the SOAP request content -->
    <xsl:element name="{$inputTagName}" namespace="http://nbcr.sdsc.edu/opal/types">
      <argList>
	      <!-- Construct argList argument -->
        <!-- First flags -->
        <xsl:apply-templates select="$config/opal:appConfig/opal:metadata/opal:types/opal:flags/opal:flag">
          <xsl:with-param name="inputNode" select="$inputNodeValue"/>
        </xsl:apply-templates>
        <!-- Then taggedParams -->
	      <xsl:apply-templates select="$config/opal:appConfig/opal:metadata/opal:types/opal:taggedParams/opal:param" mode="args">
	        <xsl:with-param name="sep"><xsl:value-of select="$taggedParamSep"/></xsl:with-param>
          <xsl:with-param name="inputNode" select="$inputNodeValue"/>
	      </xsl:apply-templates>
        <!-- And finally untaggedParams -->
	      <xsl:apply-templates select="$config/opal:appConfig/opal:metadata/opal:types/opal:untaggedParams/opal:param" mode="args">
	        <xsl:with-param name="sep"><xsl:value-of select="taggedParamSep"/></xsl:with-param>
          <xsl:with-param name="inputNode" select="$inputNodeValue"/>
	      </xsl:apply-templates>
      </argList>
      
      <!-- Add numProcs et wallClockTime parameters -->
      <xsl:if test="numProcs">
        <numProcs><xsl:value-of select="numProcs"/></numProcs>
      </xsl:if>
      <xsl:if test="wallClockTime">
        <wallClockTime><xsl:value-of select="wallClockTime"/></wallClockTime>
      </xsl:if>

      <!-- Add userEmail, password and sendNotification-->
      <xsl:if test="userEmail">
        <userEmail><xsl:value-of select="userEmail"/></userEmail>
      </xsl:if>
      <xsl:if test="password">
        <password><xsl:value-of select="password"/></password>
      </xsl:if>
      <xsl:if test="sendNotification">
        <sendNotification><xsl:value-of select="sendNotification"/></sendNotification>
      </xsl:if>
      
      <!-- Construct inputFile arguments -->
      <xsl:apply-templates select="$config/opal:appConfig/opal:metadata/opal:types/opal:taggedParams/opal:param" mode="input">
          <xsl:with-param name="inputNode" select="$inputNodeValue"/>
        </xsl:apply-templates>
      <xsl:apply-templates select="$config/opal:appConfig/opal:metadata/opal:types/opal:untaggedParams/opal:param" mode="input">
          <xsl:with-param name="inputNode" select="$inputNodeValue"/>
        </xsl:apply-templates>
    </xsl:element>
  </xsl:template>
  
  
  
  <xsl:template match="opal:flag">
	  <xsl:param name="inputNode"/>
	  <xsl:variable name="elemId" select="opal:id"/>
	  <xsl:choose>
	    <xsl:when test="(not($inputNode/node()[(name()=$elemId)]) or not($inputNode/node()[(name()=$elemId) and node()])) and opal:default='true'">
	      <xsl:value-of select="opal:tag"/> <!-- Not specified, use default -->
	      <xsl:text> </xsl:text>
	    </xsl:when>
	    <xsl:when test="string($inputNode/node()[(name()=$elemId) and (boolean(.)=true())]) = 'true'">
	      <xsl:value-of select="opal:tag"/> <!-- Set to true by user -->
	      <xsl:text> </xsl:text>
	    </xsl:when>
	  </xsl:choose>
  </xsl:template>
  
  <xsl:template match="opal:param" mode="args">
    <xsl:param name="sep"/>
    <xsl:param name="inputNode"/>
    <xsl:variable name="elemId" select="opal:id"/>
    <xsl:choose>
      <xsl:when test="(not($inputNode/node()[(name()=$elemId)]) or not($inputNode/node()[(name()=$elemId) and node()])) and opal:default ">
          <xsl:value-of select="opal:tag"/> <!-- Not specified, use default -->
          <xsl:value-of select="$sep"/>
          <xsl:value-of select="opal:default"/>
          <xsl:text> </xsl:text>
      </xsl:when>
      <xsl:when test="$inputNode/node()[(name()=$elemId) and node()]">
        <xsl:choose>
          <xsl:when test="opal:paramType='FILE' and (opal:ioType='INOUT' or opal:ioType='INPUT')">
            <xsl:if test="$inputNode/node()[(name()=$elemId)]/name[node()] and ($inputNode/node()[(name()=$elemId)]/contents[node()] or $inputNode/node()[(name()=$elemId)]/location[node()] or $inputNode/node()[(name()=$elemId)]/attachment[node()])">
              <xsl:value-of select="opal:tag"/> <!-- Set by user -->
              <xsl:value-of select="$sep"/>
              <xsl:value-of select="$inputNode/node()[(name()=$elemId)]/name"/>
            <xsl:text> </xsl:text>
            </xsl:if>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="opal:tag"/> <!-- Set by user -->
            <xsl:value-of select="$sep"/>
            <xsl:value-of select="$inputNode/node()[(name()=$elemId)]"/>
            <xsl:text> </xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="opal:param" mode="input">
    <xsl:param name="inputNode"/>
    <xsl:variable name="elemId" select="opal:id"/>
    <xsl:if test="$inputNode/node()[(name()=$elemId) and node()] and $inputNode/node()[(name()=$elemId)]/name[node()] and ($inputNode/node()[(name()=$elemId)]/contents[node()] or $inputNode/node()[(name()=$elemId)]/location[node()] or $inputNode/node()[(name()=$elemId)]/attachment[node()]) and opal:paramType='FILE' and (opal:ioType='INOUT' or opal:ioType='INPUT')">
      <inputFile>
        <name><xsl:value-of select="$inputNode/node()[(name()=$elemId)]/name"/></name>
        <xsl:choose>
          <xsl:when test="$inputNode/node()[(name()=$elemId)]/contents[node()]">
            <contents><xsl:value-of select="$inputNode/node()[(name()=$elemId)]/contents"/></contents>
          </xsl:when>
          <xsl:when test="$inputNode/node()[(name()=$elemId)]/location[node()]">
            <location><xsl:value-of select="$inputNode/node()[(name()=$elemId)]/location"/></location>
          </xsl:when>
          <xsl:when test="$inputNode/node()[(name()=$elemId)]/attachment[node()]">
            <attachment><xsl:value-of select="$inputNode/node()[(name()=$elemId)]/attachment"/></attachment>
          </xsl:when>
        </xsl:choose>
      </inputFile>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="node() | @*">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>

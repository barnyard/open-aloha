<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:redirect="org.apache.xalan.xslt.extensions.Redirect"
  xmlns:bt="http://bt.com"
  extension-element-prefixes="redirect bt">

  <xsl:include href="../build-functions.xsl" />

  <xsl:param name="properties-xml" select="''"/>

  <xsl:output method="xml" version="1.0" encoding="iso-8859-1" indent="yes"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>

  <xsl:template match="statistics">
    <xsl:variable name="code-coverage">
      <xsl:value-of select="tool[@type='coverage']/statistic[@name='coverage']/@value"/>
    </xsl:variable>
    <xsl:variable name="test-passes">
      <xsl:value-of select="tool[@type='tests']/statistic[@name='passed']/@value"/>
    </xsl:variable>
    <xsl:variable name="acceptability">
      <xsl:call-template name="format-percent">
        <xsl:with-param name="number" select="(number($code-coverage) div 100) * number($test-passes)"/>
      </xsl:call-template>
    </xsl:variable>

    <statistics>
      <xsl:attribute name="acceptability">
        <xsl:value-of select="$acceptability"/>
      </xsl:attribute>
      <xsl:for-each select="tool">
        <xsl:call-template name="tool-report">
          <xsl:with-param name="tool" select="."/>
        </xsl:call-template>
      </xsl:for-each>
    </statistics>
  </xsl:template>

  <xsl:template name="tool-report">
    <xsl:param name="tool"/>
    <tool type="{$tool/@type}" name="{$tool/@name}" root="{$tool/@root}" description="{$tool/@description}">
      <xsl:for-each select="statistic">
        <xsl:variable name="property" 
          select="concat($tool/@type, '.', @name)"/>
        <xsl:variable name="min">
          <xsl:call-template name="min">
            <xsl:with-param name="property" select="$property"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="max">
          <xsl:call-template name="max">
            <xsl:with-param name="property" select="$property"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:call-template name="row-check">
          <xsl:with-param name="value" select="@value"/>
          <xsl:with-param name="min" select="$min"/>
          <xsl:with-param name="max" select="$max"/>
          <xsl:with-param name="description" select="@description"/>
          <xsl:with-param name="unit" select="@unit"/>
        </xsl:call-template>
      </xsl:for-each>
    </tool>
  </xsl:template>

  <xsl:template name="row-check">
    <xsl:param name="value"/>
    <xsl:param name="min"/>
    <xsl:param name="max"/>
    <xsl:param name="description"/>
    <xsl:param name="unit"/>
    <!-- 
         The Value should not be empty, but if for example if you don't compile with debug information you won't get emma to report correctly
         and infact it will tell you that you have passed, so I have added a check for the value being empty.  Ross McWattie Fev 2007
     -->
    <xsl:if test="$min != ''">
      <xsl:variable name="success">
        <xsl:choose>
          <xsl:when test="$value ='' ">
            <xsl:value-of select="'false'"/>
          </xsl:when>
          <xsl:when test="$min &gt; $value ">
            <xsl:value-of select="'false'"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="'true'"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:call-template name="row-report">
        <xsl:with-param name="value" select="$value"/>
        <xsl:with-param name="maxmin" select="'min'"/>
        <xsl:with-param name="required" select="$min"/>
        <xsl:with-param name="success" select="$success"/>
        <xsl:with-param name="description" select="$description"/>
        <xsl:with-param name="unit" select="$unit"/>
      </xsl:call-template>
    </xsl:if>
    <xsl:if test="$max != ''">
      <xsl:variable name="success">
        <xsl:choose>
          <xsl:when test="$value ='' ">
            <xsl:value-of select="'false'"/>
          </xsl:when>
          <xsl:when test="$max &lt; $value">
            <xsl:value-of select="'false'"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="'true'"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:call-template name="row-report">
        <xsl:with-param name="value" select="$value"/>
        <xsl:with-param name="maxmin" select="'max'"/>
        <xsl:with-param name="required" select="$max"/>
        <xsl:with-param name="success" select="$success"/>
        <xsl:with-param name="description" select="$description"/>
        <xsl:with-param name="unit" select="$unit"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="row-report">
    <xsl:param name="value"/>
    <xsl:param name="maxmin"/>
    <xsl:param name="required"/>
    <xsl:param name="success"/>
    <xsl:param name="description"/>
    <xsl:param name="unit"/>
    <xsl:variable name="text">
      <xsl:choose>
        <xsl:when test="$maxmin = 'max'">
          <xsl:value-of select="concat($value,$unit,' (at most ',$required,$unit,' allowed)')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat($value,$unit,' (at least ',$required,$unit,' required)')"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <statistic description="{$description}" text="{$text}" success="{$success}"/>
  </xsl:template>

  <xsl:template name="min">
    <xsl:param name="property"/>
    <xsl:variable name="select" select="concat($property, '.min')"/>
    <xsl:value-of select="document($properties-xml)/properties/property[@name=$select]/@value"/>
  </xsl:template>

  <xsl:template name="max">
    <xsl:param name="property"/>
    <xsl:variable name="select" select="concat($property, '.max')"/>
    <xsl:value-of select="document($properties-xml)/properties/property[@name=$select]/@value"/>
  </xsl:template>

</xsl:stylesheet>

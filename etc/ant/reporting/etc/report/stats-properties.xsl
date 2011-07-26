<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:redirect="org.apache.xalan.xslt.extensions.Redirect"
  extension-element-prefixes="redirect">

  <xsl:param name="properties-xml" select="''"/>
  <xsl:output method="xml" version="1.0" encoding="iso-8859-1" indent="yes"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>
  <xsl:param name="title"
    select="document($properties-xml)/properties/property[@name='ant.project.name']/@value"/>

  <xsl:template match="statistics">
    <statistics project="{$title}">
          <xsl:for-each select="tool">
            <xsl:call-template name="tool-report">
              <xsl:with-param name="tool" select="."/>
            </xsl:call-template>
          </xsl:for-each>
    </statistics>
  </xsl:template>

  <xsl:template name="tool-report">
    <xsl:param name="tool"/>
    <xsl:element name = "{$tool/@type}" >
    <xsl:for-each select="statistic">
      <xsl:variable name="property" select="concat($tool/@type, '.', @name)"/>
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
        <xsl:with-param name="statistic" select="."/>
        <xsl:with-param name="min" select="$min"/>
        <xsl:with-param name="max" select="$max"/>
        <xsl:with-param name="description" select="@description"/>
        <xsl:with-param name="tool" select="$tool"/>
      </xsl:call-template>
    </xsl:for-each>
    </xsl:element>

  </xsl:template>

  <xsl:template name="row-check">
    <xsl:param name="statistic"/>
    <xsl:param name="min"/>
    <xsl:param name="max"/>
    <xsl:param name="description"/>
    <xsl:param name="tool"/>

    <xsl:if test="$min != ''">

      <xsl:variable name="success">
        <xsl:choose>
          <xsl:when test="$min &lt; $statistic/@value">
            <xsl:value-of select="'false'"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="'true'"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:call-template name="row-report">
        <xsl:with-param name="statistic" select="$statistic"/>
        <xsl:with-param name="maxmin" select="'min'"/>
        <xsl:with-param name="required" select="$min"/>
        <xsl:with-param name="success" select="$success"/>
        <xsl:with-param name="description" select="$description"/>
        <xsl:with-param name="tool" select="$tool"/>
      </xsl:call-template>

    </xsl:if>

    <xsl:if test="$max != ''">
      <xsl:variable name="success">
        <xsl:choose>
          <xsl:when test="$max &gt; $statistic/@value">
            <xsl:value-of select="'false'"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="'true'"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:call-template name="row-report">
        <xsl:with-param name="statistic" select="$statistic"/>
        <xsl:with-param name="maxmin" select="'max'"/>
        <xsl:with-param name="required" select="$max"/>
        <xsl:with-param name="success" select="$success"/>
        <xsl:with-param name="description" select="$description"/>
        <xsl:with-param name="tool" select="$tool"/>
      </xsl:call-template>

    </xsl:if>
  </xsl:template>

  <xsl:template name="row-report">
    <xsl:param name="statistic"/>
    <xsl:param name="maxmin"/>
    <xsl:param name="required"/>
    <xsl:param name="success"/>
    <xsl:param name="description"/>
    <xsl:param name="tool"/>

    <xsl:element name="{$statistic/@name}">
      <xsl:attribute name="value"><xsl:value-of select="$statistic/@value"/></xsl:attribute>
      <xsl:if test="$success = 'false'">
        <fail>
          <xsl:attribute name="value">
            <xsl:choose>
              <xsl:when test="$maxmin = 'max'">
                <xsl:value-of select="concat('Too many ',$tool/@description, ' ', $statistic/@description, ' (',$statistic/@value,' actual,  ',$required,' required)')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="concat('Too few ',$tool/@description, ' ', $statistic/@description, ' (',$statistic/@value,' actual, ',$required,' required)')"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </fail>
      </xsl:if>
    </xsl:element>


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

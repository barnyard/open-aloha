<xsl:stylesheet version="1.0"
  xmlns:bt="http://bt.com"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  extension-element-prefixes="bt">

  <xsl:include href="../build-functions.xsl" />
  
  <xsl:output method="xml" encoding="iso-8859-1" indent="yes"/>
  <xsl:param name="output.dir" select="'.'"/>

  <xsl:template match="/report/data/all[@name='all classes']">
    <tool type="coverage" name="emma" description="Code Coverage" root="emma/" >
      <xsl:variable name="lines">
        <xsl:value-of select="coverage[@type='line, %']/@numeric"/>
      </xsl:variable>
      <xsl:variable name="methods">
        <xsl:value-of select="coverage[@type='method, %']/@numeric"/>
      </xsl:variable>
      <xsl:variable name="classes">
        <xsl:value-of select="coverage[@type='class, %']/@numeric"/>
      </xsl:variable>
      <xsl:variable name="minlines">
        <xsl:for-each select="descendant::coverage[@type='line, %']">
          <xsl:sort 
            data-type="number" select="@numeric" order="ascending"/>
          <xsl:if test="position()=1">
            <xsl:value-of select="@numeric"/>
          </xsl:if>
        </xsl:for-each>
      </xsl:variable>
      <xsl:call-template name="getnode">
        <xsl:with-param name="description" select="'Coverage'"/>
        <xsl:with-param name="attribute" select="'coverage'"/>
        <xsl:with-param name="input">
          <xsl:call-template name="format-percent">
            <xsl:with-param name="number" select="(number($lines) div 100) * (number($methods) div 100) * number($classes)"/>
          </xsl:call-template>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="getnode">
        <xsl:with-param name="description" select="'Classes'"/>
        <xsl:with-param name="attribute" select="'classes'"/>
        <xsl:with-param name="input" select="$classes"/>
      </xsl:call-template>
      <xsl:call-template name="getnode">
        <xsl:with-param name="description" select="'Methods'"/>
        <xsl:with-param name="attribute" select="'methods'"/>
        <xsl:with-param name="input" select="$methods"/>
      </xsl:call-template>
      <xsl:call-template name="getnode">
        <xsl:with-param name="description" select="'Lines'"/>
        <xsl:with-param name="attribute" select="'lines'"/>
        <xsl:with-param name="input" select="$lines"/>
      </xsl:call-template>
      <xsl:call-template name="getnode">
        <xsl:with-param name="description" select="'Minimum lines'"/>
        <xsl:with-param name="attribute" select="'minlines'"/>
        <xsl:with-param name="input" select="$minlines"/>
      </xsl:call-template>
    </tool>
  </xsl:template>

  <xsl:template name="getnode">
    <xsl:param name="description"/>
    <xsl:param name="attribute"/>
    <xsl:param name="input"/>
    <statistic 
      unit="%" 
      name="{$attribute}" 
      value="{$input}"
      description="{$description}"/>
  </xsl:template>

</xsl:stylesheet>
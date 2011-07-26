
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" encoding="iso-8859-1" indent="yes"/>
  <xsl:param name="output.dir" select="'.'"/>

  <xsl:template match="/same">
    <tool type="duplication" name="same" description="Duplication" root="same/" >
      <xsl:variable name="maxlines">
        <xsl:for-each select="duplication">
          <xsl:sort data-type="number" select="@length" order="descending"/>
          <xsl:if test="position()=1">
            <xsl:value-of select="@length"/>
          </xsl:if>
        </xsl:for-each>
      </xsl:variable>
      <xsl:call-template name="getnode">
        <xsl:with-param name="description" select="'Worst line violation'"/>
        <xsl:with-param name="attribute" select="'lines'"/>
        <xsl:with-param name="value" select="$maxlines"/>
      </xsl:call-template>
    </tool>
  </xsl:template>

  <xsl:template name="getnode">
    <xsl:param name="attribute"/>
    <xsl:param name="value"/>
    <xsl:param name="description"/>
    <statistic name="{$attribute}">
      <xsl:if test="string-length($value) = 0">
        <xsl:attribute name="value">
          <xsl:value-of select="'0'"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="string-length($value) > 0">
        <xsl:attribute name="value">
          <xsl:value-of select="$value"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:attribute name="description">
        <xsl:value-of select="$description"/>
      </xsl:attribute>
    </statistic>
  </xsl:template>

</xsl:stylesheet>
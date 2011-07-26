
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" version="1.0" encoding="iso-8859-1" indent="yes"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>

  <xsl:template match="coverage">
    <coverage>
      <xsl:attribute name="numeric">
        <xsl:choose>
          <xsl:when test="contains(@value, '%')">
            <xsl:value-of select="substring-before(@value, '%')"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="@value"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:copy-of select="@*[not(.='')]|node()"/>
    </coverage>
  </xsl:template>

  <xsl:template match="@*|node()|text()">
    <xsl:copy>
      <xsl:apply-templates select="@*|*|text()" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
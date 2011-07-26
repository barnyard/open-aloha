<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:redirect="org.apache.xalan.xslt.extensions.Redirect"
  xmlns:datetime="http://exslt.org/dates-and-times"

  extension-element-prefixes="redirect datetime">

  <xsl:output method="xml" version="1.0" encoding="iso-8859-1" indent="yes"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>
  <xsl:param name="xmlfiles" select="''"/>
  <xsl:param name="project" select="''"/>
  <xsl:param name="user" select="''"/>

  <xsl:template match="statistics">
    <statistics>
      <xsl:attribute name="date">
        <xsl:value-of select="datetime:date-time()"/>
      </xsl:attribute>
      <xsl:attribute name="project">
        <xsl:value-of select="$project"/>
      </xsl:attribute>
      <xsl:attribute name="user">
        <xsl:value-of select="$user"/>
      </xsl:attribute>
      <xsl:call-template name="tokenize">
        <xsl:with-param name="string" select="$xmlfiles"/>
        <xsl:with-param name="delimiters" select="';'"/>
      </xsl:call-template>
    </statistics>
  </xsl:template>

  <xsl:template match="tool">
    <xsl:copy>
      <xsl:apply-templates select="@*|*|text()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@*|node()|text()">
    <xsl:copy>
      <xsl:apply-templates select="@*|*|text()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template name="tokenize">
    <xsl:param name="string" select="''"/>
    <xsl:param name="delimiters" select="' &#x9;&#xA;'"/>
    <xsl:choose>
      <xsl:when test="not($string)"/>
      <xsl:when test="not($delimiters)">
        <xsl:call-template name="_tokenize-characters">
          <xsl:with-param name="string" select="$string"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="_tokenize-delimiters">
          <xsl:with-param name="string" select="$string"/>
          <xsl:with-param name="delimiters" select="$delimiters"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="_tokenize-characters">
    <xsl:param name="string"/>
    <xsl:if test="$string">

      <xsl:apply-templates select="document(substring($string, 2))/tool"/>
      <xsl:call-template name="_tokenize-characters">
        <xsl:with-param name="string" select="substring($string, 2)" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="_tokenize-delimiters">
    <xsl:param name="string"/>
    <xsl:param name="delimiters"/>
    <xsl:variable name="delimiter" select="substring($delimiters, 1, 1)"/>
    <xsl:choose>
      <xsl:when test="not($delimiter)">
        <xsl:apply-templates select="document($string)/tool"/>
      </xsl:when>
      <xsl:when test="contains($string, $delimiter)">
        <xsl:if test="not(starts-with($string, $delimiter))">
          <xsl:call-template name="_tokenize-delimiters">
            <xsl:with-param name="string" select="substring-before($string, $delimiter)"/>
            <xsl:with-param name="delimiters" select="substring($delimiters, 2)"/>
          </xsl:call-template>
        </xsl:if>
        <xsl:call-template name="_tokenize-delimiters">
          <xsl:with-param name="string" select="substring-after($string, $delimiter)"/>
          <xsl:with-param name="delimiters" select="$delimiters"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="_tokenize-delimiters">
          <xsl:with-param name="string" select="$string"/>
          <xsl:with-param name="delimiters" select="substring($delimiters, 2)"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


</xsl:stylesheet>
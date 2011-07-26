<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:redirect="org.apache.xalan.xslt.extensions.Redirect"
  extension-element-prefixes="redirect">

  <xsl:output method="xml" version="1.0" encoding="iso-8859-1" indent="yes"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>
  <xsl:param name="xmlfiles" select="''"/>

  <xsl:template match="root">
    <root>
      <xsl:call-template name="tokenize">
        <xsl:with-param name="string" select="$xmlfiles"/>
        <xsl:with-param name="delimiters" select="';'"/>
      </xsl:call-template>
    </root>
  </xsl:template>

  <xsl:template match="testsuite">
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
      <!--
      <xsl:message>
        <xsl:value-of select="concat('Merging ',substring($string, 2))"/>
      </xsl:message>
      -->
      <xsl:comment>
        <xsl:value-of select="concat('start of ',substring($string, 2))"/>
      </xsl:comment>
      <xsl:apply-templates select="document(substring($string, 2))/testsuite"/>
      <xsl:comment>
        <xsl:value-of select="concat('end of ',substring($string, 2))"/>
      </xsl:comment>
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
        <xsl:comment>
          <xsl:value-of select="concat('start of ',$string)"/>
        </xsl:comment>
        <xsl:apply-templates select="document($string)/testsuite"/>
        <xsl:comment>
          <xsl:value-of select="concat('end of ',$string)"/>
        </xsl:comment>
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
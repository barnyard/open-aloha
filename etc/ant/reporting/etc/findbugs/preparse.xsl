<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt="http://xml.apache.org/xslt">

  <xsl:output method="xml" version="1.0" encoding="iso-8859-1" indent="yes"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>
  <xsl:param name="output.dir" select="'.'"/>

  <xsl:include href="../build-templates.xsl"/>
  <xsl:include href="../html-templates.xsl"/>

  <xsl:template match="BugCollection">
    <BugCollection>
      <xsl:attribute name="version">
        <xsl:value-of select="@version"/>
      </xsl:attribute>
      <xsl:attribute name="modified">
        <xsl:value-of select="'bt-build-process'"/>
      </xsl:attribute>
      <xsl:apply-templates select="Project"/>
      <xsl:for-each select="BugInstance[@category!='MALICIOUS_CODE']">
        <xsl:apply-templates select="."/>
      </xsl:for-each>
    </BugCollection>
  </xsl:template>

  <xsl:template match="@*|node()|text()">
    <xsl:copy>
      <xsl:apply-templates select="@*|*|text()" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="Class|SourceLine|Method">
    <xsl:variable name="cname">
      <xsl:call-template name="after-last">
        <xsl:with-param name="string" select="@classname"/>
        <xsl:with-param name="search" select="'.'"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:copy>
      <xsl:attribute name="package">
        <xsl:value-of select="substring-before(@classname, concat('.', $cname))"/>
      </xsl:attribute>
      <xsl:attribute name="class">
        <xsl:value-of select="$cname"/>
      </xsl:attribute>
      <xsl:apply-templates select="@*|*|text()" />
    </xsl:copy>

  </xsl:template>


</xsl:stylesheet>
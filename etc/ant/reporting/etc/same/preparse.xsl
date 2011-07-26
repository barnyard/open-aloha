<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt="http://xml.apache.org/xslt">

  <xsl:output method="xml" version="1.0" encoding="iso-8859-1" indent="yes"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>
  <xsl:param name="output.dir" select="'.'"/>
  <xsl:param name="source-root"/>
  <xsl:param name="same-lines-max" select="'12'"/>
  <xsl:variable name="src" select="translate($source-root, '\:', '/_')"/>

  <xsl:template match="same">
    <same>
      <xsl:apply-templates select="duplication"/>
    </same>
  </xsl:template>

  <xsl:template match="duplication">
    <xsl:if test="(@length &gt; $same-lines-max)">
      <xsl:copy>
        <xsl:apply-templates select="@* | node() " />
      </xsl:copy>
    </xsl:if>
  </xsl:template>

  <xsl:template match="file">
    <file>
      <xsl:copy-of select="@*[not(.='')]|node()"/>
      <xsl:attribute name="class">
        <xsl:call-template name="to-classname">
          <xsl:with-param name="path" select="@name"/>
        </xsl:call-template>
      </xsl:attribute>
      <xsl:attribute name="package">
        <xsl:call-template name="to-packagename">
          <xsl:with-param name="path" select="@name"/>
        </xsl:call-template>
      </xsl:attribute>
    </file>
  </xsl:template>

  <xsl:template match="@*|node()|text()">
    <xsl:copy>
      <xsl:apply-templates select="@*|*|text()" />
    </xsl:copy>
  </xsl:template>

  <xsl:template name="to-packagename">
		<xsl:param name="path"/>
        <xsl:variable name="clz">/<xsl:call-template name="after-last"><xsl:with-param name="string" select="substring-after(translate($path, '\:', '/_'), $src)"/><xsl:with-param name="search" select="'/'"/></xsl:call-template></xsl:variable>
		<xsl:value-of
			select="translate(substring-before(substring-after(translate($path, '\:', '/_'), $src), $clz), '/', '.')"/>
	</xsl:template>

  <xsl:template name="to-classname">
    <xsl:param name="path"/>
    <xsl:variable name="clz">
      <xsl:call-template name="after-last">
        <xsl:with-param name="string" 
          select="substring-after(translate($path, '\:', '/_'), $src)"/>
        <xsl:with-param name="search" select="'/'"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="ext">
      <xsl:call-template name="extension">
        <xsl:with-param name="path" select="$path"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$ext = 'java'">
        <xsl:value-of select="substring-before($clz, '.java')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$clz"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="after-last">
    <xsl:param name="string"/>
    <xsl:param name="search"/>
    <xsl:choose>
      <xsl:when test="contains($string, $search)">
        <xsl:call-template name="after-last">
          <xsl:with-param name="string"
              select="substring-after($string, $search)"/>
          <xsl:with-param name="search" select="$search"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$string"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="extension">
    <xsl:param name="path"/>
    <xsl:choose>
      <xsl:when test="contains($path, '.')">
        <xsl:call-template name="after-last">
          <xsl:with-param name="string"
              select="$path"/>
          <xsl:with-param name="search" select="'.'"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="''"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
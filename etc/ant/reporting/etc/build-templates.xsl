<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt=        "http://xml.apache.org/xslt"
  xmlns:redirect=     "org.apache.xalan.xslt.extensions.Redirect"
  extension-element-prefixes="redirect lxslt xsl">

  <!-- returns ../../../ when passed blah/blah/blah -->
  <xsl:template name="back.url">
    <xsl:param name="name"/>
    <xsl:variable name="after" select="normalize-space(translate($name, '.',''))"/>
    <xsl:variable name="slashes" select="string-length($name)-string-length($after) + 1"/>
    <xsl:call-template name="append">
      <xsl:with-param name="in" select="''"/>
      <xsl:with-param name="count" select="$slashes"/>
      <xsl:with-param name="append" select="'../'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="append">
    <xsl:param name="in"/>
    <xsl:param name="append"/>
    <xsl:param name="count"/>
    <xsl:variable name="out" select="concat($in, $append)"/>
    <xsl:variable name="counter" select="$count - 1"/>
    <xsl:choose>
      <xsl:when test="$counter &gt; -1">
        <xsl:call-template name="append">
          <xsl:with-param name="in" select="$out"/>
          <xsl:with-param name="count" select="$counter"/>
          <xsl:with-param name="append" select="$append"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$in"/>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template name="output-file">
    <xsl:param name="class"/>
    <xsl:param name="package"/>
    <xsl:value-of select="concat(translate($package,'.','/'),'/',$class,'.html')"/>
  </xsl:template>

  <xsl:template name="output-file-for-package">
    <xsl:param name="package"/>
    <xsl:param name="suffix"/>
    <xsl:value-of select="concat(translate($package,'.','/'),'/', $suffix,'.html')"/>
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

  <xsl:template name="format-percent">
    <xsl:param name="number"/>
    <xsl:variable name="res">
      <xsl:choose>
        <xsl:when test="contains(number($number),'NaN')">
          <xsl:value-of select="0.00" />
        </xsl:when>
        <xsl:when test="number=''">
          <xsl:value-of select="0.00" />
        </xsl:when>
        <xsl:when test="number(number) = 0">
          <xsl:value-of select="0.00" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="format-number($number, '0.00')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="res2">
      <xsl:choose>
        <xsl:when test="$res=''">
          <xsl:value-of select="0.00"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$res"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:value-of select="$res2"/>
  </xsl:template>
    

</xsl:stylesheet>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:func="http://exslt.org/functions"
  xmlns:bt="http://bt.com"
  extension-element-prefixes="func">

  <xsl:include href="build-templates.xsl"/>

  <func:function name="bt:format-percent">
    <xsl:param name="number" />

    <xsl:variable name="result">
      <xsl:call-template name="format-percent">
        <xsl:with-param name="number" select="$number"/>
      </xsl:call-template>
    </xsl:variable>
    <func:result select="result"/>

  </func:function>

 </xsl:stylesheet>

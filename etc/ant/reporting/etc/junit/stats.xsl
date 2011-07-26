
<xsl:stylesheet version="1.0"
  xmlns:bt="http://bt.com"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  extension-element-prefixes="bt">

  <!--xsl:include href="../../etc/xsl/build-functions.xsl" /-->
  <xsl:include href="../build-functions.xsl" />

  <xsl:output method="xml" encoding="iso-8859-1" indent="yes"/>

  <xsl:template match="/root">
    <xsl:variable name="tests">
      <xsl:value-of select="sum(testsuite/@tests)"/>
    </xsl:variable>
    <xsl:variable name="errors">
      <xsl:value-of select="sum(testsuite/@errors)"/>
    </xsl:variable>
    <xsl:variable name="failures">
      <xsl:value-of select="sum(testsuite/@failures)"/>
    </xsl:variable>
    <xsl:variable name="passes">
      <xsl:value-of select="number($tests) -number($errors) -number($failures)"/>
    </xsl:variable>
    <tool type="tests" name="junit" description="Tests" root="junit/">
      <statistic name="errors" description="Errors">
        <xsl:attribute name="value">
          <xsl:value-of select="$errors"/>
        </xsl:attribute>
      </statistic>
      <statistic name="failures" description="Failures">
        <xsl:attribute name="value">
          <xsl:value-of select="$failures"/>
        </xsl:attribute>
      </statistic>
      <statistic name="passed" description="Passed">
        <xsl:attribute name="value">
          <xsl:call-template name="format-percent">
            <xsl:with-param name="number" select="(100 div number($tests)) * number($passes)"/>
          </xsl:call-template>
        </xsl:attribute>
        <xsl:attribute name="unit">
          <xsl:value-of select="'%'"/>
        </xsl:attribute>
      </statistic>
    </tool>
  </xsl:template>



</xsl:stylesheet>

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" encoding="iso-8859-1" indent="yes"/>

  <xsl:template match="/BugCollection">
    <tool type="bugs" name="findbugs" description="Statically Detectable Bugs" root="findbugs/">
      <statistic name="high" description="High">
        <xsl:attribute name="value">
          <xsl:value-of select="count(BugInstance[@priority='1'])"/>
        </xsl:attribute>
      </statistic>
      <statistic name="medium" description="Medium">
        <xsl:attribute name="value">
          <xsl:value-of select="count(BugInstance[@priority='2'])"/>
        </xsl:attribute>
      </statistic>
      <statistic name="low" description="Low">
        <xsl:attribute name="value">
          <xsl:value-of select="count(BugInstance[@priority='3'])"/>
        </xsl:attribute>
      </statistic>
    </tool>
  </xsl:template>

</xsl:stylesheet>

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" encoding="iso-8859-1" indent="yes"/>

  <xsl:template match="/JDepend">
    <tool type="cycles" name="jdepend" description="Cyclic Dependencies" root="jdepend/">
      <statistic name="cycles" description="Cycles">
        <xsl:attribute name="value">
          <xsl:value-of select="count(Cycles/Package)"/>
        </xsl:attribute>
      </statistic>
    </tool>
  </xsl:template>

</xsl:stylesheet>
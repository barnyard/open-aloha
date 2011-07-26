
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" encoding="iso-8859-1" indent="yes"/>

  <xsl:template match="/checkstyle">
    <tool type="conventions" name="checkstyle" description="Code Conventions" root="checkstyle/">
      <xsl:attribute name="files">
        <xsl:value-of select="count(file)"/>
      </xsl:attribute>
      <statistic name="errors" description="Errors">
        <xsl:attribute name="value">
          <xsl:value-of select="count(file/error)"/>
        </xsl:attribute>
      </statistic>
    </tool>
  </xsl:template>

</xsl:stylesheet>
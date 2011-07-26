<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:output method="html"/>

	<xsl:template match="/" mode="html">
		<xsl:if test="count(/cruisecontrol/html) &gt; 0">
			<table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
				<tbody>
					<tr>
						<th class="checkstyle-sectionheader" colspan="5" align="left">HTML documentation</th>
					</tr>
					<tr>
						<xsl:copy-of select="/cruisecontrol/html"/>
					</tr>
				</tbody>
			</table>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="/">
        <xsl:apply-templates select="." mode="html"/>
    </xsl:template>
</xsl:stylesheet>

<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:output method="html"/>

	<xsl:template match="/" mode="emma">
		<xsl:if test="count(/cruisecontrol/report/data/all/package) &gt; 0">
			<table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
				<tbody>
					<tr>
						<th class="checkstyle-sectionheader" colspan="5" align="left">Emma Results</th>
					</tr>
					<tr>
						<td class="checkstyle-sectionheader">Package</td>
						<td class="checkstyle-sectionheader">Class Coverage</td>
						<td class="checkstyle-sectionheader">Method Coverage</td>
						<td class="checkstyle-sectionheader">Block Coverage</td>
						<td class="checkstyle-sectionheader">Line Coverage</td>
					</tr>
					<xsl:for-each select="/cruisecontrol/report/data/all/package">
						<xsl:sort select="@name"/>
						<tr>
							<xsl:if test="position() mod 2 = 1">
								<xsl:attribute name="class">checkstyle-oddrow</xsl:attribute>
							</xsl:if>
							<td><xsl:value-of select="@name"/></td>
								<xsl:for-each select="coverage">
								<td>
									<xsl:value-of select="@value"/>	
								</td>
							</xsl:for-each>
						</tr>
					</xsl:for-each>
				</tbody>
			</table>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="/">
        <xsl:apply-templates select="." mode="emma"/>
    </xsl:template>
</xsl:stylesheet>

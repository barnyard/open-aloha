<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:include href="fitnesse-css.xsl"/>

	<xsl:output method="html"/>
	<xsl:template match="/" mode="fitnesse">
		<xsl:apply-templates select="cruisecontrol/testResults" mode="fitnesse"/>
	</xsl:template>
	
	
	<xsl:template match="testResults[result]" mode="fitnesse">
		<xsl:call-template name="fitnesseCss"/>
		<script type="text/javascript" language="javascript">
		<![CDATA[
			function showDetails(rowNumber) {
			
				var detailsRow = document.getElementById("details" + rowNumber);
				var detailsButton = document.getElementById("button" + rowNumber);
				
				if (detailsRow != null && detailsButton != null) {
					if (detailsButton.value == "Show Details") {
						detailsButton.value = "Hide Details";
						detailsRow.style.display = "";
					}
					else {
						detailsButton.value = "Show Details";
						detailsRow.style.display = "none"
					}
				}
			
			}]]>
		</script>
		<table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
			<tbody>
				<tr>
					<th class="checkstyle-sectionheader" colspan="6" align="left">
                Fitnesse Results for <xsl:value-of select="rootPath"/>
					</th>
				</tr>
				<tr>
					<td class="checkstyle-sectionheader">Page</td>
					<td class="checkstyle-sectionheader">Right</td>
					<td class="checkstyle-sectionheader">Wrong</td>
					<td class="checkstyle-sectionheader">Ignored</td>
					<td class="checkstyle-sectionheader">Exception</td>
					<td class="checkstyle-sectionheader"> </td>
				</tr>
				<tr>
					<td>Overall</td>
					<td>
						<xsl:value-of select="finalCounts/right"/>
					</td>
					<td>
						<xsl:value-of select="finalCounts/wrong"/>
					</td>
					<td>
						<xsl:value-of select="finalCounts/ignores"/>
					</td>
					<td>
						<xsl:value-of select="finalCounts/exceptions"/>
					</td>
					<td> </td>
				</tr>
				<xsl:for-each select="result">
					<tr>
						<xsl:if test="position() mod 2 = 1">
							<xsl:attribute name="class">checkstyle-oddrow</xsl:attribute>
						</xsl:if>
						<td>
							<xsl:value-of select="relativePageName"/>
						</td>
						<td>
							<xsl:value-of select="counts/right"/>
						</td>
						<td>
							<xsl:value-of select="counts/wrong"/>
						</td>
						<td>
							<xsl:value-of select="counts/ignores"/>
						</td>
						<td>
							<xsl:value-of select="counts/exceptions"/>
						</td>
						<td>
							<xsl:element name="input">
                                <xsl:attribute name="type">submit</xsl:attribute>
								<xsl:attribute name="id">button<xsl:value-of select="position()"/></xsl:attribute>
								<xsl:attribute name="onClick">showDetails(<xsl:value-of select="position()"/>)</xsl:attribute>
								<xsl:attribute name="value">Show Details</xsl:attribute>								
							</xsl:element>
						</td>
					</tr>
					<xsl:element name="tr">
						<xsl:attribute name="id">details<xsl:value-of select="position()"/></xsl:attribute>
						<xsl:attribute name="style">display: none;</xsl:attribute>
						<td colspan="6">
							<xsl:value-of select="content" disable-output-escaping="yes"/>
						</td>
					</xsl:element>
				</xsl:for-each>
			</tbody>
		</table>
	</xsl:template>
	
	
	<!--<xsl:template match="/">
		<xsl:apply-templates select="." mode="fitnesse"/>
	</xsl:template>-->
</xsl:stylesheet>

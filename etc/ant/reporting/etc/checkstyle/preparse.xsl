<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:lxslt="http://xml.apache.org/xslt">

	<xsl:output method="xml" version="1.0" encoding="iso-8859-1"
		indent="yes" />
	<xsl:decimal-format decimal-separator="." grouping-separator="," />
	<xsl:param name="output.dir" select="'.'" />
	<xsl:param name="source-root" />
	<xsl:param name="junit-source-root" />
	<!-- xsl:param name="src" select="translate($source-root, '\:', '/_')"/-->
	<!-- xsl:variable name="src" select="translate($source-root, '\:', '/_')"/-->

	<!-- xsl:param name="src"/>
		<xsl:choose>
		<xsl:when test="contains($junit-source-root, /checkstyle/@name )">
		<xsl:param name="src" select="translate($junit-source-root, '\:', '/_')"/>
		</xsl:when>
		<xsl:otherwise>
		<xsl:param name="src" select="translate($source-root, '\:', '/_')"/>
		</xsl:otherwise>
		</xsl:choose-->


	<xsl:include href="../build-templates.xsl" />
	<xsl:include href="../html-templates.xsl" />


	<xsl:template match="checkstyle">
		<xsl:comment>
			Build from source at
			<xsl:value-of select="$source-root" />
		</xsl:comment>
		<!-- xsl:comment>Which was mapped to  <xsl:value-of select="$src"/></xsl:comment-->

		<checkstyle>
			<xsl:apply-templates select="//file" />
		</checkstyle>
	</xsl:template>

	<xsl:variable name="test" select="" />


	<xsl:template match="file">
		<xsl:apply-templates select="." mode="add-package-and-classname" />
	</xsl:template>

	<xsl:template match="@*|node()|text()">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|text()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="*|@*" mode="add-package-and-classname">
		<xsl:variable name="src">
			<xsl:choose>
				<xsl:when test="contains(@name, $junit-source-root)">
					<xsl:value-of select="translate($junit-source-root, '\:', '/_')" />
				</xsl:when>
				
				<xsl:otherwise>
					<xsl:value-of select="translate($source-root, '\:', '/_')" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:param name="path" select="@name"/>

		<xsl:variable name="clz">
			<xsl:call-template name="after-last">
				<xsl:with-param name="string" select="substring-after(translate(@name, '\:', '/_'), $src)" />
				<xsl:with-param name="search" select="'/'" />
			</xsl:call-template>
		</xsl:variable>

		<xsl:variable name="ext">
			<xsl:call-template name="extension">
				<xsl:with-param name="path" select="$path" />
			</xsl:call-template>
		</xsl:variable>
		
		<file>
			<xsl:attribute name="class">
				<xsl:choose>
					<xsl:when test="$ext = 'java'">
						<xsl:value-of select="substring-before($clz, '.java')" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$clz" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			
			<xsl:attribute name="package">
				<xsl:value-of select="translate(substring-before(substring-after(translate($path, '\:', '/_'), $src), $clz), '/', '.')" />
			</xsl:attribute>
			
			<xsl:copy-of select="@*[not(.='')]|node()" />
		</file>
	</xsl:template>


	<xsl:template name="extension">
		<xsl:param name="path" />
		<xsl:choose>
			<xsl:when test="contains($path, '.')">
				<xsl:call-template name="after-last">
					<xsl:with-param name="string" select="$path" />
					<xsl:with-param name="search" select="'.'" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="''" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:redirect="org.apache.xalan.xslt.extensions.Redirect"
  extension-element-prefixes="redirect">

  <xsl:param name="properties-xml" select="''"/>
  <xsl:param name="excludes" select="''"/>
  <xsl:param name="title"
    select="document($properties-xml)/properties/property[@name='ant.project.name']/@value"/>

  <xsl:output method="html" version="1.0" encoding="iso-8859-1" indent="yes"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>

  <xsl:template match="statistics">
    <html>
      <head>
        <link href="style/stylesheet.css" rel="stylesheet" media="screen"/>
      </head>
      <body>
        <table width="100%">
          <tr>
            <td>
              <h1>Build Report</h1>
              <h2>
                <xsl:value-of select="$title"/>
              </h2>
            </td>
            <td align="center">
              <h1>Acceptability</h1>
              <h1>
              <xsl:value-of select="@acceptability"/> %
              </h1>
            </td>
            <td align="right">
              <img src="style/project-logo.png"/>
            </td>
          </tr>
        </table>
        <table>
          <xsl:for-each select="tool">
            <xsl:call-template name="tool-report">
              <xsl:with-param name="tool" select="."/>
            </xsl:call-template>
          </xsl:for-each>
        </table>
        <xsl:if test="$excludes != ''">
        <p>* Excludes reporting on some files. Excludes filter is "<xsl:value-of select="$excludes"/>"</p>
        </xsl:if>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="tool-report">
    <xsl:param name="tool"/>
    <tr>
      <td colspan="3">
        <hr/>
      </td>
    </tr>
    <tr>
      <td width="20%" colspan="3" class="heading">
        <a>
          <xsl:attribute name="href">
            <xsl:value-of select="concat($tool/@root, 'index.html')"/>
          </xsl:attribute>
          <xsl:value-of select="$tool/@description"/>
        </a>
      </td>
    </tr>
    <xsl:for-each select="statistic">
      <xsl:call-template name="row-check">
        <xsl:with-param name="success" select="@success"/>
        <xsl:with-param name="text" select="@text"/>
        <xsl:with-param name="description" select="@description"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="row-check">
    <xsl:param name="success"/>
    <xsl:param name="text"/>
    <xsl:param name="description"/>

    <xsl:variable name="class">
      <xsl:choose>
        <xsl:when test="$success = 'true'">
          <xsl:value-of select="'success'"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'failure'"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="icon">
      <xsl:choose>
        <xsl:when test="$success = 'true'">
          <xsl:value-of select="'style/pass.png'"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'style/fail.png'"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <tr>
      <td width="10px">
        <img>
          <xsl:attribute name="src">
            <xsl:value-of select="$icon"/>
          </xsl:attribute>
        </img>
      </td>
      <td> 
        <xsl:value-of select="$description"/>
      </td>
      <td>
        <xsl:attribute name="class">
          <xsl:value-of select="$class"/>
        </xsl:attribute>
        <xsl:value-of select="$text"/> 
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
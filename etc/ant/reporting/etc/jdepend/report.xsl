<xsl:stylesheet  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:redirect="http://xml.apache.org/xalan/redirect"
  extension-element-prefixes="redirect">
<xsl:output method="html" indent="yes" encoding="US-ASCII"/>

  <xsl:param name="output.dir" select="'reports/jdepend'"/>

  <xsl:include href="../build-templates.xsl"/>
  <xsl:include href="../html-templates.xsl"/>

<xsl:template match="JDepend">
   <!-- create the index.html -->
   <redirect:write file="{$output.dir}/index.html">
      <xsl:call-template name="index.html"/>
   </redirect:write>


   <!-- create the overview-packages.html at the root -->
  <redirect:write file="{$output.dir}/overview-summary.html">
    <xsl:apply-templates select="." mode="overview.packages"/>
  </redirect:write>

  <!-- create the all-cycles.html at the root -->
  <redirect:write file="{$output.dir}/all-cycles.html">
    <xsl:apply-templates select="Cycles" mode="all.cycles"/>
  </redirect:write>

  <xsl:for-each select="Cycles/Package">
    <redirect:write file="{$output.dir}/cycle-{@Name}.html">
      <xsl:call-template name="cycle-html">
        <xsl:with-param name="package" select="."/>
      </xsl:call-template>
    </redirect:write>
  </xsl:for-each>


</xsl:template>


<xsl:template name="index.html">
<html>
   <head>
      <title>JDepend Analysis</title>
   </head>
      <frameset cols="20%,80%">
         <frame src="all-cycles.html" name="classListFrame" target="classFrame"/>
         <frame src="overview-summary.html" name="classFrame"/>
      </frameset>
      <noframes>
         <h2>Frame Alert</h2>
         <p>
            This document is designed to be viewed using the frames feature. If you see this message, you are using a non-frame-capable web client.
         </p>
      </noframes>
</html>
</xsl:template>


  <xsl:template name="cycle-html">
    <xsl:param name="package"/>
      <html>
        <xsl:call-template name="html-head"/>
          <body>
            <xsl:call-template name="pageHeader"/>
            <h2><xsl:value-of select="@Name"/></h2>
            <p>This package is part of a cycle. The other packages in the cycle are:</p>
            <p>

              <table>
                <xsl:for-each select="Package">
                  <tr>
                    <td nowrap="nowrap">
         <a target="classFrame">
           <xsl:attribute name="href">
             <xsl:value-of select="concat('cycle-', . , '.html')"/>
           </xsl:attribute>
           <xsl:value-of select="."/></a>
    </td>
                  </tr>
                </xsl:for-each>
              </table>
            </p>
    </body>
    </html>

  </xsl:template>


<!-- 
	 Calculates the number of cyclic dependencies that a package has, I am not 100%
 	 sure that this makes sense as it fails on having dependencies within the same logical
 	 grouping, ie com.bt.capabilities.sms has a dependency on com.bt.capabilities.sms.dataaccess
 	 that seems perfectly reasonable to me that it breaks the structure of the code into sub
 	 packages.  Mighte need reviewing once the BRL is implemented team wide.
 -->
<xsl:template match="JDepend" mode="overview.packages">
   <html>
      <xsl:call-template name="html-head"/>
      <body>
         <xsl:call-template name="pageHeader"/>
        <h2>Overview</h2>
        <p>
          <xsl:choose>
          <xsl:when test="count(Cycles/Package) = 0">
            <p>There are no cyclic dependencies.</p>
          </xsl:when>
          <xsl:otherwise>
            <p><xsl:value-of select="concat('There are ', count(Cycles/Package) ,' cyclic dependent packages in this code.')"/></p>
          </xsl:otherwise>
        </xsl:choose>
        </p>
        <p>
          A cyclic dependency is when a package is dependent on another, which is in turn dependent on the first. A cyclic dependency can have more than two packages in the cycle.
        </p>

      </body>
   </html>
</xsl:template>

<xsl:template match="JDepend" mode="cycles.details">
   <html>
      <xsl:call-template name="html-head"/>
      <body>
         <h2><a href="index.html.html" target="_top">Home</a></h2>
         <xsl:call-template name="pageHeader"/>
  <table width="100%"><tr align="left"><h2>Cycles</h2><td>
  </td><td align="right">
  [<a href="overview-summary.html">summary</a>]
  [<a href="overview-packages.html">packages</a>]
  [cycles]
  [<a href="overview-explanations.html">explanations</a>]
   </td></tr></table>
  <!--<table width="100%"><tr><td>
  </td><td align="right">
    [<a href="#NVsummary">summary</a>]
  [<a href="#NVpackages">packages</a>]
  [<a href="#NVcycles">cycles</a>]
   [<a href="#NVexplanations">explanations</a>]
  </td></tr></table> -->

  <xsl:if test="count(Cycles/Package) = 0">
    <p>There are no cyclic dependancies.</p>
  </xsl:if>
  <xsl:for-each select="Cycles/Package">
     <h3><a><xsl:attribute name="name">#CY<xsl:value-of select="@Name"/></xsl:attribute><xsl:value-of select="@Name"/></a></h3><p>
    <xsl:for-each select="Package">
      <xsl:value-of select="."/><br/>
    </xsl:for-each></p>
  </xsl:for-each>
  <!-- this is often a long listing; provide a lower navigation table also -->
  <table width="100%"><tr align="left"><td></td><td align="right">
  [<a href="overview-summary.html">summary</a>]
  [<a href="overview-packages.html">packages</a>]
  [cycles]
  [<a href="overview-explanations.html">explanations</a>]
   </td></tr></table>
  </body>
  </html>
</xsl:template>





<!--
I do not know JDepend enough to know if every error results in a non-analyzed package,
but that is how I am presenting it to the viewer.  This may need to change.
  @bug there will be a problem here, I don't know yet how to handle unnamed package :(
-->
<xsl:template match="JDepend/Packages/Package" mode="all.packages.nolink">
  <tr>
    <td nowrap="nowrap">
       Not Analyzed: <xsl:value-of select="@name"/>
    </td>
  </tr>
</xsl:template>

<!--
Creates an html file that contains a link to all package links in overview-cycles.html.
  @bug there will be a problem here, I don't know yet how to handle unnamed package :(
-->
<xsl:template match="JDepend/Cycles" mode="all.cycles">
  <html>
    <xsl:call-template name="html-head"/>
    <body>
      <h2><a href="index.html" target="_top">Home</a></h2>
      <h2>Cycles</h2>
        <table width="100%">
           <xsl:apply-templates select="Package" mode="all.cycles">
            <xsl:sort select="@Name"/>
          </xsl:apply-templates>
        </table>
    </body>
  </html>
</xsl:template>

<xsl:template match="JDepend/Cycles/Package" mode="all.cycles">
  <tr>
    <td nowrap="nowrap">
         <a href="cycle-{@Name}.html" target="classFrame"><xsl:value-of select="@Name"/></a>
    </td>
  </tr>
</xsl:template>

<!-- Page HEADER -->
<xsl:template name="pageHeader">
			  <xsl:call-template name="html-heading">
          <xsl:with-param name="name" select="'Cyclic Dependency Report'"/>
			  </xsl:call-template>
</xsl:template>

</xsl:stylesheet>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:lxslt="http://xml.apache.org/xslt"
                xmlns:redirect="http://xml.apache.org/xalan/redirect"
                extension-element-prefixes="redirect">
  <xsl:output method="html" indent="yes" encoding="US-ASCII"/>

  <xsl:param name="output.dir" select="'reports/emma'"/>

  <xsl:include href="../html-templates.xsl"/>
  <xsl:include href="../build-templates.xsl"/>

  <xsl:template match="/">

    <redirect:write file="{$output.dir}/index.html">
      <xsl:call-template name="index.html"/>
    </redirect:write>

    <!-- create the overview-packages.html at the root -->
    <redirect:write file="{$output.dir}/overview-summary.html">
      <html>
        <head>
          <title>Unit Test Results: Summary</title>
          <xsl:call-template name="create.stylesheet.link">
            <xsl:with-param name="package.name"/>
          </xsl:call-template>
        </head>
        <body>
          <xsl:call-template name="pageHeader">
            <xsl:with-param name="package.name"/>
          </xsl:call-template>
          <a href="raw.html" target="_top">Raw EMMA Report</a>

          <h2>Summary</h2>
          <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
            <tr valign="top">
              <th>Package</th>
              <th>Class Coverage</th>
              <th>Method Coverage</th>
              <th>Block Coverage</th>
              <th>Line Coverage</th>
            </tr>
            <xsl:apply-templates select="package[not(./@name = preceding-sibling::package/@name)]"
                                 mode="overview.packages">
              <xsl:sort select="@name"/>
            </xsl:apply-templates>
            <xsl:apply-templates select="." mode="overview.packages"/>
          </table>
        </body>
      </html>
    </redirect:write>

    <!-- create the all-packages.html at the root -->
    <redirect:write file="{$output.dir}/overview-frame.html">
      <html>
        <head>
          <xsl:call-template name="create.stylesheet.link">
            <xsl:with-param name="package.name"/>
          </xsl:call-template>
        </head>
        <body>
          <h2>
            <a href="index.html" target="_top">Home</a>
          </h2>
          <h2>Packages</h2>
          <table width="100%">
            <xsl:apply-templates select="." mode="all.packages"/>
          </table>
        </body>
      </html>
    </redirect:write>

    <!-- create the all-classes.html at the root -->
    <redirect:write file="{$output.dir}/allclasses-frame.html">
      <html>
        <head>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="package.name"/>
            </xsl:call-template>
        </head>
        <body>
            <h2>All Classes</h2>
            <table width="100%">
             <xsl:apply-templates select="//class[not(./@name = preceding-sibling::class/@name)]"
                                 mode="all.classes">
              <xsl:sort select="@name"/>
            </xsl:apply-templates>
            </table>
        </body>
    </html>
    </redirect:write>

    <xsl:apply-templates select="//class" mode="class.details"></xsl:apply-templates>
    <xsl:apply-templates select="//package" mode="package.details"></xsl:apply-templates>
  </xsl:template>

  <xsl:template name="index.html">
    <html>
      <head>
        <title>Code Coverage Results.</title>
      </head>
      <frameset cols="20%,80%">
        <frameset rows="30%,70%">
          <frame src="overview-frame.html" name="packageListFrame"/>
          <frame src="allclasses-frame.html" name="classListFrame"/>
        </frameset>
        <frame src="overview-summary.html" name="classFrame"/>
        <noframes>
          <h2>Frame Alert</h2>
          <p>
            This document is designed to be viewed using the frames feature. If you see this message, you are using a
            non-frame-capable web client.
          </p>
        </noframes>
      </frameset>
    </html>
  </xsl:template>

  <xsl:template match="report/data/all" mode="all.packages">

    <xsl:apply-templates select="package[not(./@name = preceding-sibling::package/@name)]" mode="all.packages">
      <xsl:sort select="@name"/>
    </xsl:apply-templates>

  </xsl:template>

  <xsl:template match="report/data/all" mode="overview.packages">

    <xsl:apply-templates select="package[not(./@name = preceding-sibling::package/@name)]" mode="overview.packages">
      <xsl:sort select="@name"/>
    </xsl:apply-templates>

  </xsl:template>

  <xsl:template match="package" mode="all.packages">
    <tr>
      <td nowrap="nowrap">
        <a href="./{translate(@name,'.','/')}/package-summary.html" target="classFrame">
          <xsl:value-of select="@name"/>
          <xsl:if test="@name = ''">&lt;none&gt;</xsl:if>
        </a>
        |
        <xsl:value-of select="coverage[@type='line, %']/@value"/>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="package" mode="overview.packages">
    <tr>
      <td nowrap="nowrap">
        <xsl:value-of select="@name"/>
      </td>
      <td nowrap="nowrap">
        <xsl:value-of select="coverage[@type='class, %']/@value"/>
      </td>
      <td nowrap="nowrap">
        <xsl:value-of select="coverage[@type='method, %']/@value"/>
      </td>
      <td nowrap="nowrap">
        <xsl:value-of select="coverage[@type='block, %']/@value"/>
      </td>
      <td nowrap="nowrap">
        <xsl:value-of select="coverage[@type='line, %']/@value"/>
      </td>
    </tr>
  </xsl:template>


  <!--
  called to produce a list of all classes
  -->
  <xsl:template match="class" mode="all.classes">
    <xsl:variable name="package" select="../../@name"/>
    <tr>
        <td nowrap="nowrap">
            <a target="classFrame">
                <xsl:attribute name="href">
                    <xsl:if test="not($package='')">
                        <xsl:value-of select="translate($package,'.','/')"/><xsl:text>/</xsl:text>
                    </xsl:if><xsl:value-of select="@name"/><xsl:text>.html</xsl:text>
                </xsl:attribute>
                <xsl:value-of select="@name"/>
            </a>
        </td>
    </tr>
  </xsl:template>

  <!--
  creates the class detail pages
  -->
  <xsl:template match="class" mode="class.details">
    <xsl:variable name="package" select="../../@name"/>
    <xsl:variable name="filename"><xsl:if test="not($package='')"><xsl:value-of select="translate($package,'.','/')"/><xsl:text>/</xsl:text></xsl:if><xsl:value-of select="@name"/><xsl:text>.html</xsl:text></xsl:variable>
    <xsl:variable name="prefix-url">
    <xsl:call-template name="back.url">
      <xsl:with-param name="name" select="concat($package, '/')"/>
    </xsl:call-template>
  </xsl:variable>

    <redirect:write file="{$output.dir}/{$filename}">
      <html>
        <head>
          <xsl:call-template name="create.stylesheet.link">
            <xsl:with-param name="package.name" select="$package"/>
          </xsl:call-template>

        </head>
        <body>
            <xsl:call-template name="html-heading-class">
              <xsl:with-param name="prefix-url" select="$prefix-url"/>
              <xsl:with-param name="package" select="$package"/>
              <xsl:with-param name="class" select="@name"/>
              <xsl:with-param name="name" select="'Code Coverage'"/>
            </xsl:call-template>

          <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
            <tr>
              <th>Class Coverage</th>
              <th>Method Coverage</th>
              <th>Block Coverage</th>
              <th>Line Coverage</th>
            </tr>
            <tr>
              <td nowrap="nowrap">
                <xsl:value-of select="coverage[@type='class, %']/@value"/>
              </td>
              <td nowrap="nowrap">
                <xsl:value-of select="coverage[@type='method, %']/@value"/>
              </td>
              <td nowrap="nowrap">
                <xsl:value-of select="coverage[@type='block, %']/@value"/>
              </td>
              <td nowrap="nowrap">
                <xsl:value-of select="coverage[@type='line, %']/@value"/>
              </td>
            </tr>
          </table>

          <h2>Coverage</h2>
          <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
            <tr>
              <th>Method</th>
              <th>Total Coverage</th>
              <th>Block Coverage</th>
              <th>Line Coverage</th>
            </tr>

            <xsl:for-each select="method[not(./@name = preceding-sibling::method/@name)]">
              <xsl:sort select="@name"/>
              <tr>
                <td nowrap="nowrap">
                  <xsl:value-of select="@name"/>
                </td>
                <td nowrap="nowrap">
                  <xsl:value-of select="coverage[@type='method, %']/@value"/>
                </td>
                <td nowrap="nowrap">
                  <xsl:value-of select="coverage[@type='block, %']/@value"/>
                </td>
                <td nowrap="nowrap">
                  <xsl:value-of select="coverage[@type='line, %']/@value"/>
                </td>
              </tr>
            </xsl:for-each>
          </table>


        </body>
      </html>
    </redirect:write>
  </xsl:template>

  <!--

  TEMPLATE for a package summary
  -->
  <xsl:template match="package" mode="package.details">
      <xsl:variable name="package" select="@name"/>
      <xsl:variable name="filename"><xsl:if test="not($package='')"><xsl:value-of select="translate($package,'.','/')"/><xsl:text>/</xsl:text></xsl:if><xsl:text>package-summary.html</xsl:text></xsl:variable>
    <xsl:variable name="prefix-url">
      <xsl:call-template name="back.url">
        <xsl:with-param name="name" select="concat($package, '/')"/>
      </xsl:call-template>
    </xsl:variable>
    <redirect:write file="{$output.dir}/{$filename}">
      <html>
        <head>
          <xsl:call-template name="create.stylesheet.link">
            <xsl:with-param name="package.name" select="$package"/>
          </xsl:call-template>

        </head>
        <body>
          <xsl:call-template name="html-heading-class">
            <xsl:with-param name="prefix-url" select="$prefix-url"/>
            <xsl:with-param name="package" select="$package"/>
            <xsl:with-param name="class" select="''"/>
            <xsl:with-param name="name" select="'Package Code Coverage'"/>
          </xsl:call-template>
          <h2>Classes</h2>
          <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
            <tr>
              <th>Class</th>
              <th>Class Coverage</th>
              <th>Method Coverage</th>
              <th>Block Coverage</th>
              <th>Line Coverage</th>
            </tr>
            <xsl:for-each select="srcfile/class[not(./@name = preceding-sibling::srcfile/class/@name)]">
              <xsl:sort select="@name"/>
            <tr>
              <td><a><xsl:attribute name="href"><xsl:value-of select="@name"/>.html</xsl:attribute><xsl:value-of select="@name"/></a></td>
              <td><xsl:value-of select="coverage[@type='class, %']/@value"/></td>
              <td><xsl:value-of select="coverage[@type='method, %']/@value"/></td>
              <td><xsl:value-of select="coverage[@type='block, %']/@value"/></td>
              <td><xsl:value-of select="coverage[@type='line, %']/@value"/></td>
            </tr>
            </xsl:for-each>
          </table>

        </body>
      </html>
    </redirect:write>
  </xsl:template>





  <!-- create the link to the stylesheet based on the package name -->
  <xsl:template name="create.stylesheet.link">
    <xsl:param name="package.name"/>
    <link rel="stylesheet" type="text/css" title="Style">
      <xsl:attribute name="href">../<xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name"/></xsl:call-template></xsl:if>style/stylesheet.css</xsl:attribute>
    </link>
  </xsl:template>


  <!-- Page HEADER -->
  <xsl:template name="pageHeader">
    <xsl:param name="package.name"/>
    <table width="100%">
      <tr>
        <td align="left">
          <h1>Code Coverage Results</h1>
        </td>
        <td align="right">
          <img>
            <xsl:attribute name="src">../<xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name"/></xsl:call-template></xsl:if>style/project-logo.png</xsl:attribute>
          </img>
        </td>
      </tr>
    </table>
    <hr size="1"/>
  </xsl:template>

  <!--
      transform string like a.b.c to ../../../
      @param path the path to transform into a descending directory path
  -->
  <xsl:template name="path">
    <xsl:param name="path"/>
    <xsl:if test="contains($path,'.')">
      <xsl:text>../</xsl:text>
      <xsl:call-template name="path">
        <xsl:with-param name="path">
          <xsl:value-of select="substring-after($path,'.')"/>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:if>
    <xsl:if test="not(contains($path,'.')) and not($path = '')">
      <xsl:text>../</xsl:text>
    </xsl:if>
  </xsl:template>


</xsl:stylesheet>

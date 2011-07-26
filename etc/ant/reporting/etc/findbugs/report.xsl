<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:redirect="http://xml.apache.org/xalan/redirect"
  xmlns:stringutils="xalan://org.apache.tools.ant.util.StringUtils"
  extension-element-prefixes="redirect">

  <xsl:output method="html" indent="yes" encoding="US-ASCII"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>
  <xsl:param name="output.dir" select="'reports/findbugs'"/>
  <xsl:param name="messages.xml" select="'.'"/>

  <xsl:include href="../build-templates.xsl"/>
  <xsl:include href="../html-templates.xsl"/>

  <xsl:key name="kPackage" match="Class" use="@package"/>
  <xsl:key name="kClass" match="Class" use="@class"/>
  <xsl:key name="kClassname" match="Class" use="@classname"/>

  <xsl:variable name="mm" select="document($messages.xml)"/>

  <xsl:template match="BugCollection">
    <xsl:variable name="bugs" select="BugInstance"/>

    <redirect:write file="{$output.dir}/index.html">
      <xsl:call-template name="index.html"/>
    </redirect:write>

    <redirect:write file="{$output.dir}/overview-frame.html">
      <xsl:call-template name="overview-frame.html">
        <xsl:with-param name="classes" select="//Class"/>
      </xsl:call-template>
    </redirect:write>

    <redirect:write file="{$output.dir}/allclasses-frame.html">
      <xsl:call-template name="allclasses-frame.html">
        <xsl:with-param name="classes" select="//Class"/>
      </xsl:call-template>
      <xsl:for-each
            select="//Class[generate-id()=generate-id(key('kClassname', @classname)[1])]">
        <xsl:call-template name="classes.html">
          <xsl:with-param name="class" select="."/>
          <xsl:with-param name="bugs" select="$bugs"/>
        </xsl:call-template>
      </xsl:for-each>
    </redirect:write>

    <xsl:call-template name="allclasses-frame.html">
      <xsl:with-param name="classes" select="//Class"/>
    </xsl:call-template>

    <xsl:for-each
          select="//Class[generate-id()=generate-id(key('kPackage', @package)[1])]">
      <xsl:variable name="filename">
        <xsl:call-template name="output-file-for-package">
          <xsl:with-param name="package" select="@package"/>
          <xsl:with-param name="suffix" select="'package-summary'"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="package" select="@package" />

      <redirect:write file="{$output.dir}/{$filename}">
        <xsl:call-template name="package-summary.html">
          <xsl:with-param name="package" select="@package"/>
          <xsl:with-param name="bugs" select="../../BugInstance[Class/@package=$package]"/>
        </xsl:call-template>
      </redirect:write>
    </xsl:for-each>


    <redirect:write file="{$output.dir}/overview-summary.html">
      <xsl:call-template name="overview-summary.html">
        <xsl:with-param name="bugs" select="."/>
      </xsl:call-template>
    </redirect:write>


  </xsl:template>

  <xsl:template name="package-summary.html">
    <xsl:param name="package"/>
    <xsl:param name="bugs"/>

    <xsl:variable name="prefix-url">
      <xsl:if test="not($package = '')">
        <xsl:call-template name="back.url">
          <xsl:with-param name="name" select="concat($package, '/')"/>
        </xsl:call-template>
      </xsl:if>
    </xsl:variable>

    <html>
      <xsl:call-template name="html-head">
        <xsl:with-param name="prefix-url" select="$prefix-url"/>
      </xsl:call-template>
      <body>
        <xsl:call-template name="html-heading">
          <xsl:with-param name="name" select="'Package Summary'"/>
          <xsl:with-param name="subname" select="$package"/>
          <xsl:with-param name="prefix-url" select="$prefix-url"/>
        </xsl:call-template>
      </body>
    </html>
    <table>
      <tr>
        <th>Priority</th>
        <th>Class</th>
        <th>Bug</th>
      </tr>

      <xsl:for-each select="$bugs">
        <xsl:sort select="@priority"  order="ascending" data-type="number"/>
        <xsl:sort select="Class/@name"/>
        <tr>
          <td>
            <xsl:call-template name="level">
              <xsl:with-param name="level" select="@priority"/>
            </xsl:call-template>
          </td>
          <td>
            <a>
              <xsl:attribute name="href">
                <xsl:value-of select="concat(Class/@class, '.html')"/>
              </xsl:attribute>
              <xsl:value-of select="Class/@class"/>
            </a>
          </td>
          <td>
            <xsl:call-template name="bug-description">
              <xsl:with-param name="type" select="@type"/>
            </xsl:call-template>
          </td>
        </tr>
      </xsl:for-each>
    </table>
  </xsl:template>

  <xsl:template name="bug-summary-row">
    <xsl:param name="bug"/>


  </xsl:template>

  <xsl:template name="index.html">
    <html>
      <head>
        <title>Bugs</title>
      </head>
      <frameset cols="20%,80%">
        <frameset rows="30%,70%">
          <frame src="overview-frame.html" name="packageListFrame"/>
          <frame src="allclasses-frame.html" name="classListFrame"/>
        </frameset>
        <frame src="overview-summary.html" name="classFrame"/>
        <noframes>
        </noframes>
      </frameset>
    </html>
  </xsl:template>

  <xsl:template name="overview-summary.html">
    <xsl:param name="bugs"/>

    <html>
      <xsl:call-template name="html-head"/>
      <body>
        <xsl:call-template name="html-heading">
          <xsl:with-param name="name" select="'Bugs'"/>
        </xsl:call-template>

        <h2>Bug Summary</h2> 
        You can also examine the <a href="raw.html" target="_top">Raw FindBugs Report</a>.
        <p>
          Bugs are split into 3 priorities: High, Medium and Low. There were a total of
          <xsl:value-of select="count($bugs/BugInstance)"/>
bugs in this code which could be determinted just by looking at it.
        </p>
        <table>
          <tr>
            <th>Priority</th>
            <th>Count</th>
          </tr>
          <tr>
            <td>High</td>
            <td>
              <xsl:value-of select="count($bugs/BugInstance[@priority='1'])"/>
            </td>
          </tr>
          <tr>
            <td>Medium</td>
            <td>
              <xsl:value-of select="count($bugs/BugInstance[@priority='2'])"/>
            </td>
          </tr>
          <tr>
            <td>Low</td>
            <td>
              <xsl:value-of select="count($bugs/BugInstance[@priority='3'])"/>
            </td>
          </tr>
        </table>

      </body>


    </html>
  </xsl:template>

  <xsl:template name="classes.html">
    <xsl:param name="class"/>
    <xsl:param name="bugs"/>
    <xsl:choose>
      <xsl:when test="@role"/>
      <xsl:otherwise>
        <xsl:variable name="filename" select="concat(translate($class/@package, '.', '/'), '/', $class/@class, '.html')"/>
        <xsl:variable name="prefix-url">
          <xsl:if test="not($class/@package = '')">
            <xsl:call-template name="back.url">
              <xsl:with-param name="name" select="concat($class/@package, '/')"/>
            </xsl:call-template>
          </xsl:if>
        </xsl:variable>
        <redirect:write file="{$output.dir}/{$filename}">
          <html>
            <xsl:call-template name="html-head">
              <xsl:with-param name="prefix-url" select="$prefix-url" />
            </xsl:call-template>
            <body>

              <xsl:call-template name="html-heading-class">
                <xsl:with-param name="name" select="'Bugs'"/>
                <xsl:with-param name="prefix-url" select="$prefix-url" />
                <xsl:with-param name="class" select="$class/@class" />
                <xsl:with-param name="package" select="$class/@package" />
                <xsl:with-param name="prefix-url" select="$prefix-url" />
              </xsl:call-template>


              <table>


                <xsl:for-each select="$bugs[Class/@classname=$class/@classname]">

                  <xsl:variable name="bug-description">
                    <xsl:call-template name="bug-description">
                      <xsl:with-param name="type" select="@type"/>
                    </xsl:call-template>
                  </xsl:variable>

                  <xsl:variable name="bug-detail">
                    <xsl:call-template name="bug-detail">
                      <xsl:with-param name="type" select="@type"/>
                    </xsl:call-template>
                  </xsl:variable>

                  <xsl:variable name="bug-code">
                    <xsl:call-template name="bug-code">
                      <xsl:with-param name="abbreviation" select="@abbrev"/>
                    </xsl:call-template>
                  </xsl:variable>

                  <tr>
                    <th colspan="2">
                      <xsl:value-of select="$bug-description"/>
                    </th>
                  </tr>
                  <tr>
                    <td width="50%">
                      <table>
                        <xsl:for-each select="*">
                          <tr>
                            <td align="top">
                              <xsl:apply-templates select="." mode="breadcrumb">
                                <xsl:with-param name="prefix-url" select="$prefix-url"/>
                              </xsl:apply-templates>
                            </td>
                          </tr>
                        </xsl:for-each>
                      </table>
                    </td>
                    <td>
                      <xsl:value-of select="$bug-detail" disable-output-escaping="yes"/>
                    </td>
                  </tr>
                </xsl:for-each>
              </table>

            </body>
          </html>
        </redirect:write>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="Class" mode="breadcrumb">
    <xsl:param name="prefix-url" select="'./'"/>
    <xsl:if test="@role">
      <xsl:value-of select="concat('References ',@classname)"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="Method" mode="breadcrumb">
    <xsl:param name="prefix-url" select="'./'"/>
    <xsl:variable name="pretty">
      <xsl:call-template name="pretty-method-signature">
        <xsl:with-param name="method" select="@name"/>
        <xsl:with-param name="signature" select="@signature"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:if test="string-length(@role = 0)">
      <h3>
        <xsl:value-of select="$pretty" disable-output-escaping="yes"/>
      </h3>
    </xsl:if>

    <xsl:if test="@role = 'METHOD_CALLED'">
      <xsl:value-of select="concat('calling ', $pretty)" disable-output-escaping="yes"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="Field" mode="breadcrumb">
    <xsl:param name="prefix-url" select="'./'"/>
    <h3>
      <strong>
        <xsl:value-of select="concat(@name,' ')"/>
      </strong>
      <xsl:if test="@isStatic = 'true'">
        <xsl:value-of select="'static '"/>
      </xsl:if>
      <xsl:call-template name="pretty-java-type">
        <xsl:with-param name="signature" select="@signature"/>
      </xsl:call-template>
    </h3>
  </xsl:template>


  <xsl:template match="SourceLine" mode="breadcrumb">
    <xsl:param name="prefix-url" select="'./'"/>
    <a>
      <xsl:attribute name="href">
        <xsl:value-of select="concat($prefix-url, '../../reports/java2html/', translate(@classname, '.', '/'), '.html', '#', @start)"/>
      </xsl:attribute>
      <xsl:value-of select="concat('( line ', @start, ' to ', @end, ')')"/>
    </a>
  </xsl:template>



  <xsl:template name="allclasses-frame.html">
    <xsl:param name="classes"/>

    <html>
      <xsl:call-template name="html-head"/>
      <body>
        <h2>All Classes</h2>
        <table width="100%">
          <xsl:for-each
            select="$classes[generate-id()=generate-id(key('kClass', @class)[1])]">
            <xsl:sort select="@class"/>
            <xsl:choose>
              <xsl:when test="@role"/>
              <xsl:otherwise>
                <tr>
                  <td nowrap="nowrap">
                    <a target="classFrame">
                      <xsl:attribute name="href">
                        <xsl:value-of select="concat(translate(@package, '.', '/'), '/', @class, '.html')"/>
                      </xsl:attribute>
                      <xsl:value-of select="@class"/>
                    </a>
                  </td>
                </tr>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </table>
      </body>
    </html>

  </xsl:template>

  <xsl:template name="overview-frame.html">
    <xsl:param name="classes"/>
    <html>
      <xsl:call-template name="html-head"/>
      <body>
        <h2>
          <a target="classFrame" href="overview-summary.html">Home</a>
        </h2>
        <h2>Packages</h2>
        <table width="100%">
          <xsl:for-each
            select="$classes[generate-id()=generate-id(key('kPackage', @package)[1])]">
            <xsl:choose>
              <xsl:when test="@role"/>
              <xsl:otherwise>
                <tr>
                  <td nowrap="nowrap">
                    <a target="classFrame">
                      <xsl:attribute name="href">
                        <xsl:value-of select="concat(translate(@package, '.', '/'), '/package-summary.html')"/>
                      </xsl:attribute>
                      <xsl:value-of select="@package"/>
                    </a>
                  </td>
                </tr>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </table>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="BugInstance" mode="overview-frame">
    <html>
      <h1>cock</h1>
    </html>
  </xsl:template>

  <xsl:template name="bug-description">
    <xsl:param name="type"/>
<xsl:value-of select="$mm/MessageCollection/BugPattern[@type=$type]/ShortDescription"/>
    <xsl:value-of select="''"/>
  </xsl:template>

  <xsl:template name="bug-detail">
    <xsl:param name="type"/>
    <xsl:value-of select="$mm/MessageCollection/BugPattern[@type=$type]/Details"/>
    <xsl:value-of select="''"/>
  </xsl:template>

  <xsl:template name="bug-code">
    <xsl:param name="abbreviation"/>
   <xsl:value-of select="$mm/MessageCollection/BugCode[@abbrev=$abbreviation]"/>
    <xsl:value-of select="''"/>
  </xsl:template>

  <xsl:template name="level">
    <xsl:param name="level"/>
    <xsl:if test="$level = 1">
      <xsl:value-of select="'High'"/>
    </xsl:if>
    <xsl:if test="$level = 2">
      <xsl:value-of select="'Medium'"/>
    </xsl:if>
    <xsl:if test="$level = 3">
      <xsl:value-of select="'Low'"/>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>

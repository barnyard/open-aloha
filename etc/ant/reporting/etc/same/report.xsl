<xsl:stylesheet
  xmlns:xsl=          "http://www.w3.org/1999/XSL/Transform"
  xmlns:lxslt=        "http://xml.apache.org/xslt"
  xmlns:redirect=     "http://xml.apache.org/xalan/redirect"
  xmlns:stringutils=  "xalan://org.apache.tools.ant.util.StringUtils"
  xmlns:func=         "http://exslt.org/functions"
  xmlns:cr=           "http://cr.intra.bt.com/cr"
  extension-element-prefixes="xsl lxslt redirect stringutils func cr"
  version="1.0">

  <xsl:output method="html" indent="yes" encoding="US-ASCII"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>
  <xsl:param name="output.dir" select="'reports/same'"/>
  <xsl:param name="same-lines-max" select="'12'"/>
  <xsl:param name="messages.xml"/>

  <xsl:key name="kPackage" match="file" use="@package"/>
  <xsl:key name="kClass" match="file" use="@class"/>

  <xsl:include href="../build-templates.xsl"/>
  <xsl:include href="../html-templates.xsl"/>

  <xsl:template match="same">
    <xsl:variable name="same" select="."/>
    <redirect:write file="{$output.dir}/index.html">
      <xsl:call-template name="index.html"/>
    </redirect:write>

    <redirect:write file="{$output.dir}/overview-frame.html">
      <xsl:call-template name="overview-frame.html">
        <xsl:with-param name="duplications" select="$same"/>
        <xsl:with-param name="sort" select="'true'"/>
      </xsl:call-template>
    </redirect:write>

    <redirect:write file="{$output.dir}/overview-frame-worst.html">
      <xsl:call-template name="overview-frame.html">
        <xsl:with-param name="duplications" select="$same"/>
        <xsl:with-param name="sort" select="'false'"/>
      </xsl:call-template>
    </redirect:write>

    <redirect:write file="{$output.dir}/overview-summary.html">
      <xsl:call-template name="overview-summary.html">
        <xsl:with-param name="duplications" select="$same"/>
      </xsl:call-template>
    </redirect:write>

    <xsl:for-each
      select="duplication/file[generate-id()=generate-id(key('kPackage', @package)[1])]">
      <xsl:sort select="@package"/>
      <xsl:call-template name="package-overview-frame">
        <xsl:with-param name="name" select="@package"/>
        <xsl:with-param name="files" select="key('kPackage', @package)"/>
      </xsl:call-template>
    </xsl:for-each>

    <redirect:write file="{$output.dir}/allclasses-frame.html">
      <xsl:call-template name="allclasses-frame.html">
        <xsl:with-param name="duplications" select="$same"/>
        <xsl:with-param name="sort" select="'true'"/>
      </xsl:call-template>
    </redirect:write>

    <redirect:write file="{$output.dir}/allclasses-frame-worst.html">
      <xsl:call-template name="allclasses-frame.html">
        <xsl:with-param name="duplications" select="$same"/>
        <xsl:with-param name="sort" select="'false'"/>
      </xsl:call-template>
    </redirect:write>

    <xsl:for-each
      select="duplication/file[generate-id()=generate-id(key('kClass', @class)[1])]">
      <xsl:variable name="path" select="@name"/>
      <xsl:call-template name="class">
        <xsl:with-param name="package" select="@package"/>
        <xsl:with-param name="name" select="@class"/>
        <xsl:with-param name="duplications"
          select="$same/duplication[file/@name=$path]"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="overview-summary.html">
    <html>
      <body>
        <xsl:call-template name="html-head"/>
			  <xsl:call-template name="html-heading">
          <xsl:with-param name="name" select="'Duplication'"/>
			  </xsl:call-template>

        <h2>Summary Totals</h2>
        <p><xsl:value-of select="concat('For all duplicates which have a line count of more than ',$same-lines-max)"/>
        </p>
        <table width="100%">
          <tr>
            <th>Duplicated Lines</th>
            <th>Affected Packages</th>
            <th>Affected Classes</th>
          </tr>
          <tr>
            <td>
              <xsl:value-of select="sum(duplication/@length)"/>
            </td>
            <td>
              <xsl:value-of select="count(duplication/file[generate-id()=generate-id(key('kPackage', @package)[1])])"/>
            </td>
            <td>
              <xsl:value-of select="count(duplication/file[generate-id()=generate-id(key('kClass', @class)[1])])"/>
            </td>
          </tr>
        </table>
        <hr size="1"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="index.html">
    <html>
      <head>
        <title>Duplication Analysis</title>
      </head>
      <frameset cols="20%,80%">
        <frameset rows="30%,70%">
          <frame src="overview-frame.html" name="packageListFrame" title="All Packages"/>
          <frame src="allclasses-frame.html" name="packageFrame" title="All classes and interfaces (except non-static nested types)"/>
        </frameset>
        <frame src="overview-summary.html" name="classFrame" title="Package, class and interface descriptions" scrolling="yes"/>
        <noframes>
          <h2>Frame Alert</h2>
          <p>
                This document is designed to be viewed using the frames feature. If you see this message, you are using a non-frame-capable web client.
          </p>
        </noframes>
      </frameset>
    </html>
  </xsl:template>

  <xsl:template name="overview-frame.html">
    <xsl:param name="sort" select="'false'"/>
    <html>
      <xsl:call-template name="html-head"/>
      <body>
        <h2>
          <a target="_top" href="index.html">Home</a>
        </h2>
        <h2>Packages</h2>

          <xsl:choose>
            <xsl:when test="$sort = 'true'">
              <p>Name|<a href="overview-frame-worst.html">Offences</a></p>
              <table>
                <xsl:for-each
                  select="duplication/file[generate-id()=generate-id(key('kPackage', @package)[1])]">
                  <xsl:sort select="@package" order="ascending"/>

                  <xsl:variable name="package.dir">
                    <xsl:call-template name="package.dir">
                      <xsl:with-param name="name" select="@package"/>
                    </xsl:call-template>
                  </xsl:variable>
                  <tr>
                    <td>
                      <a href="{$package.dir}/package-frame.html" target="packageFrame">
                        <xsl:value-of select="@package"/>
                      </a>
                    </td>
                  </tr>
                </xsl:for-each>
              </table>
            </xsl:when>

            <xsl:otherwise>
              <p><a href="overview-frame.html">Name</a>|Offences</p>
              <table>
                <xsl:for-each
                  select="duplication/file[generate-id()=generate-id(key('kPackage', @package)[1])]">

                  <xsl:variable name="package.dir">
                    <xsl:call-template name="package.dir">
                      <xsl:with-param name="name" select="@package"/>
                    </xsl:call-template>
                  </xsl:variable>
                  <tr>
                    <td>
                      <a href="{$package.dir}/package-frame.html" target="packageFrame">
                        <xsl:value-of select="@package"/>
                      </a>
                    </td>
                  </tr>
                </xsl:for-each>
              </table>
            </xsl:otherwise>
          </xsl:choose>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="allclasses-frame.html">
    <xsl:param name="duplications"/>
    <xsl:param name="sort" select="'false'"/>
    <html>
      <xsl:call-template name="html-head"/>
      <h2>Classes</h2>
      <xsl:choose>
        <xsl:when test="$sort = 'true'">
        <p>Name|<a href="allclasses-frame-worst.html">Offences</a></p>
          <table>
            <xsl:for-each
      select="$duplications/duplication/file[generate-id()=generate-id(key('kClass', @class)[1])]">
              <xsl:sort select="@class"/>
              <xsl:variable name="url"><xsl:call-template name="package.dir"><xsl:with-param name="name" select="@package"/></xsl:call-template>/<xsl:value-of select="@class"/>.html</xsl:variable>
              <tr>
                <td>
                  <a href="{$url}" target="classFrame">
                    <xsl:value-of select="@class"/>
                  </a>
                  <xsl:value-of select="concat('  ',../@length)"/>
                </td>
              </tr>
            </xsl:for-each>
          </table>
        </xsl:when>

        <xsl:otherwise >
        <p><a href="allclasses-frame.html">Name</a>|Offences</p>
          <table>
            <xsl:for-each
      select="$duplications/duplication/file[generate-id()=generate-id(key('kClass', @class)[1])]">
              <xsl:variable name="url"><xsl:call-template name="package.dir"><xsl:with-param name="name" select="@package"/></xsl:call-template>/<xsl:value-of select="@class"/>.html</xsl:variable>
              <tr>
                <td>
                  <a href="{$url}" target="classFrame">
                    <xsl:value-of select="@class"/>
                  </a>
                  <xsl:value-of select="concat('  ',../@length)"/>
                </td>
              </tr>
            </xsl:for-each>
          </table>
        </xsl:otherwise>
      </xsl:choose>
      <p>Class lines</p>
    </html>
  </xsl:template>

  <xsl:template name="package-overview-frame">
    <xsl:param name="name"/>
    <xsl:param name="files"/>
    <xsl:variable name="package.dir">
      <xsl:call-template name="package.dir">
        <xsl:with-param name="name" select="@package"/>
      </xsl:call-template>
    </xsl:variable>

    <redirect:write file="{$output.dir}/{$package.dir}/package-frame.html">
      <html>
        <xsl:call-template name="html-head">
          <xsl:with-param name="prefix-url">
            <xsl:call-template name="back.url">
              <xsl:with-param name="name" select="@package"/>
            </xsl:call-template>
          </xsl:with-param>
        </xsl:call-template>
        <font size="+1" CLASS="FrameTitleFont">
          <xsl:value-of select="$name"/>
        </font>
        <table>
          <xsl:for-each
            select="$files[generate-id()=generate-id(key('kClass', @class)[1])]">
            <xsl:sort select="." order="ascending"/>
            <tr>
              <td>
                <a href="{@class}.html" target="classFrame">
                  <xsl:value-of select="@class"/>
                </a>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </html>
    </redirect:write>

  </xsl:template>

  <xsl:template name="class">
    <xsl:param name="name"/>
    <xsl:param name="package"/>
    <xsl:param name="duplications"/>
    <xsl:variable name="package.dir">
      <xsl:call-template name="package.dir">
        <xsl:with-param name="name" select="$package"/>
      </xsl:call-template>
    </xsl:variable>

    <redirect:write file="{$output.dir}/{$package.dir}/{$name}.html">
      <html>
        <xsl:call-template name="html-head">
          <xsl:with-param name="prefix-url">
            <xsl:call-template name="back.url">
              <xsl:with-param name="name" select="@package"/>
            </xsl:call-template>
          </xsl:with-param>
        </xsl:call-template>
        <body>

          <xsl:call-template name="html-heading-class">
            <xsl:with-param name="prefix-url">
              <xsl:call-template name="back.url">
                <xsl:with-param name="name" select="@package"/>
              </xsl:call-template>
            </xsl:with-param>
            <xsl:with-param name="name" select="'Duplication'"/>
            <xsl:with-param name="class" select="@class"/>
            <xsl:with-param name="package" select="@package"/>
          </xsl:call-template>

          <xsl:variable name="all-files" select="$duplications/file[@class!=$name or @package!=$package]"/>
          <b>
            <xsl:value-of select="concat(count($all-files),' duplications')"/>
          </b>

          <br/>

          <xsl:call-template name="duplications.summary">
            <xsl:with-param name="duplications" select="$duplications"/>
            <xsl:with-param name="package" select="$package"/>
            <xsl:with-param name="class" select="$name"/>
          </xsl:call-template>

          <hr/><br/><br/>

          <xsl:call-template name="duplications.details">
            <xsl:with-param name="duplications" select="$duplications"/>
            <xsl:with-param name="package" select="$package"/>
            <xsl:with-param name="class" select="$name"/>
          </xsl:call-template>
        </body>
      </html>
    </redirect:write>

  </xsl:template>

  <xsl:template name="duplications.summary">
    <xsl:param name="duplications"/>
    <xsl:param name="class"/>
    <xsl:param name="package"/>

    <xsl:variable name="prefix-url">
      <xsl:call-template name="back.url">
        <xsl:with-param name="name" select="@package"/>
      </xsl:call-template>
    </xsl:variable>

    <table>
      <tr>
        <th>Lines</th><th>Class</th>
      </tr>

      <xsl:for-each select="$duplications">
        <xsl:variable name="dup" select="."/>
        <xsl:for-each select="file[@class!=$class or @package!=$package]">
          <tr>
            <td>
              <xsl:value-of select="$dup/@length"/>
            </td>
            <td>
              <xsl:variable name="relative.url">
                <xsl:call-template name="back.url">
                  <xsl:with-param name="name" select="@package"/>
                </xsl:call-template>
                <xsl:call-template name="package.dir">
                  <xsl:with-param name="name" select="@package"/>
                </xsl:call-template>
              </xsl:variable>
              <a href="#{generate-id($dup)}">
                <xsl:value-of select="concat(@package,'.',@class)"/>
              </a>
              <xsl:value-of select="' at line '"/>
              <a>

                <xsl:attribute name="href">
                  <xsl:call-template name="src.link">
                    <xsl:with-param name="prefix-url" select="$prefix-url"/>
                    <xsl:with-param name="package"    select="@package"/>
                    <xsl:with-param name="class"      select="@class"/>
                    <xsl:with-param name="line"       select="@line"/>
                  </xsl:call-template>

                </xsl:attribute>
                <xsl:value-of select="@line"/>
              </a>


            </td>
          </tr>
        </xsl:for-each>
      </xsl:for-each>
    </table>
  </xsl:template>

  <xsl:template name="duplications.details">
    <xsl:param name="duplications"/>
    <xsl:param name="class"/>
    <xsl:param name="package"/>

    <xsl:variable name="prefix-url">
      <xsl:call-template name="back.url">
        <xsl:with-param name="name" select="@package"/>
      </xsl:call-template>
    </xsl:variable>
    <table>
      <xsl:for-each select="$duplications">
        <xsl:variable name="selected-files" select="file[@class!=$class or @package!=$package]"/>

        <tr>
          <a name="{generate-id(.)}"> </a>
          <th>
            <xsl:value-of select="concat('Duplication of ',@length, ' lines of code')"/>
          </th>
        </tr>
        <tr>
          <td>
            <table>
              <tr>

                <td valign="top" nowrap="true">Duplicated in
                  <ul>
                    <xsl:for-each select="$selected-files">

                      <li>
                        <xsl:variable name="relative.url">
                          <xsl:call-template name="back.url">
                            <xsl:with-param name="name" select="@package"/>
                          </xsl:call-template>
                          <xsl:call-template name="package.dir">
                            <xsl:with-param name="name" select="@package"/>
                          </xsl:call-template>
                        </xsl:variable>
                        <a href="{$relative.url}/{@class}.html">
                          <xsl:value-of select="translate(concat(@package, '.', @class), ' ','')"/>
                        </a>
                        <xsl:value-of select="' at line '"/>
                        <a>
                          <xsl:attribute name="href">
                            <xsl:call-template name="src.link">
                              <xsl:with-param name="prefix-url" select="$prefix-url"/>
                              <xsl:with-param name="package"    select="@package"/>
                              <xsl:with-param name="class"      select="@class"/>
                              <xsl:with-param name="line"       select="@line"/>
                            </xsl:call-template>
                          </xsl:attribute>
                        </a>
                        <xsl:value-of select="@line"/>
                      </li>
                    </xsl:for-each>
                  </ul>
                </td>
              </tr>
              <tr>
                <td>
                  <pre class="code">
                    <xsl:value-of select="codefragment"/>
                  </pre>
                </td>
              </tr>
            </table>
          </td>
        </tr>
      </xsl:for-each>
    </table>

  </xsl:template>


  <xsl:template name="package.dir">
    <xsl:param name="name"/>
    <xsl:if test="not($name = '')">
      <xsl:value-of select="translate($name,'.','/')"/>
    </xsl:if>
    <xsl:if test="$name = ''">.</xsl:if>
  </xsl:template>

</xsl:stylesheet>

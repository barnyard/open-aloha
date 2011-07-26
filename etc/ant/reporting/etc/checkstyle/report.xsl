<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:redirect="http://xml.apache.org/xalan/redirect"
  extension-element-prefixes="redirect"
  version="1.0">

  <xsl:output method="html" indent="yes" encoding="US-ASCII"/>
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>
  <xsl:param name="source-root"/>
  <xsl:variable name="src" select="translate($source-root, '\:', '/_')"/>

  <xsl:key name="kPackage" match="file" use="@package"/>
  <xsl:key name="kClass" match="file" use="@class"/>

  <xsl:include href="../build-templates.xsl"/>
  <xsl:include href="../html-templates.xsl"/>

  <xsl:param name="output.dir" select="'reports/checkstyle'"/>

  <xsl:template match="checkstyle">
    <!-- create the index.html -->
    <redirect:write file="{$output.dir}/index.html">
      <xsl:call-template name="index.html"/>
    </redirect:write>

    <redirect:write file="{$output.dir}/overview-frame.html">
      <xsl:apply-templates select="." mode="overview"/>
    </redirect:write>

    <!-- create the all-classes.html at the root -->
    <redirect:write file="{$output.dir}/allclasses-frame.html">
      <xsl:apply-templates select="." mode="all.classes"/>
    </redirect:write>

    <redirect:write file="{$output.dir}/index.html">
      <xsl:call-template name="index.html"/>
    </redirect:write>

    <xsl:for-each select="file[generate-id()=generate-id(key('kPackage', @package)[1])]">

      <xsl:call-template name="package-frame.html">
        <xsl:with-param name="name" select="@package"/>
        <xsl:with-param name="files" select="key('kPackage', @package)"/>
      </xsl:call-template>

      <xsl:call-template name="package-errors-frame.html">
        <xsl:with-param name="name" select="@package"/>
        <xsl:with-param name="files" select="key('kPackage', @package)"/>
      </xsl:call-template>
    </xsl:for-each>


    <!-- create the overview-packages.html at the root -->
    <redirect:write file="{$output.dir}/overview-summary.html">
      <xsl:apply-templates select="." mode="overview-summary.html"/>
    </redirect:write>

    <!-- process all files -->
    <xsl:apply-templates select="file"/>
  </xsl:template>

  <xsl:template name="index.html">
    <html>
      <head>
        <title>Checkstyle Results</title>
      </head>
      <frameset cols="20%,80%">
        <frameset rows="30%,70%">
          <frame name="packageFrame" src="overview-frame.html"/>
          <frame name="classListFrame" src="allclasses-frame.html"/>
        </frameset>
        <frame name="classFrame" src="overview-summary.html"/>
        <noframes>
          <h2>Frame Alert</h2>
          <p>
                This document is designed to be viewed using the frames feature. If you see this message, you are using a non-frame-capable web client.
          </p>
        </noframes>
      </frameset>
    </html>
  </xsl:template>

  <xsl:template name="package-frame.html">
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
        <h2>Classes</h2>

        <a href="package-errors-frame.html"><xsl:value-of select="'Errors'"/></a>
        <xsl:value-of select="'|Name'"/>
        <hr/>
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

  <xsl:template name="package-errors-frame.html">
    <xsl:param name="name"/>
    <xsl:param name="files"/>
    <xsl:variable name="package.dir">
      <xsl:call-template name="package.dir">
        <xsl:with-param name="name" select="@package"/>
      </xsl:call-template>
    </xsl:variable>

    <redirect:write file="{$output.dir}/{$package.dir}/package-errors-frame.html">
      <html>
        <xsl:call-template name="html-head">
          <xsl:with-param name="prefix-url">
            <xsl:call-template name="back.url">
              <xsl:with-param name="name" select="@package"/>
            </xsl:call-template>
          </xsl:with-param>
        </xsl:call-template>
        <h2>Classes</h2>

        <xsl:value-of select="'Errors|'"/>
        <a href="package-frame.html"><xsl:value-of select="'Name'"/></a>
        <hr/>
        <font size="+1" CLASS="FrameTitleFont">
          <xsl:value-of select="$name"/>
        </font>
        <table>
          <xsl:for-each
            select="$files">
            <xsl:sort select="count(child::*)" order="descending"/>
            <xsl:variable name="first">
              <xsl:call-template name="isfirst">
                <xsl:with-param name="name" select="@name"/>
              </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="name" select="@name"/>
            <xsl:choose >
              <xsl:when test="$first = 'true'">
                <tr>
                  <td>
                    <a href="{@class}.html" target="classFrame">
                      <xsl:value-of select="@class"/>
                    </a>
                  </td>
                  <td><xsl:value-of select="concat('{', count(child::*), '}')"/></td>
                </tr>
              </xsl:when>
              <xsl:otherwise>

              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </table>
      </html>
    </redirect:write>
  </xsl:template>



  <xsl:template match="checkstyle" mode="overview">
    <html>
      <xsl:call-template name="html-head"/>


      <h2><a target="classFrame" href="overview-summary.html">Home</a></h2>
      <h2>Packages</h2>


      <body>
        <table>
          <xsl:for-each
            select="file[generate-id()=generate-id(key('kPackage', @package)[1])]">
            <xsl:sort select="@package" order="ascending"/>
            <xsl:variable name="package.dir">
              <xsl:call-template name="package.dir">
                <xsl:with-param name="name" select="@package"/>
              </xsl:call-template>
            </xsl:variable>
            <tr>
              <td nowrap="nowrap">
                <a href="{$package.dir}/package-errors-frame.html" target="classListFrame">
                  <xsl:value-of select="@package"/>
                </a>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </body>
    </html>
  </xsl:template>



  <!--
  Replace DOS characters in a path.
  Replace '\' with '/', ':' with '_'.
  -->

    
  <!--
  Creates an all-classes.html file that contains a link to all files.
  -->
  <xsl:template match="checkstyle" mode="all.classes">
    <html>
      <xsl:call-template name="html-head"/>

      <body>
        <h2>Files</h2>
        <p>
          <table width="100%">
            <xsl:apply-templates select="file" mode="all.classes">
              <xsl:sort select="@class"/>
            </xsl:apply-templates>
          </table>
        </p>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="checkstyle" mode="filelist">
    <h3>Files</h3>
    <table class="log" border="0" cellpadding="5" cellspacing="2" width="100%">
      <tr>
        <th>Name</th>
        <th>Errors</th>
      </tr>
      <xsl:apply-templates select="file" mode="filelist">
        <xsl:sort select="@name"/>
      </xsl:apply-templates>
    </table>
  </xsl:template>

  <xsl:template match="file" mode="filelist">
    <xsl:variable name="first">
      <xsl:call-template name="isfirst">
        <xsl:with-param name="name" select="@name"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="name" select="@name"/>

    <xsl:if test="$first = 'true'">
      <xsl:variable name="new-name">
        <xsl:call-template name="output-file">
          <xsl:with-param name="class" select="@class"/>
          <xsl:with-param name="package" select="@package"/>
        </xsl:call-template>
      </xsl:variable>
      <tr>
        <xsl:call-template name="alternated-row"/>
        <td nowrap="nowrap">
          <a>
            <xsl:attribute name="href">
              <xsl:value-of select="$new-name"/>
            </xsl:attribute>
            <xsl:value-of select="$new-name"/>
          </a>
        </td>
        <td>
          <xsl:value-of select="count(../file[@name = $name]/error)"/>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="file" mode="all.classes">
    <xsl:variable name="first">
      <xsl:call-template name="isfirst">
        <xsl:with-param name="name" select="@name"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:if test="$first = 'true'">
      <xsl:variable name="new-name">
        <xsl:call-template name="output-file">
          <xsl:with-param name="class" select="@class"/>
          <xsl:with-param name="package" select="@package"/>
        </xsl:call-template>
      </xsl:variable>
      <tr>
        <td nowrap="nowrap">
          <a target="classFrame">
            <xsl:attribute name="href">
              <xsl:value-of select="$new-name"/>
            </xsl:attribute>
            <xsl:value-of select="@class"/>
          </a>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <!--
  transform string like a/b/c to ../../../
  @param path the path to transform into a descending directory path
  -->
  <xsl:template name="path">
    <xsl:param name="path"/>
    <xsl:if test="contains($path,'/')">
      <xsl:text>../</xsl:text>
      <xsl:call-template name="path">
        <xsl:with-param name="path">
          <xsl:value-of select="substring-after($path,'/')"/>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:if>
    <xsl:if test="not(contains($path,'/')) and not($path = '')">
      <xsl:text>../</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="file">
    <xsl:variable name="first">
      <xsl:call-template name="isfirst">
        <xsl:with-param name="name" select="@name"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="name" select="@name"/>

    <xsl:if test="$first = 'true'">
      <xsl:variable name="new-name">
        <xsl:call-template name="output-file">
          <xsl:with-param name="class" select="@class"/>
          <xsl:with-param name="package" select="@package"/>
        </xsl:call-template>
      </xsl:variable>

      <redirect:write file="{$output.dir}/{$new-name}">
        <html>
          <xsl:variable name="prefix-url">
            <xsl:if test="not(@package = '')"><xsl:call-template name="back.url"><xsl:with-param name="name" select="concat(@package, '/')"/></xsl:call-template></xsl:if>
          </xsl:variable>
          <xsl:call-template name="html-head">
            <xsl:with-param name="prefix-url" select="$prefix-url" />
          </xsl:call-template>
          <body>

            <xsl:call-template name="html-heading-class">
              <xsl:with-param name="name" select="'Checkstyle'"/>
              <xsl:with-param name="prefix-url" select="$prefix-url" />
              <xsl:with-param name="package" select="@package" />
              <xsl:with-param name="class" select="@class" />
            </xsl:call-template>

            <table class="log" border="0" cellpadding="5" cellspacing="2" width="100%">
              <tr>
                <th>Error Description</th>
                <th>Line</th>
              </tr>
              <xsl:for-each select="../file[@name = $name]/error">
                <tr>
                  <xsl:call-template name="alternated-row"/>
                  <td>
                    <xsl:value-of select="@message"/>
                  </td>
                  <td>
                    <a>
                      <xsl:attribute name="href">
                        <xsl:value-of select="concat($prefix-url, '../java2html/', translate(../@package, '.', '/'), '/', ../@class, '.html#', @line)"/> 
                      </xsl:attribute>
                    <xsl:value-of select="@line"/>
                    </a>
                  </td>
                </tr>
              </xsl:for-each>
            </table>
          </body>
        </html>
      </redirect:write>
    </xsl:if>
  </xsl:template>

  <xsl:template match="checkstyle" mode="overview-summary.html">
    <html>
      <xsl:call-template name="html-head"/>
      <body>
        <xsl:call-template name="html-heading">
          <xsl:with-param name="name" select="'Checkstyle Summary'"/>
        </xsl:call-template>

    <xsl:variable name="fileCount" select="count(file)"/>
    <xsl:variable name="errorCount" select="count(file/error)"/>
    <table class="log" border="0" cellpadding="5" cellspacing="2" width="100%">
      <tr>
        <th>Files</th>
        <th>Errors</th>
      </tr>
      <tr>
        <xsl:call-template name="alternated-row"/>
        <td>
          <xsl:value-of select="$fileCount"/>
        </td>
        <td>
          <xsl:value-of select="$errorCount"/>
        </td>
      </tr>
    </table>

    </body>
    </html>
  </xsl:template>

       <!-- determine if this is the first occurance of the given name in the input -->
  <xsl:template name="isfirst">
    <xsl:param name="name"/>
    <xsl:value-of select="count(preceding-sibling::file[@name=$name]) = 0"/>
  </xsl:template>


  <xsl:template name="package.dir">
    <xsl:param name="name"/>
    <xsl:if test="not($name = '')">
      <xsl:value-of select="translate($name,'.','/')"/>
    </xsl:if>
    <xsl:if test="$name = ''">.</xsl:if>
  </xsl:template>

</xsl:stylesheet>


<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:lxslt=        "http://xml.apache.org/xslt"
  xmlns:redirect=     "org.apache.xalan.xslt.extensions.Redirect"
  extension-element-prefixes="redirect lxslt xsl">

  <xsl:template name="html-head">
    <xsl:param name="prefix-url" select="''"/>
    <head>
      <link rel="stylesheet" type="text/css" href="../{$prefix-url}/style/stylesheet.css" title="Style"/>
    </head>
  </xsl:template>

  <xsl:template name="html-project-img">
    <xsl:param name="prefix-url" select="''"/>
    <head>
      <a target="_top">
        <xsl:attribute name="href">
          <xsl:value-of select="concat('../', $prefix-url, '/index.html')"/>
        </xsl:attribute>
        <img src="../{$prefix-url}/style/project-logo.png" border="0"/>
      </a>
    </head>
  </xsl:template>

  <xsl:template name="html-heading">
    <xsl:param name="prefix-url" select="''"/>
    <xsl:param name="name"/>
    <xsl:param name="subname" select="''"/>
    <table width="100%">
      <tr>
        <td>
          <h1>
            <xsl:value-of select="$name"/>
          </h1>
          <h3>
            <xsl:value-of select="$subname"/>
          </h3>
        </td>
        <td align="right">
          <xsl:call-template name="html-project-img">
            <xsl:with-param name="prefix-url" select="$prefix-url"/>
          </xsl:call-template>
        </td>
      </tr>
    </table>
    <hr size="1"/>
  </xsl:template>

  <xsl:template name="alternated-row">
     <xsl:attribute name="class">
       <xsl:if test="position() mod 2 = 1">a</xsl:if>
       <xsl:if test="position() mod 2 = 0">b</xsl:if>
     </xsl:attribute>
   </xsl:template>

   <xsl:template name="src.link">
     <xsl:param name="prefix-url"/>
     <xsl:param name="package"/>
     <xsl:param name="class"/>
     <xsl:param name="line" select="''"/>
     <xsl:value-of select="concat($prefix-url, '../java2html/', translate($package, '.', '/'), '/', $class, '.html#', $line)"/>

   </xsl:template>

   <xsl:template name="html-heading-class">
     <xsl:param name="prefix-url"/>
     <xsl:param name="package"/>
     <xsl:param name="class"/>
     <xsl:param name="name"/>
     <xsl:call-template name="html-heading">
       <xsl:with-param name="prefix-url" select="$prefix-url"/>
       <xsl:with-param name="name" select="$name"/>
     </xsl:call-template>
     <h2>
       <font size="-1">
         <xsl:value-of select="$package"/>
       </font>
       <br/>
       <a>
         <xsl:if test="false=contains($class, '.')">
           <xsl:attribute name="href">
             <xsl:call-template name="src.link">
               <xsl:with-param name="class" select="$class"/>
               <xsl:with-param name="prefix-url" select="$prefix-url"/>
               <xsl:with-param name="package" select="$package"/>
             </xsl:call-template>
           </xsl:attribute>
         </xsl:if>
         <xsl:value-of select="$class"/>
       </a>
     </h2>
   </xsl:template>

  <!--
    eg.
      name=<init>
      name=getName()

      signature=(Ljava/lang/String;Ljava/lang/String;)V
      signature=(Ljava/lang/String;)Ljava/lang/String;
  -->
  <xsl:template name="pretty-method-signature">
    <xsl:param name="method"/>
    <xsl:param name="signature"/>
    <xsl:variable name="return">
      <xsl:call-template name="pretty-java-type">
        <xsl:with-param name="signature" select="substring-after($signature, ')')"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="params">
      <xsl:call-template name="pretty-java-type">
        <xsl:with-param name="signature" select="substring-after(substring-before($signature, ')'), '(')"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:value-of select="concat($return, ' ', $method, ' (', $params, ')')" disable-output-escaping="yes"/>

  </xsl:template>

  <!--
    examples IN       OUT

    V                 void
    Z                 boolean
    B                 byte
    C                 char
    S                 short
    I                 int
    J                 long
    F                 float
    D                 double
    L#class#;         class
    [type             type[]
  -->
  <xsl:template name="pretty-java-type">
    <xsl:param name="signature" select="''"/>
    <xsl:param name="done" select="''"/>

    <xsl:variable name="return" select="substring-after($signature, ')')"/>

    <xsl:choose>
      <xsl:when test="string-length($signature) = 0">
        <xsl:value-of select="translate($done, '/', '.')"/>
      </xsl:when>

      <xsl:otherwise>
        <xsl:variable name="current">
          <xsl:if test="string-length($done) &gt; 0">
            <xsl:value-of select="concat($done, ', ')"/>
          </xsl:if>
        </xsl:variable>


        <xsl:if test="starts-with($signature, 'V')">
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="substring($signature, 2, string-length($signature) -1)"/>
            <xsl:with-param name="done" select="concat($current,'void')"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:if test="starts-with($signature, 'Z')">
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="substring($signature, 2, string-length($signature) -1)"/>
            <xsl:with-param name="done" select="concat($current,'boolean')"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:if test="starts-with($signature, 'B')">
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="substring($signature, 2, string-length($signature) -1)"/>
            <xsl:with-param name="done" select="concat($current,'byte')"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:if test="starts-with($signature, 'C')">
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="substring($signature, 2, string-length($signature) -1)"/>
            <xsl:with-param name="done" select="concat($current,'char')"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:if test="starts-with($signature, 'S')">
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="substring($signature, 2, string-length($signature) -1)"/>
            <xsl:with-param name="done" select="concat($current,'short')"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:if test="starts-with($signature, 'I')">
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="substring($signature, 2, string-length($signature) -1)"/>
            <xsl:with-param name="done" select="concat($current,'int')"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:if test="starts-with($signature, 'J')">
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="substring($signature, 2, string-length($signature) -1)"/>
            <xsl:with-param name="done" select="concat($current,'long')"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:if test="starts-with($signature, 'F')">
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="substring($signature, 2, string-length($signature) -1)"/>
            <xsl:with-param name="done" select="concat($current,'float')"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:if test="starts-with($signature, 'D')">
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="substring($signature, 2, string-length($signature) -1)"/>
            <xsl:with-param name="done" select="concat($current,'double')"/>
          </xsl:call-template>
        </xsl:if>

        <!--  string is L###;???
          extract ### and call again with ???-->
        <xsl:if test="starts-with($signature, 'L')">
          <xsl:variable name="before" select="substring-before($signature, ';')"/>
          <xsl:variable name="after" select="substring-after($signature, ';')"/>
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="$after"/>
            <xsl:with-param name="done" select="concat($current,substring-after($before, 'L'))"/>
          </xsl:call-template>
        </xsl:if>

        <!--  string is []###;???
          extract ### [] and call again with ???-->
        <xsl:if test="starts-with($signature, '[]')">
          <xsl:variable name="before" select="substring-before($signature, ';')"/>
          <xsl:variable name="after" select="substring-after($signature, ';')"/>
          <xsl:call-template name="pretty-java-type">
            <xsl:with-param name="signature" select="$after"/>
            <xsl:with-param name="done" select="concat($current,substring-after($before, '[]'), '[]')"/>
          </xsl:call-template>
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
  <HTML>
   <HEAD>
   <TITLE></TITLE>

   <style type="text/css">
	.PtHeading{font-family:"Book Antiqua";font-size:18.0pt;font-weight:"bold";color:"#6495ED";}
	.PtData{font-family:"Georgia";font-size:9.0pt;font-weight:"bold";color:"#9c9c9c";}
	.PageHeader{font-family:"Georgia";font-size:10.0pt;font-weight:"bold";color:"#6495ED";}
	.leftPaneHeading{font-family:"Arial Black";font-size:10.0pt;color:"#6495ED";}
	.leftPaneData{font-family:"Georgia";font-size:9.0pt;font-weight:"normal";color:"#000000";}
	.rightPaneHeading{font-family:"Arial Black";font-size:10.0pt;color:"#6495ED";}
	.rightPaneData{font-family:"Georgia";font-size:9.0pt;font-weight:"normal";color:"#000000";}
	.tfoot{display:table-footer-group;}
	.TableFooter{font-family:"Georgia";font-size:9.0pt;font-weight:"bold";color:"#9c9c9c";}
	.eclinicalworks{font-family:"Arial";font-style:italic;font-size:8.0pt;color:"#b9d0e8";}
	.PNSig{font-family:"Georgia";font-size:10.0pt;font-weight:"bold";color:"#6495ED";}
	.Facility{font-family:"Georgia";font-size:9.0pt;font-weight:"bold";color:"#6495ED";}
	pre.preclass{font-family:"Georgia";font-size:9.0pt;font-weight:"normal";color:"#000000";word-wrap: break-word;white-space: normal;}

  </style>

   </HEAD>

  <BODY>
    <hr/>
    <table cellpadding="0" cellspacing="0" border="0" width="100%">
      <tbody>
	<tr><td>

    <table cellspacing="0" cellpadding="0" width="100%">
      <tr class="PtHeading">
	<th rowspan="6"><img id="logo" alt="" height="100" width="300" src="https://static.apixio.com/images/clients/smchs/logo/smmcLogo.jpg"></img></th>
        <td align="right"><b><xsl:value-of select="//return/patient"/></b></td>
      </tr>

      <tr class="PtData">
        <td align="right">
		<xsl:value-of select="//return/age"/>&#160; old &#160;<xsl:value-of select="//return/sex"/>,&#160;DOB: <xsl:value-of select="//return/DOB"/>
        </td>
      </tr>

      <tr class="PtData">
        <td align="right">
		<xsl:value-of select="//return/address"/>
        </td>
      </tr>

      <tr class="PtData">
      <xsl:if test="//return/phone!=''">
        <td align="right">
          Home: <xsl:value-of select="//return/phone"/>&#160;&#160;&#160;
        </td>
      </xsl:if>
      </tr>

      <tr class="PtData">
        <td align="right">
          <xsl:if test="//return/doctor!=''">
		Provider:&#160;<xsl:value-of select="//return/doctor"/>&#160;&#160;&#160;
	  </xsl:if>
        </td>
      </tr>
    </table>

    <hr/>

    <table cellspacing="0" cellpadding="0" width="100%">

      <tr width="100%">
         <td ALIGN="LEFT" class="PageHeader">
           <xsl:if test="//return/enctype!=''">
		<xsl:value-of select="//return/enctype"/>
	 </xsl:if>
         </td>
      </tr>

	<tr height="20"><td></td></tr>

      <TR ALIGN="LEFT" >
	<TD WIDTH="17%" VALIGN="TOP" ALIGN="LEFT" class="rightPaneHeading">Answered by&#160;&#160;</TD>
        <TD WIDTH="53%" ALIGN="LEFT" class="rightPaneData"><xsl:value-of select="//return/user"/></TD>
        <TD WIDTH="30%" ALIGN="RIGHT" class="rightPaneData">Date: <xsl:value-of select="//return/date"/></TD>
      </TR>
      <TR width="100%">
				<TD>&#160;</TD>
        <TD>&#160;</TD>
        <TD  ALIGN="RIGHT" class="rightPaneData">Time: <xsl:value-of select="//return/time"/>&#160;&#160;&#160;</TD>
      </TR>

			<tr height="20"><td></td></tr>

    </table>

<TABLE WIDTH="100%" BORDER="0">

	<xsl:if test="//return/caller !=''">
	    <TR>
	    	<TD  WIDTH="17%" ALIGN="LEFT" class="rightPaneHeading">Caller&#160;</TD>
	    	<TD  WIDTH="83%" ALIGN="LEFT" class="rightPaneData"><xsl:value-of select="//return/caller"/></TD>
	    </TR>
            <TR>
            	<TD><br/></TD>
            </TR>
    	</xsl:if>


	<xsl:if test="//return/reason !=''">
	    <TR>
	    	<TD WIDTH="17%" VALIGN="TOP" ALIGN="LEFT" class="rightPaneHeading">Reason&#160;</TD>
	    	<TD WIDTH="83%" ALIGN="LEFT" class="rightPaneData"><xsl:value-of select="//return/reason"/></TD>
	    </TR>
            <TR>
            	<TD><br/></TD>
            </TR>
    	</xsl:if>

	<xsl:if test="//return/message !=''">
	    <TR>
	    	<TD WIDTH="17%" VALIGN="TOP" ALIGN="LEFT" class="rightPaneHeading">Message&#160;</TD>
	    	<TD WIDTH="83%" ALIGN="LEFT" class="rightPaneData"><pre class="preclass"><xsl:value-of select="//return/message"/></pre></TD>
	    </TR>
            <TR>
            	<TD><br/></TD>
            </TR>
    	</xsl:if>


	<xsl:if test="//return/actiontaken !=''">
	    <TR>
	    	<TD WIDTH="17%" VALIGN="TOP" ALIGN="LEFT" class="rightPaneHeading">Action Taken&#160;</TD>
	    	<TD WIDTH="83%" ALIGN="LEFT" class="rightPaneData"><xsl:value-of select="//return/actiontaken"/></TD>
	    </TR>
            <TR>
            	<TD><br/></TD>
            </TR>
    	</xsl:if>


	<xsl:if test="//return/notes !=''">
	    <TR>
	    	<TD WIDTH="17%" VALIGN="TOP" ALIGN="LEFT" class="rightPaneHeading">Notes&#160;</TD>
	    	<TD WIDTH="83%" ALIGN ="LEFT" class="rightPaneData"><pre class="preclass"><xsl:value-of select="//return/notes"/></pre></TD>
	    </TR>
            <TR>
            	<TD><br/></TD>
            </TR>
    	</xsl:if>

	</TABLE>

	<xsl:for-each select="//subItems">
            <xsl:for-each select="item">
               <TABLE WIDTH="100%" border="0">
                  <xsl:for-each select="itemValue">
                     <TR>
                        <xsl:if test="(position()=1)">
                           <TD WIDTH="17%" ALIGN="LEFT" class="rightPaneHeading"><xsl:value-of select="../itemName"/></TD>
                        </xsl:if>

                        <xsl:if test="not(position()=1)">
                           <TD WIDTH="17%" ALIGN="LEFT" class="rightPaneData" VALIGN="TOP">&#160;</TD>
                        </xsl:if>

                        <TD WIDTH="83%" ALIGN="LEFT" class="rightPaneData"><xsl:value-of select="."/></TD>
                     </TR>
                  </xsl:for-each>
               </TABLE><br/>
            </xsl:for-each>
         </xsl:for-each>

	 		 <xsl:for-each select="//eMsgs">
		      <HR/><br/>
               <TABLE WIDTH="100%" border="0">
                  <xsl:for-each select="eMsg">
				  <xsl:if test="(position()=1)">
                          <TD  colspan="2" VALIGN="TOP" ALIGN="CENTER" ><FONT COLOR="#333333" SIZE="4"><b>eMessages</b></FONT></TD>
                   </xsl:if>

						<TR>
							<TD  VALIGN="TOP" ALIGN="LEFT" class="rightPaneHeading">From:&#160;&#160;</TD>
							<TD  ALIGN="LEFT" class="rightPaneData"><xsl:value-of select="eFrom"/></TD>
					    </TR>
						<TR>
						   <TD  VALIGN="TOP" ALIGN="LEFT" class="rightPaneHeading">Created:&#160;&#160;</TD>
							<TD  ALIGN="LEFT" class="rightPaneData" > <xsl:value-of select="eDtCreated"/></TD>
						</TR>
						<TR>
						   <TD  VALIGN="TOP" ALIGN="LEFT" class="rightPaneHeading">Sent:&#160;&#160;</TD>
							<TD  ALIGN="LEFT" class="rightPaneData" > <xsl:value-of select="eDtSent"/></TD>
						</TR>


				<xsl:if test="eSubject !=''">
					<TR>
						<TD WIDTH="17%" VALIGN="TOP" ALIGN="LEFT" class="rightPaneHeading">Subject:&#160;</TD>
						<TD WIDTH="83%" ALIGN="LEFT" class="rightPaneData" ><xsl:value-of select="eSubject"/></TD>
					</TR>
					</xsl:if>

				<xsl:if test="eMessage !=''">
					<TR>
						<TD WIDTH="17%" VALIGN="TOP" ALIGN="LEFT" class="rightPaneHeading">Message:&#160;</TD>
						<TD WIDTH="83%" ALIGN="LEFT" class="rightPaneData" ><xsl:value-of select="eMessage" disable-output-escaping="yes"/></TD>
					</TR>
						<TR>
							<TD><br/></TD>
						</TR>
					</xsl:if>
					<xsl:if test="(position()!=last())">
						<TR>
							<TD  colspan="2" VALIGN="TOP" ALIGN="CENTER"><HR width="80%"/><br/></TD>
						</TR>
					</xsl:if>
                  </xsl:for-each>
               </TABLE><br/>
         </xsl:for-each>

		<xsl:if test = "//return/VirtualFlag = 'yes'">
			<HR/>
		 <table cellspacing="0" cellpadding="0" width="100%">
					 <tr height="20"><td></td></tr>
			</table>
		</xsl:if>

   <table cellspacing="0" cellpadding="0" width="100%">
     <tbody>
     <tr>
     <td width="100%">
     <table cellspacing="8" cellpadding="0" border="0" align="left" height="100%" width="100%">

       <xsl:if test="//subItems/cc!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/cc"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/hpi!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/hpi"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/CurrentMeds!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/CurrentMeds"/>
         </td>
	</tr>
	</xsl:if>

	<xsl:if test="//subItems/PastHistory!=''">
	<tr>
	 <td valign="top">
	   <xsl:apply-templates select="//subItems/PastHistory"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/SurgicalHistory!=''">
	<tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/SurgicalHistory"/>
         </td>
	</tr>
       </xsl:if>

       <xsl:if test="//subItems/FamilyHistory!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/FamilyHistory"/>
         </td>
	</tr>
	</xsl:if>

       <xsl:if test="//subItems/SocialHistory!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/SocialHistory"/>
         </td>
	</tr>
	</xsl:if>

	<xsl:if test="//subItems/gynHistory!=''">
	<tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/gynHistory"/>
         </td>
	</tr>
	</xsl:if>

	<xsl:if test="//subItems/obHistory!=''">
	<tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/obHistory"/>
         </td>
	</tr>
	</xsl:if>

	<xsl:if test="//subItems/allergies!=''">
	<tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/allergies"/>
         </td>
	</tr>
	</xsl:if>

	<xsl:if test="//subItems/Hospitalization!=''">
	<tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/Hospitalization"/>
         </td>
	</tr>
	</xsl:if>

	<xsl:if test="//subItems/ros!=''">
	<tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/ros"/>
         </td>
	</tr>
	</xsl:if>

	 <xsl:if test="//subItems/vitals!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/vitals"/>
         </td>
       </tr>
       </xsl:if>

	 <xsl:if test="//subItems/vitals2!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/vitals2"/>
         </td>
       </tr>
       </xsl:if>
       
        <xsl:if test="//subItems/vitals2BR!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/vitals2BR"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/examination!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/examination"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/PhysicalExamination!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/PhysicalExamination"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/Therapeutic!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/Therapeutic"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/TherapyAssessment!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/TherapyAssessment"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/assessment!='' or //subItems/notesBR!=''">
				 <tr>
					 <td valign="top">
							<xsl:apply-templates select="//subItems/assessment"/>
					 </td>
				</tr>
				<xsl:if test="//subItems/notesBR!=''">
					<tr>
						 <td valign="top">
								<xsl:if test="count(//subItems/assessment) = 0">
									<table cellspacing="0" cellpadding="0" width="100%">
										<tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Assessment"/></td></tr>
									</table>
								</xsl:if>
								 <xsl:apply-templates select="//subItems/notesBR"/>
						 </td>
					</tr>
				</xsl:if>
				<xsl:if test="//subItems/axis4!=''">
					<tr>
						 <td valign="top">
									 <xsl:apply-templates select="//subItems/axis4"/>
						 </td>
					</tr>
				</xsl:if>
				<xsl:if test="//subItems/axis5!=''">
					<tr>
						 <td valign="top">
									 <xsl:apply-templates select="//subItems/axis5"/>
						 </td>
					</tr>
				</xsl:if>
      </xsl:if>

       <xsl:if test="//subItems/treatment!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/treatment"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/Procedure!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/Procedure"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/Immunization!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/Immunization"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/xrays!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/xrays"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/labs!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/labs"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/preventive!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/preventive"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/procedures!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/procedures"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/FollowUp!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/FollowUp"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//return/addendums!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//return/addendums"/>
         </td>
       </tr>
       </xsl:if>

     </table>
     </td>
     </tr>
     </tbody>
   </table>

   </td></tr>
   </tbody>
   <tfoot style="display:table-footer-group">
            <tr>
              <td>
                <br/><hr/>
                <table cellspacing="0" cellpadding="0" border="0" width="100%">
                  <tr class="TableFooter">
		    <br/><td align="center">Patient:&#160;<xsl:value-of select="//return/patient"/>&#160;&#160;&#160;&#160;DOB:&#160;<xsl:value-of select="//return/DOB"/>&#160;&#160;&#160;&#160;Provider:&#160;<xsl:value-of select="//return/doctor"/>&#160;&#160;&#160;&#160;<xsl:value-of select="//return/date"/></td>
                  </tr>
		  <tr height="10">
			<td/>
		  </tr>
		  <tr class="eclinicalworks">
		    <td align="center">Note generated by eClinicalWorks EMR/PM Software (www.eClinicalWorks.com)</td>
                  </tr>
                </table>
              </td>
            </tr>
    </tfoot>
   </table>

  </BODY>
  </HTML>
</xsl:template>

<xsl:template match="addendums">
  <p>&#160;</p>
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="rightPaneHeading"><td COLSPAN="2">Addendum:</td></tr>
    <tr class="rightPaneData"><td>
	 <xsl:apply-templates select="addendum"/>
    </td></tr>
    <tr height="5"><td></td></tr>
  </table>
</xsl:template>

<xsl:template match="addendum">
	<tr class="rightPaneData"><td width="10"></td><td><xsl:value-of select="date"/>&#160;<xsl:value-of select="time"/>&#160;<xsl:value-of select="userName"/>&#160;&#62;&#160;<xsl:value-of select="notes"/></td></tr>
</xsl:template>

<xsl:template match="notes">
  <table cellspacing="0" cellpadding="0" width="100%" border="0">
	<tr class="rightPaneData"><td><xsl:value-of select="."/></td></tr>
  </table>
</xsl:template>

<xsl:template match="notesBR">
  <table cellspacing="0" cellpadding="0" width="100%" border="0">
	<tr class="rightPaneData"><td><xsl:value-of select="." disable-output-escaping="yes"/></td></tr>
  </table>
</xsl:template>

<xsl:template match="axis4">
  <table cellspacing="0" cellpadding="0" width="100%" border="0">
	<tr class="rightPaneData"><td>Axis IV: <xsl:value-of select="."/></td></tr>
  </table>
</xsl:template>

<xsl:template match="axis5">
  <table cellspacing="0" cellpadding="0" width="100%" border="0">
	<tr class="rightPaneData"><td>Axis V: <xsl:value-of select="."/></td></tr>
  </table>
</xsl:template>

<xsl:template match="treatment">
<xsl:choose>
<xsl:when test="assessment != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Treatment"/></td></tr>
    <tr class="rightPaneData"><td>
      <table class="rightPaneData" cellspacing="0" cellpadding="0" width="100%">
        <xsl:for-each select="assessment">
          <tr><td>
	  <xsl:if test="(position()!=1)"><br/></xsl:if>
           <b><xsl:value-of select="position()"/>&#46;&#160;<xsl:value-of select="name"/></b>&#160;
           <xsl:for-each select="rx"><br/>
             <xsl:value-of select="."/>
           </xsl:for-each>
           <xsl:for-each select="Test">
		      	 <br />
				   &#160;&#160;&#160;&#160;&#160;<u>LAB: <xsl:value-of select="TestName" /></u>
				    <xsl:if test="Result !=''">
				        &#160;<span class="normaltext"><xsl:value-of select="Result" /></span>
				    </xsl:if>
				    <xsl:if test="TestValues">
				    	<table cellspacing="0" cellpadding="0" class="rightPaneData" border="0" width="100%">
				  		  <xsl:for-each select="TestValues">
				  		  <xsl:if test="LabFlag =''">
						     <tr>
							     <td width="50%" align="left">
					 				&#160;&#160;&#160;&#160;&#160;<xsl:value-of select="LabName" />
					 			</td>
								<td width="20%" align="left">
					 				<xsl:value-of select="LabValue" />
					 			</td>
					 			<td width="30%" align="left">
					 				<xsl:value-of select="LabRange" />
					 			</td>
							</tr>
		  				</xsl:if>
						<xsl:if test="LabFlag !=''">
						     <tr>
							     <td width="50%" align="left" style="font-weight:bold;">
					 				&#160;&#160;&#160;&#160;&#160;<xsl:value-of select="LabName" />
					 			</td>
								<td width="20%" align="left" style="font-weight:bold;">
					 				<xsl:value-of select="LabValue" />
					 			</td>
					 			<td width="30%" align="left" style="font-weight:bold;">
					 				<xsl:value-of select="LabRange" />
					 			</td>
							</tr>
						</xsl:if>
				   		 </xsl:for-each>
				    	</table>
				    </xsl:if>
				    <xsl:if test="Notes !=''">
				      <table class="rightPaneData"><tr>
				       <td> &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;</td>
				       <td> <span class="normaltext"><xsl:value-of  select="Notes" /></span></td>
				       </tr>
				     </table>
				    </xsl:if>
	           </xsl:for-each>
           <xsl:for-each select="Xrayname">
            <br/><xsl:value-of select="."/>&#160;&#160;
           </xsl:for-each>
           <xsl:for-each select="Procedurename">
            <br/><xsl:value-of select="."/>&#160;&#160;
           </xsl:for-each>           
           <xsl:for-each select="notes">
            <br/><xsl:value-of select="."/>&#160;&#160;
           </xsl:for-each>
           <xsl:for-each select="notesBR">
            <br/><xsl:value-of select="." disable-output-escaping="yes"/>&#160;&#160;
           </xsl:for-each>
           <xsl:for-each select="Referral">
  &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;
  <br/> Referral To:<xsl:value-of select="RefName" />
     <xsl:if test="RefTo !=''"><xsl:value-of select="RefTo" /></xsl:if>
      <xsl:if test="FacilityTo !=''">&#160;<xsl:value-of select="FacilityTo" />
    </xsl:if>
     <xsl:if test="Speciality !=''">
     &#160;&#160;
    <xsl:value-of select="Speciality" />
    </xsl:if>
    <br/>Reason:<xsl:value-of select="Reason" /><br/>
    </xsl:for-each>
          </td></tr>

        </xsl:for-each>
      </table>
    </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>


<xsl:template match="CurrentMeds">
  <table cellspacing="0" cellpadding="0" width="100%" border="0">
    <tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/CurrentMedication"/></td></tr>
    <tr class="leftPaneData"><td>
     <xsl:for-each select="itemValue">
 		<table class="leftPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="."/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
     </td></tr>
  </table>
</xsl:template>

<xsl:template match="PastHistory">
  <xsl:choose>
  <xsl:when test="itemValue != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/MedicalHistory"/></td></tr>
    <tr class="leftPaneData"><td>
     <xsl:for-each select="itemValue">
 		<table class="leftPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="."/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
     </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="SurgicalHistory">
  <xsl:choose>
  <xsl:when test=". != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/SurgicalHistory"/></td></tr>
    <tr class="leftPaneData"><td>
     <xsl:for-each select="itemValue">
 		<table class="leftPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="."/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
     </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="FamilyHistory">
  <xsl:choose>
  <xsl:when test=". != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/FamilyHistory"/></td></tr>
    <tr class="leftPaneData"><td>
     <xsl:for-each select="itemValue">
 		<table class="leftPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="."/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
      <xsl:for-each select="itemValueBR">
 		<table class="leftPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="." disable-output-escaping="yes"/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
     </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="SocialHistory">
<xsl:variable name="structDataDisplay" select='structDataDisplay'/>
<xsl:variable name="showTreeInSocialHistory" select='showTreeInSocialHistory'/>
  <xsl:choose>
  <xsl:when test=". != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/SocialHistory"/></td></tr>
    <tr class="leftPaneData"><td>

      <xsl:for-each select="category">
      <xsl:variable name="treeview" select='count(categoryName)'/>
             <xsl:if test="not(position()=1)">
               <br/>
             </xsl:if>
             <xsl:if test="count(categoryName) != 0">
                <u><xsl:value-of select="categoryName"/></u>&#160;
			   <br/>
			 </xsl:if>
			    <xsl:for-each select="socialDetail">
			   <xsl:if test="position()=1 and $structDataDisplay != '1'">
               		<xsl:if test="$showTreeInSocialHistory = 1">&#160;&#160;&#160;&#160;&#160;&#160;</xsl:if>
             	</xsl:if>
			    
           <xsl:variable name="structFlag" select='struct'/>
           <xsl:if test="($structFlag='0')">
                <xsl:value-of select="value"/>
           </xsl:if>
           <xsl:if test="($structFlag='1') ">
              <xsl:value-of select="itemname"/>&#160;
           <xsl:if test="count(StData) != 0">
              <xsl:for-each select="StData">
                  <xsl:variable name="stType" select='type'/>
                  <xsl:attribute name="encid">
                   <xsl:value-of select="encId"/>
                  </xsl:attribute>
                  <xsl:attribute name="categoryId">
                   <xsl:value-of select="catId"/>
                  </xsl:attribute>
                  <xsl:attribute name="itemId">
                    <xsl:value-of select="propId"/>
                  </xsl:attribute>
                  <xsl:attribute name="structId">
                    <xsl:value-of select="structId"/>
                  </xsl:attribute>
                  <xsl:attribute name="structType">
                    <xsl:value-of select="type"/>
                  </xsl:attribute>
                  <xsl:choose>
                      <xsl:when test="$structDataDisplay ='1'">
                        <br/>&#160;&#160;&#160;
                      </xsl:when>
                      <xsl:when test="($structDataDisplay ='0' or $structDataDisplay ='2') and not(position()=1)">,&#160;</xsl:when>
                  </xsl:choose>
                  <xsl:value-of select="name"/>
                  <xsl:choose>
                     <xsl:when test="$structDataDisplay ='1'">
                     &#160;<i><xsl:value-of select="value"/></i>
                     </xsl:when>
                     <xsl:when test="$structDataDisplay ='0'">
                     &#160;<xsl:value-of select="value"/>
                     </xsl:when>
                     <xsl:when test="$structDataDisplay ='2'">
                     &#160;<b><xsl:value-of select="value"/></b>
                     </xsl:when>
                 </xsl:choose>
              </xsl:for-each>
           </xsl:if>
           <xsl:if test="count(StData) = 0">
                    <xsl:value-of select="value"/>.&#160;
           </xsl:if>
              <xsl:if test="($structDataDisplay ='0' or $structDataDisplay ='2') and count(StData) != 0">.&#160;</xsl:if>
           </xsl:if>
           <xsl:if test="$structDataDisplay ='1' and position() != last()">
                <br/>
            </xsl:if>
       </xsl:for-each>

     </xsl:for-each>
       <xsl:for-each select="itemValue">
        <table class="leftPaneData" cellspacing="0" cellpadding="0">
          <tr>
            <td>
               <xsl:value-of select="."/>
             </td>
           </tr>
         </table>
     </xsl:for-each>
       <xsl:for-each select="itemValueBR">
        <table class="leftPaneData" cellspacing="0" cellpadding="0">
          <tr>
            <td>
               <xsl:value-of select="." disable-output-escaping="yes"/>
             </td>
           </tr>
         </table>
     </xsl:for-each>
     <xsl:for-each select="category">
	<xsl:if test="categoryNotes !=''">
	<br/><xsl:value-of select="categoryNotes"/>
	</xsl:if>
      </xsl:for-each>
       <xsl:for-each select="category">
    <xsl:if test="categoryNotesBR !=''">
    <xsl:if test="count(categoryName) != 0 or $structDataDisplay = 1">
    <br/>
    </xsl:if><xsl:value-of select="categoryNotesBR" disable-output-escaping="yes"/>
    </xsl:if>
      </xsl:for-each>


     </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="gynHistory">
 <xsl:variable name="structDataDisplay" select='structDataDisplay'/>
  <xsl:choose>
  <xsl:when test=". != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/GynHistory"/></td></tr>
    <tr class="leftPaneData"><td>
     <xsl:for-each select="category">
                <xsl:if test="position() != 1">
                <br/>
                </xsl:if>
           <xsl:value-of select="catName"/>&#160;
                 <xsl:variable name="structFlag" select='struct'/>
                 <xsl:choose>
                 <xsl:when test="$structFlag = 1">
                 <xsl:if test="count(StData) != 0">
                     <xsl:for-each select="StData">
                        <xsl:if test="$structDataDisplay = 1">
                            <br/>&#160;&#160;&#160;
                        </xsl:if>
                        <xsl:value-of select="name"/>&#160;
                         <xsl:choose>
                           <xsl:when test="$structDataDisplay = 0">
                               <xsl:value-of select="value"/>
                           </xsl:when>
                           <xsl:when test="$structDataDisplay = 2">
                               <b><xsl:value-of select="value"/></b>
                           </xsl:when>
                           <xsl:when test="$structDataDisplay = 1">
                               <i><xsl:value-of select="value"/></i>
                           </xsl:when>
                         </xsl:choose>
                         <xsl:if test="position() != last() and $structDataDisplay != 1">,&#160;</xsl:if>
                         <xsl:if test="position() = last() and $structDataDisplay != 1">.&#160;</xsl:if>
                     </xsl:for-each>
                  </xsl:if>
                   <xsl:if test="count(StData) = 0">
                      <xsl:value-of select="value"/>.&#160;
                  </xsl:if>
                 </xsl:when>
                 <xsl:otherwise>
                    <xsl:value-of select="value"/>.&#160;
                 </xsl:otherwise>
                 </xsl:choose>
     </xsl:for-each>
     </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="obHistory">
 <xsl:variable name="structDataDisplay" select='structDataDisplay'/>
  <xsl:choose>
  <xsl:when test=". != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/OBHistory"/></td></tr>
    <tr class="leftPaneData"><td>
     <xsl:for-each select="category">
                <xsl:if test="position() != 1">
                <br/>
                </xsl:if>
           <xsl:value-of select="catName"/>&#160;
                 <xsl:variable name="structFlag" select='struct'/>
                 <xsl:choose>
                 <xsl:when test="$structFlag = 1">
                 <xsl:if test="count(StData) != 0">
                     <xsl:for-each select="StData">
                        <xsl:if test="$structDataDisplay = 1">
                            <br/>&#160;&#160;&#160;
                        </xsl:if>
                        <xsl:value-of select="name"/>&#160;
                         <xsl:choose>
                           <xsl:when test="$structDataDisplay = 0">
                               <xsl:value-of select="value"/>
                           </xsl:when>
                           <xsl:when test="$structDataDisplay = 2">
                               <b><xsl:value-of select="value"/></b>
                           </xsl:when>
                           <xsl:when test="$structDataDisplay = 1">
                               <i><xsl:value-of select="value"/></i>
                           </xsl:when>
                         </xsl:choose>
                         <xsl:if test="position() != last() and $structDataDisplay != 1">,&#160;</xsl:if>
                         <xsl:if test="position() = last() and $structDataDisplay != 1">.&#160;</xsl:if>
                     </xsl:for-each>
                  </xsl:if>
                   <xsl:if test="count(StData) = 0">
                      <xsl:value-of select="value"/>.&#160;
                  </xsl:if>
                 </xsl:when>
                 <xsl:otherwise>
                    <xsl:value-of select="value"/>.&#160;
                 </xsl:otherwise>
                 </xsl:choose>
     </xsl:for-each>
     </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="allergies">
  <xsl:choose>
  <xsl:when test="itemValue != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Allergies"/></td></tr>
    <tr class="leftPaneData"><td>
     <xsl:for-each select="itemValue">
 		<table class="leftPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="."/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
     </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="Hospitalization">
  <xsl:choose>
  <xsl:when test="itemValue != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Hospitalization"/></td></tr>
    <tr class="leftPaneData"><td>
     <xsl:for-each select="itemValue">
 		<table class="leftPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="."/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
     </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="ros">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/ROS"/></td></tr>
    <tr class="leftPaneData"><td>
    <xsl:for-each select="category">
      <xsl:if test="(position()!=1)">
	<br/>
      </xsl:if>
      <u><xsl:value-of select="categoryName"/></u>:&#160;
	<xsl:for-each select="rosDetail">
		    <xsl:if test="(position()=1)">
		    	<br/>&#160;&#160;&#160;&#160;&#160;&#160;
		    </xsl:if>

            <xsl:choose>
              <xsl:when test="starts-with(value,'*')"><xsl:value-of select="translate(value,'*','')" />&#160;<xsl:value-of select="name" /></xsl:when>
              <xsl:when test="value = 'yes' "><xsl:value-of select="name" />&#160;<xsl:value-of select="translate(value,'*','')" /></xsl:when>
              <xsl:when test="value = 'no' "><xsl:value-of select="translate(value,'*','')" />&#160;<xsl:value-of select="name" /></xsl:when>
              <xsl:otherwise><xsl:value-of select="name" /><xsl:if test="value !=''">&#160;</xsl:if><xsl:value-of select="translate(value,'*','')" /></xsl:otherwise>
            </xsl:choose>

<xsl:if test="notes !=''">
<xsl:if test="value !=''">,&#160;<xsl:value-of select="rosNotes2" disable-output-escaping="yes"/></xsl:if>
<xsl:if test="value =''">&#160;<xsl:value-of select="rosNotes2" disable-output-escaping="yes"/></xsl:if>
</xsl:if>.&#160;


	</xsl:for-each>
       &#160;&#160;
    </xsl:for-each>
    <xsl:if test="count(category) > 0"><br/></xsl:if><xsl:for-each select="notesBR">
      <xsl:value-of select="." disable-output-escaping="yes"/>
    </xsl:for-each>
    </td></tr>
  </table>
</xsl:template>

<xsl:template match="cc">
   <xsl:choose>
   <xsl:when test="itemValue != ''">
   <table cellspacing="0" cellpadding="0" width="100%">
     <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/CC"/></td></tr>
     <tr class="rightPaneData"><td>
     <xsl:for-each select="itemValue">
 		<table class="rightPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="position()"/>&#46;&#160;<xsl:value-of select="."/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
     </td></tr>
   </table>
   </xsl:when>
   </xsl:choose>
</xsl:template>

<xsl:template match="preventive">
  <xsl:choose>
  <xsl:when test=". != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/PreventiveMedicine"/></td></tr>
    <tr class="rightPaneData"><td>
    <xsl:for-each select="itemValue">
 		<table class="rightPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="."/><br/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
    </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="PhysicalExamination">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/PhysicalExamination"/></td></tr>
    <tr class="rightPaneData"><td>
    <xsl:for-each select="category">
      	<xsl:if test="(position()!=1)">
	<br/>
    </xsl:if>
    <u><xsl:value-of select="categoryName"/></u>:&#160;
	<br/>&#160;&#160;&#160;&#160;&#160;&#160;
      	<xsl:for-each select="categoryPhyExamDetail">
            <xsl:value-of select="itemName" />&#160;<xsl:value-of select="phyExamNotes" disable-output-escaping="yes"/>.&#160;
 	</xsl:for-each>
    </xsl:for-each>

    	<br/>&#160;&#160;&#160;

	<xsl:for-each select="notes">
      		<xsl:value-of select="."/>
    	</xsl:for-each>
   </td></tr>
  </table>
</xsl:template>

<xsl:template match="hpi">
<xsl:variable name="structDataDisplay" select='structDataDisplay'/>
   <xsl:choose>
   <xsl:when test=". != ''">
   <table cellspacing="0" cellpadding="0" width="100%">
      <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/HPI"/></td></tr>
      <tr class="rightPaneData"><td>
      <xsl:for-each select="category">
	<xsl:if test="(position()!=1)">
	<br/>
	</xsl:if>
	<u><xsl:value-of select="categoryName"/></u>:&#160;
	<xsl:if test="categoryNotesHeader != ' '">
		<br/>&#160;&#160;&#160;&#160;&#160;&#160; <xsl:value-of select="categoryNotesHeader" disable-output-escaping="yes"/>
	</xsl:if>
                 <xsl:for-each select="HpiDetail1">
                 	<xsl:if test="(position()=1)">
		    	  <br/>&#160;&#160;&#160;&#160;&#160;&#160;
		   	</xsl:if>

		   	    <xsl:if test="(position()=1)">
		    		<xsl:value-of select="prefix1" />
		   	    </xsl:if>
			    <xsl:value-of select="prefix2" />
                            <xsl:value-of select="hpiName" />

                   <xsl:if test="duration !=''">&#160;for&#160;<xsl:value-of select="duration" /></xsl:if>

                <xsl:variable name="structFlag" select='struct'/>

               <xsl:if test="($structFlag='0') and (notes !='') ">&#160;<xsl:value-of select="hpiNotes2" disable-output-escaping="yes"/>.&#160;</xsl:if>
                <xsl:if test="($structFlag='1') and (notes !='') ">&#160;
                <xsl:if test="count(StData) != 0">
                    <xsl:for-each select="StData">
                             <xsl:choose>
                                 <xsl:when test="$structDataDisplay ='1'">
                                   <br/>&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;
                                   </xsl:when>
                                   <xsl:when test="($structDataDisplay ='0' or $structDataDisplay ='2') and not(position()=1)">,&#160;</xsl:when>
                                   </xsl:choose>
                                <xsl:value-of select="name"/>
                                 <xsl:choose>
                                    <xsl:when test="$structDataDisplay ='1'">
                                    &#160;<i><xsl:value-of select="value"/></i>
                                    </xsl:when>
                                    <xsl:when test="$structDataDisplay ='0'">
                                    &#160;<xsl:value-of select="value"/>
                                    </xsl:when>
                                    <xsl:when test="$structDataDisplay ='2'">
                                    &#160;<b><xsl:value-of select="value"/></b>
                                    </xsl:when>
                                </xsl:choose>
                    </xsl:for-each>
                     </xsl:if>
                   <xsl:if test="count(StData) = 0">&#160;<xsl:value-of select="hpiNotes2" disable-output-escaping="yes"/>.&#160;</xsl:if>
                    <xsl:if test="($structDataDisplay ='0' or $structDataDisplay ='2') and count(StData) != 0">.&#160;</xsl:if>
                </xsl:if><xsl:if test="(count(StData) = 0 and notes = '')">.&#160;</xsl:if>
                     <xsl:if test="$structDataDisplay ='1' and position() != last()">
                         <br/>&#160;&#160;&#160;&#160;&#160;&#160;
                     </xsl:if>
                 </xsl:for-each>

                 <xsl:for-each select="HpiDetail2">
                 	<xsl:if test="(position()=1)">
		    			<br/>&#160;&#160;&#160;&#160;&#160;&#160;
		   			</xsl:if>

		   			<xsl:if test="(position()=1)">
		    			<xsl:value-of select="prefix" />
		   			</xsl:if>
                     <xsl:value-of select="hpiName" />

                     <xsl:if test="duration !=''">&#160;for&#160;<xsl:value-of select="duration" /></xsl:if>

                     <xsl:variable name="structFlag" select='struct'/>

               <xsl:if test="($structFlag='0') and (notes !='') ">&#160;<xsl:value-of select="hpiNotes2" disable-output-escaping="yes"/>.&#160;</xsl:if>
                <xsl:if test="($structFlag='1') and (notes !='') ">&#160;
                 <xsl:if test="count(StData) != 0">
                    <xsl:for-each select="StData">
                             <xsl:choose>
                                 <xsl:when test="$structDataDisplay ='1'">
                                   <br/>&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;
                                   </xsl:when>
                                   <xsl:when test="($structDataDisplay ='0' or $structDataDisplay ='2') and not(position()=1)">,&#160;</xsl:when>
                                   </xsl:choose>
                                <xsl:value-of select="name"/>
                                 <xsl:choose>
                                    <xsl:when test="$structDataDisplay ='1'">
                                    &#160;<i><xsl:value-of select="value"/></i>
                                    </xsl:when>
                                    <xsl:when test="$structDataDisplay ='0'">
                                    &#160;<xsl:value-of select="value"/>
                                    </xsl:when>
                                    <xsl:when test="$structDataDisplay ='2'">
                                    &#160;<b><xsl:value-of select="value"/></b>
                                    </xsl:when>
                                </xsl:choose>
                    </xsl:for-each>
                    </xsl:if>
                   <xsl:if test="count(StData) = 0">&#160;<xsl:value-of select="hpiNotes2" disable-output-escaping="yes"/>.&#160;</xsl:if>
                    <xsl:if test="($structDataDisplay ='0' or $structDataDisplay ='2') and count(StData) != 0">.&#160;</xsl:if>
                </xsl:if><xsl:if test="($structFlag='0' and count(StData) = 0 and notes = '')">.&#160;</xsl:if>
                     <xsl:if test="$structDataDisplay ='1' and position() != last()">
                         <br/>&#160;&#160;&#160;&#160;&#160;&#160;
                     </xsl:if>
                 </xsl:for-each>
                 <xsl:for-each select="HpiDetail3">
                 	<xsl:if test="(position()=1)">
		    			<br/>&#160;&#160;&#160;&#160;&#160;&#160;
		   			</xsl:if>

                     <xsl:value-of select="hpiName" />

                     <xsl:if test="duration !=''">&#160;for&#160;<xsl:value-of select="duration" /></xsl:if>

                     <xsl:variable name="structFlag" select='struct'/>

               <xsl:if test="($structFlag='0') and (notes !='') ">&#160;<xsl:value-of select="hpiNotes2" disable-output-escaping="yes"/>.&#160;</xsl:if>
                <xsl:if test="($structFlag='1') and (notes !='') ">&#160;
                <xsl:if test="count(StData) != 0">
                    <xsl:for-each select="StData">
                             <xsl:choose>
                                 <xsl:when test="$structDataDisplay ='1'">
                                   <br/>&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;
                                   </xsl:when>
                                   <xsl:when test="($structDataDisplay ='0' or $structDataDisplay ='2') and not(position()=1)">,&#160;</xsl:when>
                                   </xsl:choose>
                                <xsl:value-of select="name"/>
                                 <xsl:choose>
                                    <xsl:when test="$structDataDisplay ='1'">
                                    &#160;<i><xsl:value-of select="value"/></i>
                                    </xsl:when>
                                    <xsl:when test="$structDataDisplay ='0'">
                                    &#160;<xsl:value-of select="value"/>
                                    </xsl:when>
                                    <xsl:when test="$structDataDisplay ='2'">
                                    &#160;<b><xsl:value-of select="value"/></b>
                                    </xsl:when>
                                </xsl:choose>
                    </xsl:for-each>
                  </xsl:if>
                   <xsl:if test="count(StData) = 0">&#160;<xsl:value-of select="hpiNotes2" disable-output-escaping="yes"/>.&#160;</xsl:if>
                    <xsl:if test="($structDataDisplay ='0' or $structDataDisplay ='2') and count(StData) != 0">.&#160;</xsl:if>
                </xsl:if>
                     <xsl:if test="$structDataDisplay ='1' and position() != last()">
                         <br/>&#160;&#160;&#160;&#160;&#160;&#160;
                     </xsl:if>
                 </xsl:for-each>

	<xsl:if test="hpiCatNotes != ' '">
	<br/>&#160;&#160;&#160;&#160;&#160;&#160; <xsl:value-of select="hpiCatNotes" disable-output-escaping="yes"/>
	</xsl:if>
      </xsl:for-each>
      <xsl:value-of select="itemAdtlNotes"/>
   </td></tr>
   </table>
   </xsl:when>
   </xsl:choose>
</xsl:template>

<xsl:template match="xrays">
  <table cellspacing="0" cellpadding="0" width="100%">
  <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/DiagnosticImaging"/></td></tr>
  <tr class="rightPaneData"><td><xsl:value-of select="."/></td></tr>
  </table>
</xsl:template>

<xsl:template match="labs">
  <table cellspacing="0" cellpadding="0" width="100%">
  	<tr class="rightPaneHeading">
  		<td width="10"></td>
  		<td>
  			<xsl:value-of select="//return/ChartHeadings/LabReports"/>
  			&#160;
  		</td>
  	</tr>
  </table>
  <table cellspacing="0" cellpadding="0" width="100%">
  	<xsl:for-each select="labdetail">
  	<tr class="rightPaneData"><td>
  		&#160;&#160;&#160;&#160;&#160;
  		<u>Lab: <xsl:value-of select="TestName" /></u>
  		<xsl:if test="Result !=''">
  			&#160;
  			<xsl:value-of select="Result" />
  		</xsl:if>
  		</td></tr>
  		<tr class="rightPaneData"><td>
  		<xsl:if test="TestValues">
  			<table cellspacing="0" cellpadding="0" border="0" width="100%">
  				<xsl:for-each select="TestValues">
  				<xsl:if test="LabFlag =''">
  					<tr class="rightPaneData">
  						<td width="50%" align="left">
  							&#160;&#160;&#160;&#160;&#160;
  							<xsl:value-of select="LabName" />
  						</td>
  						<td width="20%" align="left">
  							<xsl:value-of select="LabValue" />
  						</td>
  						<td width="30%" align="left">
  							<xsl:value-of select="LabRange" />
  						</td>
  					</tr>
  				</xsl:if>
				<xsl:if test="LabFlag !=''">
  					<tr class="rightPaneData">
  						<td width="50%" align="left" style="font-weight:bold;">
  							&#160;&#160;&#160;&#160;&#160;
  							<xsl:value-of select="LabName" />
  						</td>
  						<td width="20%" align="left" style="font-weight:bold;">
  							<xsl:value-of select="LabValue" />
  						</td>
  						<td width="30%" align="left" style="font-weight:bold;">
  							<xsl:value-of select="LabRange" />
  						</td>
  					</tr>
				</xsl:if>
  				</xsl:for-each>
  			</table>
  		</xsl:if>
  		</td></tr>
  		<tr class="rightPaneData"><td>
  		<xsl:if test="Notes !=''">
  			<table>
  				<tr class="rightPaneData">
  					<td>
  						&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;
  					</td>
  					<td>
  						<span class="normaltext">
  							<xsl:value-of select="Notes" />
  						</span>
  					</td>
  				</tr>
  			</table>
  		</xsl:if>
  		</td></tr>
  	</xsl:for-each>
  </table>
</xsl:template>

<xsl:template match="procedures">
  <xsl:choose>
  <xsl:when test=". != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
  <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/ProcedureCodes"/></td></tr>
  <tr class="rightPaneData"><td>
    <xsl:for-each select="itemValue">
 		<table class="rightPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="."/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
    </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="FollowUp">
  <table cellspacing="0" cellpadding="0" width="100%">
     <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/NextAppointment"/></td></tr>
     <tr class="rightPaneData"><td><xsl:value-of select="."/></td></tr>
  </table>
</xsl:template>

<xsl:template match="vitals">
  <table cellspacing="0" cellpadding="0" width="100%">
     <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Vitals"/></td></tr>
     <tr class="rightPaneData"><td><xsl:value-of select="."/></td></tr>
  </table>
</xsl:template>

<xsl:template match="vitals2">
  <table cellspacing="0" cellpadding="0" width="100%">
     <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Vitals"/></td></tr>
     <tr class="rightPaneData"><td><xsl:value-of select="." disable-output-escaping="yes"/></td></tr>
  </table>
</xsl:template>

<xsl:template match="vitals2BR">
  <table cellspacing="0" cellpadding="0" width="100%">
     <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Vitals"/></td></tr>
     <tr class="rightPaneData"><td><xsl:value-of select="." disable-output-escaping="yes"/></td></tr>
  </table>
</xsl:template>

<xsl:template match="examination">
<xsl:variable name="structDataDisplay" select='structDataDisplay'/>
   <xsl:choose>
   <xsl:when test=". != ''">
   <table cellspacing="0" cellpadding="0" width="100%">
      <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Examination"/></td></tr>
      <tr class="rightPaneData"><td>
      <xsl:for-each select="category">
			<xsl:variable name="ExamCatName"><xsl:value-of select="categoryName"/></xsl:variable>
			<xsl:variable name="ExamCatNotesBR"><xsl:value-of select="categoryNotesBR"/></xsl:variable>
			<xsl:variable name="ExamCatNotes"><xsl:value-of select="categoryNotes"/></xsl:variable>
				<xsl:if test="$ExamCatName !=''">
         <u><xsl:value-of select="categoryName"/></u>:<br/>&#160;&#160;&#160;&#160;&#160;&#160;
						<xsl:for-each select="categoryInDetail">
							<xsl:for-each select="cat_det">
							<xsl:variable name="structFlag2" select='struct'/>
							<xsl:if test="$structDataDisplay ='1' and position() != '1'">
                            <br/>&#160;&#160;&#160;&#160;&#160;&#160;
                            </xsl:if>
							<xsl:if test="($structFlag2='0')">
									<xsl:value-of select="categorySubName" />&#160;<xsl:value-of select="examNotes2" disable-output-escaping="yes"/>.&#160;
							</xsl:if>
							<xsl:if test="($structFlag2='1') ">
							 <xsl:value-of select="categorySubName" />&#160;
						<xsl:if test="count(StData) != 0">
							 <xsl:for-each select="StData">
							     <xsl:variable name="stType" select='type'/>
							     <span class="normaltext">
								     <xsl:attribute name="encid">
	                                    <xsl:value-of select="encId"/>
	                                 </xsl:attribute>
                                     <xsl:attribute name="categoryId">
                                        <xsl:value-of select="catId"/>
                                     </xsl:attribute>
		                             <xsl:attribute name="itemId">
		                                <xsl:value-of select="propId"/>
		                             </xsl:attribute>

                                     <xsl:attribute name="structId">
                                        <xsl:value-of select="structId"/>
                                     </xsl:attribute>
                                     <xsl:attribute name="structType">
                                        <xsl:value-of select="type"/>
                                     </xsl:attribute>
                                     <xsl:choose>

	                                   <xsl:when test="$structDataDisplay ='1'">
	                                       <br/>&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;
	                                   </xsl:when>
	                                   <xsl:when test="($structDataDisplay ='0' or $structDataDisplay ='2') and not(position()=1)">,&#160;</xsl:when>
	                                 </xsl:choose>
	                                 <xsl:value-of select="name"/>
	                                 <xsl:choose>
	                                    <xsl:when test="$structDataDisplay ='1'">
	                                    &#160;<i><xsl:value-of select="value"/></i>
	                                    </xsl:when>
	                                    <xsl:when test="$structDataDisplay ='0'">
	                                    &#160;<xsl:value-of select="value"/>
	                                    </xsl:when>
	                                    <xsl:when test="$structDataDisplay ='2'">
	                                    &#160;<b><xsl:value-of select="value"/></b>
	                                    </xsl:when>
                                     </xsl:choose>
                                  </span>
							 </xsl:for-each>
						</xsl:if>
						<xsl:if test="count(StData) = 0">
                            <xsl:value-of select="examNotes" disable-output-escaping="yes"/>.&#160;
                        </xsl:if>
							 <xsl:if test="($structDataDisplay ='0' or $structDataDisplay ='2') and count(StData) != 0">.&#160;</xsl:if>
							 <xsl:if test="count(StData) != 0 and $ExamCatNotesBR =''"><br/></xsl:if>
						</xsl:if>
						<xsl:if test="($structFlag2='0' and position() = last() and $ExamCatNotesBR ='')"><br/></xsl:if>
							</xsl:for-each>
						</xsl:for-each>
						
						
						<xsl:if test="$ExamCatNotes !=''">
							&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;<xsl:value-of select="categoryNotes"/><br/>
						</xsl:if>
						<xsl:if test="$ExamCatNotesBR !=''">
							<xsl:if test="count(categoryInDetail) != 0">
	                            <br/>&#160;&#160;&#160;&#160;&#160;&#160;
	                        </xsl:if>
                            <xsl:value-of select="categoryNotesBR" disable-output-escaping="yes"/><br/>
                        </xsl:if>
				</xsl:if>
      </xsl:for-each>
   </td></tr>
   </table>
   </xsl:when>
   </xsl:choose>
</xsl:template>
<xsl:template match="Procedure">
    <xsl:choose>
    <xsl:when test=". != ''">
    <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Procedures"/></td></tr>
    <tr class="rightPaneData">
    <td>
    <xsl:for-each select="category">
      <xsl:if test="(position()!=1)">
    <br/>
      </xsl:if>
      <u><xsl:value-of select="categoryName"/></u>:&#160;
    <xsl:for-each select="procDetail">
            <xsl:if test="(position()=1)">
                <br/>&#160;&#160;&#160;&#160;&#160;&#160;
            </xsl:if>
            <xsl:value-of select="name" />
            <xsl:if test="procnotes2 !=''">&#160;<xsl:value-of select="procnotes2" disable-output-escaping="yes"/></xsl:if>.&#160;
    <xsl:if test="position() != last()">
           <br/>&#160;&#160;&#160;&#160;&#160;&#160;
   	</xsl:if>
    </xsl:for-each>
       &#160;&#160;
    </xsl:for-each>
    <xsl:if test="count(category) != 0">
    <br/>
    </xsl:if>
   <xsl:for-each select="categoryNotesBR">
      <xsl:value-of select="." disable-output-escaping="yes"/>
    </xsl:for-each>
    </td></tr>
  </table>
   </xsl:when>
   </xsl:choose>
</xsl:template>

<xsl:template match="Therapeutic">
    <table cellspacing="0" cellpadding="0" width="100%">
      <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Therapeutic"/></td></tr>
      <tr class="rightPaneData"><td>
      <xsl:for-each select="category">
	<u><xsl:value-of select="categoryName"/></u>
	<br/>
	<xsl:for-each select="categoryValues">
	    <xsl:for-each select="categoryValue">
         		&#160;&#160;&#160;&#160;&#160;&#160; <xsl:value-of select="."/><br/>
	    </xsl:for-each>
	 </xsl:for-each>
        </xsl:for-each>
     <xsl:for-each select="category">
	&#160;&#160;&#160;&#160;&#160;&#160;<xsl:value-of select="categoryNotes"/>
      </xsl:for-each>
   </td></tr>
   </table>
</xsl:template>

<xsl:template match="TherapyAssessment">
    <table cellspacing="0" cellpadding="0" width="100%">
      <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/TherapyAssessment"/></td></tr>
      <tr class="rightPaneData"><td>
      <xsl:for-each select="category">
	<u><xsl:value-of select="categoryName"/></u>
	<br/>
	<xsl:for-each select="categoryValues">
	    <xsl:for-each select="categoryValue">
         		&#160;&#160;&#160;&#160;&#160;&#160; <xsl:value-of select="."/><br/>
	    </xsl:for-each>
	 </xsl:for-each>
        </xsl:for-each>
     <xsl:for-each select="category">
	&#160;&#160;&#160;&#160;&#160;&#160;<xsl:value-of select="categoryNotes"/>
      </xsl:for-each>
   </td></tr>
   </table>
</xsl:template>

<xsl:template match="assessment">
    <xsl:choose>
    <xsl:when test="//subItems/assessment != ''">
    <xsl:if test="(position()=1)">
	<table cellspacing="0" cellpadding="0" width="100%">
	<tr class="leftPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Assessment"/></td></tr>
	</table>
    </xsl:if>
    <table cellspacing="0" cellpadding="0" width="100%">
	    <tr class="rightPaneData"><td>
            <xsl:value-of select="position()"/>. <xsl:value-of select="."/>
            </td></tr>
    </table>
    </xsl:when>
    </xsl:choose>
</xsl:template>

<xsl:template match="Immunization">
  <xsl:choose>
  <xsl:when test="itemValue != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Immunizations"/></td></tr>
    <tr class="rightPaneData"><td>
    <xsl:for-each select="itemValue">
 		<table class="rightPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:value-of select="."/>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
    </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
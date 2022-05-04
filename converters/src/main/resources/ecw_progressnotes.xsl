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
	.leftPaneData{font-family:"Georgia";font-size:8.0pt;font-weight:"normal";color:"#000000";}
	.rightPaneHeading{font-family:"Arial Black";font-size:10.0pt;color:"#6495ED";}
	.rightPaneData{font-family:"Georgia";font-size:10.0pt;font-weight:"normal";color:"#000000";}
	.tfoot{display:table-footer-group;}
	.TableFooter{font-family:"Georgia";font-size:9.0pt;font-weight:"bold";color:"#9c9c9c";}
	.eclinicalworks{font-family:"Arial";font-style:italic;font-size:8.0pt;color:"#b9d0e8";}
	.PNSig{font-family:"Georgia";font-size:10.0pt;font-weight:"bold";color:"#6495ED";}
	.Facility{font-family:"Georgia";font-size:9.0pt;font-weight:"bold";color:"#6495ED";}
     TD.tableheading{font-family:"verdana,Georgia";font-size:8.0pt;color:"#000000";font-weight: bold;}
     TD.tablecell{font-family:"Georgia";font-size:9.0pt;color:"#000000";font-weight: normal;}
	 TD.TitleColor{font-family:"Georgia";font-size:9.0pt;font-weight:"normal";color:"#000000";BACKGROUND-COLOR:#EEEEFF;}
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

      <!-- CORRECTIONS DEMOGRAPHICS DATA -->
      <xsl:if test="//return/BookAndCaseNo!=''">
	      <TR class="PtData">
	        <TD ALIGN="right">
	        	<xsl:if test="//return/NYSIDNo!=''">
	        		<xsl:choose>
	        			<xsl:when test="//return/SIDLabel != ''">
	        				<b><xsl:value-of select="//return/SIDLabel"/>:&#160;</b>
	        			</xsl:when>
	        			<xsl:otherwise>
	        				<b>NYSID No:&#160;</b>
	        			</xsl:otherwise>
	        		</xsl:choose>
	          		<xsl:value-of select="//return/NYSIDNo"/>&#160;
	          	</xsl:if>
	          	<xsl:choose>
        			<xsl:when test="//return/BCLabel != ''">
        				<b><xsl:value-of select="//return/BCLabel"/>:&#160;</b>
        			</xsl:when>
        			<xsl:otherwise>
        				<b>B&amp;C No:&#160;</b>
        			</xsl:otherwise>
        		</xsl:choose>
	          	<xsl:value-of select="//return/BookAndCaseNo"/>&#160;
	       	</TD>
	       </TR>
	        <TR class="PtData">
	          <TD ALIGN="right">
	       		<xsl:if test="//return/FacilityCode!=''">
	          		<b>Facility Code:&#160;</b><xsl:value-of select="//return/FacilityCode"/>&#160;
	          	</xsl:if>
	          	<xsl:if test="//return/HousingArea!=''">
	          		<b>Housing Area:&#160;</b><xsl:value-of select="//return/HousingArea"/>
	          	</xsl:if>
	          </TD>
	       	</TR>
	  </xsl:if>

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
	<xsl:if test="//return/GuarantorName!=''">
          Guarantor:&#160;<xsl:value-of select="//return/GuarantorName"/>&#160;&#160;&#160;
	</xsl:if>
	<xsl:if test="//return/InsuranceName!=''">
	  Insurance:&#160;<xsl:value-of select="//return/InsuranceName"/>
	</xsl:if>
	<xsl:if test="//return/PayorID!=''">
	  Payer ID:&#160;<xsl:value-of select="//return/PayorID"/>
	</xsl:if>

        </td>
      </tr>

      <tr class="PtData">
        <td align="right">
          <xsl:if test="//return/pcp!=''">
		PCP:&#160;<xsl:value-of select="//return/pcp"/>&#160;&#160;&#160;
	  </xsl:if>
	  <xsl:if test="//return/refPr!=''">
		Referring:&#160;<xsl:value-of select="//return/refPr"/>
	  </xsl:if>
	  <xsl:if test="//return/HL7ID!='' and //return/HL7ID!='0'">
		&#160;&#160;External Visit ID:&#160;<xsl:value-of select="//return/HL7ID"/>
	  </xsl:if>

        </td>
      </tr>

      <tr class="PtData">
        <td align="right" colspan="2">
          <xsl:if test="//return/ApptFacility!=''">Appointment Facility:&#160;<xsl:value-of select="//return/ApptFacility"/></xsl:if>
        </td>
      </tr>

    </table>

    <hr/>

    <table cellspacing="0" cellpadding="0" width="100%">

      <tr>
        <td ALIGN="LEFT" class="PageHeader">
          <xsl:value-of select="//return/encDate"/>
        </td>
         <td align="right" class="PageHeader">

	   <xsl:if test="//return/Resident =''">
           	<xsl:if test="//return/NotesType!=''">
		<xsl:value-of select="//return/NotesType"/>:&#160;
	   	</xsl:if>


	   	<xsl:value-of select="//return/provider"/>
	   </xsl:if>

	   <xsl:if test="//return/Resident !=''">
	   	<b>Appointment Provider:&#160;</b><xsl:value-of select="//return/provider"/>
	   </xsl:if>

         </td>
      </tr>

	<xsl:if test="//return/AttendingPr !=''">
	<tr>

        <td ALIGN="LEFT" class="PageHeader">
          &#160;
        </td>
        <td ALIGN="right" class="PageHeader">

	   	<b>Supervising Provider:&#160;</b><xsl:value-of select="//return/AttendingPr"/>

        </td>

	</tr>
	</xsl:if>
    </table>

    <table cellspacing="0" cellpadding="0" width="100%">
         <tr height="20"><td></td></tr>
    </table>

   <table cellspacing="0" cellpadding="0" width="100%">
     <tbody>
     <tr>
     <td width="35%" bgcolor="#CCCCCC" style="border-right: 2pt solid #CCCCCC" >
     <table cellspacing="8" cellpadding="0" border="0" align="left" height="100%" width="100%">
       <xsl:if test="//subItems/CurrentMeds!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/CurrentMeds"/>
         </td>
	</tr>
	</xsl:if>
	
	
	
	 <tr>
	   <td valign="top">
	     <xsl:apply-templates select="//subItems/ProblemList"/>
	   </td>
	 </tr>
  
	
	<xsl:choose>	
	 <xsl:when test= " count(//subItems/ProblemList/Problem) = 0 and //subItems/ProblemList/NKP/NKPFlag = '1'">
	<tr>
  	   <td class="leftPaneData">No Known Problems</td>
  	</tr>
    </xsl:when>
    
    <xsl:when test= "//subItems/ProblemList/NAP/NAPFlag = '1'">
	<tr>
  	   <td class="leftPaneData">No Active Problems</td>
  	</tr>
    </xsl:when>
    
    <xsl:when test= "count(//subItems/ProblemList/Problem) = 0 and //subItems/ProblemList/NKP/NKPFlag = '0'">
	<tr>
  	   <td class="leftPaneData" style="color:red">Problem List has not been verified</td>
  	</tr>
    </xsl:when>
     
     </xsl:choose>

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

     </table>
     </td>
     <td width="65%">
     <table cellspacing="8" cellpadding="0" border="0" align="left" height="100%" width="100%">
 	<xsl:if test="//subItems/item/CarePlanProblem!='' ">
     	<tr class="rightPaneHeading">
     	 <td valign="top">
     	    CarePlanProblems 
              </td>
            </tr>
            
     	<tr class="rightPaneData">
     	 <td valign="top">
     	   <xsl:apply-templates select="//subItems/item/CarePlanProblem"/>
              </td>
            </tr>

     </xsl:if>
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
       	<xsl:if test="count(//return/ShowVitalHistory) = 0">
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
       	</xsl:if>
       	<xsl:if test="count(//return/ShowVitalHistory) > 0">
       		<xsl:if test="//subItems/vitalHistory!=''">
	       <tr>
	         <td valign="top">
	       <xsl:apply-templates select="//subItems/vitalHistory"/>
	         </td>
	       </tr>
	       </xsl:if>
       	</xsl:if>
       
       <xsl:if test="//subItems/PastOrders!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/PastOrders"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/VisionExamination!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/VisionExamination"/>
         </td>
       </tr>
       </xsl:if>

       <xsl:if test="//subItems/examination!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/examination"/>
	   <xsl:apply-templates select="//subItems/examination/category/MuscularExam"/>
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

       <xsl:if test="//subItems/assessment!='' or //subItems/notes or //subItems/notesBR">
				 <xsl:if test="//subItems/assessment!=''">
					 <tr>
						 <td valign="top">
								<xsl:apply-templates select="//subItems/assessment"/>
						 </td>
					</tr>
				</xsl:if>
				<xsl:if test="//subItems/notes!=''">
					<tr>
						 <td valign="top">
								<xsl:if test="count(//subItems/assessment) = 0">
									<table cellspacing="0" cellpadding="0" width="100%">
										<tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Assessment"/></td></tr>
									</table>
								</xsl:if>
								 <xsl:apply-templates select="//subItems/notes"/>
						 </td>
					</tr>
				</xsl:if>
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
	   <!--<xsl:apply-templates select="//subItems/assessment"/>-->
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

       <xsl:if test="//subItems/DispositionAndCommunication!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/DispositionAndCommunication"/>
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

       <xsl:if test="//subItems/disposition!=''">
       <tr>
         <td valign="top">
	   <xsl:apply-templates select="//subItems/disposition"/>
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


	<xsl:if test="//return/Resident !=''">
		<xsl:if test="//return/ResidentInfo">
	       	<tr height="10">
		   <td/>
		</tr>
		<tr><td valign="top">
		   <xsl:variable name="image2" select="//return/ResidentInfo/RenderingPrUname"/>
		   <img id="signature" width="175px" height="100px" src="https://static.apixio.com/images/clients/smchs/signatures/{$image2}.jpg"></img></td></tr>
		</xsl:if>
	</xsl:if>

	<xsl:if test="//return/Resident !=''">
		<tr><td>&#160;</td></tr>
		<tr class="PNSig"><td><b>Appointment Provider:</b>&#160;<xsl:value-of select="//return/provider"/></td></tr>
        </xsl:if>

	<xsl:if test="//return/Resident !='' and //return/AttPrNotes !='' ">

		<tr class="PNSig"><td><br/><b>Confirmatory sign off:</b><br/></td></tr>
		<tr><td>&#160;&#160;&#160;<xsl:value-of select="//return/AttPrNotes"/></td></tr>
	</xsl:if>


       <!--Signature goes here-->
	<xsl:if test="//return/ShowSignature='Yes'">
	       	<tr height="10">
		   <td/>
		</tr>
	       <tr>
		 <td valign="top">
		   <xsl:variable name="image" select="//return/userName"/>
		   <img id="signature" width="175px" height="100px" src="https://static.apixio.com/images/clients/smchs/signatures/{$image}.jpg"> </img>
		 </td>
		</tr>
		<tr height="10">
		   <td/>
		</tr>
	</xsl:if>

    <xsl:if test="//return/TimeStamp!=''">
		<tr height="20">
			<td/>
		</tr>
		<tr class="PNSig">
	         <td valign="top">
		   <xsl:value-of select="//return/TimeStamp"/>
	         </td>
		</tr>

		<xsl:if test="//return/CoSignedBy!=''">
			<tr class="PNSig">
	         <td valign="top">
		   		<xsl:value-of select="//return/CoSignedBy"/>
	         </td>
			</tr>
		</xsl:if>

		<tr class="PNSig">
	         <td valign="top">
		   <xsl:value-of select="//return/SignOffStatus"/>
	         </td>
		</tr>
    </xsl:if>

	<tr height="40">
	   <td/>
	</tr>

	<tr class="Facility" width="100%">
	 <td width="100%" align="center"><hr/><br/>
	   <xsl:value-of select="//return/HospitalName"/><br/>
	   <xsl:value-of select="//return/HospitalAddress1"/><br/>
	   <xsl:value-of select="//return/HospitalAddress3"/><br/>
	   Tel:&#160;<xsl:value-of select="//return/HospitalPhone"/><br/>
	   Fax:&#160;<xsl:value-of select="//return/HospitalFax"/><br/>
         </td>
       </tr>

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
		    <br/><td align="center">Patient:&#160;<xsl:value-of select="//return/patient"/>&#160;&#160;&#160;&#160;DOB:&#160;<xsl:value-of select="//return/DOB"/>&#160;&#160;&#160;&#160;Progress Note:&#160;<xsl:value-of select="//return/provider"/>&#160;&#160;&#160;&#160;<xsl:value-of select="//return/encDate"/></td>
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

<xsl:template match="//subItems/examination/category/MuscularExam">
  &#160;&#160;&#160;
  <span class="rightpanedata"><u>Musculoskeletal Exam</u></span><br/><br/>
  <table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#6495ED" style="border-collapse:collapse">
    <xsl:for-each select="TR">
    <tr>
      <xsl:for-each select="TD">
        <td nowrap="1" >
        <xsl:attribute name="colspan">
          <xsl:value-of select="@colspan" />
        </xsl:attribute>
        <xsl:attribute name="class">
          <xsl:value-of select="@class" />
        </xsl:attribute>
        <xsl:attribute name="align">
          <xsl:value-of select="@align" />
        </xsl:attribute>
        <xsl:attribute name="title">
          <xsl:value-of select="." disable-output-escaping="yes"/>
        </xsl:attribute>
         &#160;<xsl:value-of select="." disable-output-escaping="yes"/>
        </td>
      </xsl:for-each>
    </tr>
    </xsl:for-each>
    </table>
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

<!--<xsl:template match="subItemsName">
    <br/><span class="font1"><b><xsl:value-of select="."/></b></span><br/>
</xsl:template>

<xsl:template match="itemName">
    <span style='color:black;'><b><xsl:value-of select="."/></b></span>
</xsl:template>

<xsl:template match="itemValue">
  <xsl:value-of select="."/>
</xsl:template>

<xsl:template match="categoryName">
  &#160;&#160;&#160;&#160;
    <xsl:value-of select="."/><br/>
</xsl:template>

<xsl:template match="categoryValue">
    <xsl:value-of select="."/>
</xsl:template>

<xsl:template match="categoryNotes">
    <xsl:value-of select="."/>
</xsl:template>

<xsl:template match="itemRemNotes">
  &#160;&#160;&#160;&#160;&#160;
    <xsl:value-of select="."/><br/>
</xsl:template>

<xsl:template match="itemAdtlNotes">
  &#160;&#160;&#160;&#160;&#160;
    <xsl:value-of select="."/><br/>
</xsl:template>

<xsl:template match="itemNotes">
  &#160;&#160;&#160;&#160;&#160;
    <xsl:value-of select="."/><br/>
</xsl:template>-->

<xsl:template match="treatment">
<xsl:choose>
<xsl:when test="assessment != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Treatment"/></td></tr>
    <tr class="rightPaneData"><td>
      <table class="rightPaneData" cellspacing="0" cellpadding="0" width="100%">
        <xsl:for-each select="assessment">
          <tr><td>
	<xsl:if test="(position()!=1)">	<br/></xsl:if>
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
    <br/>&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;Reason:<xsl:value-of select="Reason" /><br/>
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

<xsl:template match="ProblemList">

  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="leftPaneHeading"><td><xsl:copy-of select="//return/ChartHeadings/ProblemList" /></td></tr>
    <tr class="leftPaneData"><td>
     <xsl:for-each select="Problem">
 		<table class="leftPaneData" cellspacing="0" cellpadding="0">
		  <tr>
			<td>
			   <xsl:for-each select="code">
			                <xsl:value-of select="."/>&#160;&#160;
			                <br/>
                           </xsl:for-each>

			 </td>
			 <td>
			   <xsl:for-each select="name">
			   	<xsl:value-of select="."/>
			   	<br/>
                           </xsl:for-each>
			 </td>
		   </tr>
		 </table>
     </xsl:for-each>
     </td></tr>
    <!--<tr height="15"><td></td></tr>-->
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
           <xsl:if test="($structDataDisplay ='1' or $treeview = '0') and position() != last()">
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
    <xsl:if test="count(category) != 0">
    <br/>
    </xsl:if><xsl:for-each select="notes">
      <xsl:value-of select="."/>
    </xsl:for-each>
    <xsl:for-each select="notesBR">
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
<xsl:variable name="structDataDisplay" select='structDataDisplay'/>
  <xsl:choose>
  <xsl:when test=". != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/PreventiveMedicine"/></td></tr>
    <tr class="rightPaneData"><td>
    <xsl:for-each select="category">
        <xsl:if test="count(item) != 0">
        <table class="rightPaneData" cellspacing="0" cellpadding="0">
          <tr>
            <td>
               <xsl:value-of select="categoryname"/>:&#160;
               <xsl:if test="$structDataDisplay = 1">
                <br/>&#160;&#160;&#160;&#160;
               </xsl:if>
               <xsl:for-each select="item">
                 <xsl:if test="$structDataDisplay = 1 and position() != 1">
                     <br/>&#160;&#160;&#160;&#160;
                 </xsl:if>
                 <xsl:value-of select="itemname"/>&#160;
                 <xsl:variable name="structFlag" select='struct'/>
                 <xsl:choose>
                 <xsl:when test="$structFlag = 1">
                 <xsl:if test="count(StData) != 0">
                     <xsl:for-each select="StData">
                        <xsl:if test="$structDataDisplay = 1">
                            <br/>&#160;&#160;&#160;&#160;&#160;&#160;&#160;
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
             </td>
           </tr>
         </table>
         </xsl:if>
     </xsl:for-each>
      <xsl:for-each select="itemValueBR">
        <table class="rightPaneData" cellspacing="0" cellpadding="0">
          <tr>
            <td>
               <xsl:value-of select="." disable-output-escaping="yes"/><br/>
             </td>
           </tr>
         </table>
     </xsl:for-each>
    </td></tr>
  </table>
  </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="disposition">
  <xsl:choose>
  <xsl:when test=". != ''">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Disposition"/></td></tr>
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
  <xsl:variable name="structDataDisplay" select='structDataDisplay'/>
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/PhysicalExamination"/></td></tr>
    <tr class="rightPaneData"><td>
    <xsl:for-each select="category">

	<xsl:if test="categoryName != ''">
	<u><xsl:value-of select="categoryName"/></u>:&#160;
	<br/>&#160;&#160;&#160;&#160;&#160;&#160;
      	<xsl:for-each select="categoryPhyExamDetail">
            <xsl:value-of select="itemName" />&#160;
         <xsl:variable name="structFlag2" select='struct'/>
            <xsl:if test="($structFlag2='0')">
                <xsl:attribute name="encid">
                    <xsl:value-of select="encounterId"/>
                </xsl:attribute>
                <xsl:attribute name="categoryId">
                    <xsl:value-of select="categoryId"/>
                </xsl:attribute>
                <xsl:attribute name="itemId">
                    <xsl:value-of select="itemId"/>
                </xsl:attribute>
                <xsl:value-of select="phyExamNotes" disable-output-escaping="yes"/>.&#160;
            </xsl:if>
            <xsl:if test="($structFlag2='1') ">
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
                  <br/>&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;
                  </xsl:when>
                  <xsl:when test="($structDataDisplay ='0' or $structDataDisplay ='2') and not(position()=1)">,&#160;</xsl:when>
                  </xsl:choose>
               <FONT SIZE="2"><xsl:value-of select="name"/></FONT>
               <FONT SIZE="2">
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
               </FONT>
               </xsl:for-each>
             </xsl:if>
              <xsl:if test="count(StData) = 0">
                      <xsl:value-of select="phyExamNotes" disable-output-escaping="yes"/>.&#160;
              </xsl:if>
               <xsl:if test="($structDataDisplay ='0' or $structDataDisplay ='2') and count(StData) != 0">.&#160;</xsl:if>
            </xsl:if>
            <xsl:if test="$structDataDisplay ='1' and position() != last()">
                         <br/>&#160;&#160;&#160;&#160;&#160;&#160;
            </xsl:if>
 	</xsl:for-each>
 	</xsl:if>
 	<br/>
    </xsl:for-each>

    	&#160;&#160;&#160;

	<xsl:for-each select="notes">
      		<xsl:value-of select="."/>
    	</xsl:for-each>
    	<xsl:for-each select="notesBR">
            <xsl:value-of select="." disable-output-escaping="yes"/>
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

<!--<xsl:template match="item">
   <table cellspacing="0" cellpadding="0" width="100%">
      <tr>
         <td width="10">&#160;</td>
         <td style="word-wrap:break-word">
         <xsl:apply-templates select="itemName"/>&#160;
         <xsl:apply-templates select="itemValue"/>

	      <xsl:for-each select="category">
            <xsl:apply-templates select="categoryValue"/>
	         <xsl:apply-templates select="categoryNotes"/>
         </xsl:for-each>

         <xsl:apply-templates select="itemRemNotes"/>
         <xsl:apply-templates select="itemNotes"/>
         <xsl:apply-templates select="itemAdtlNotes"/>
      </td>
    </tr>
   </table>
</xsl:template>-->

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

<xsl:template match="DispositionAndCommunication">
  <table cellspacing="0" cellpadding="0" width="100%">
     <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/DispositionAndCommunication"/></td></tr>
     <tr class="rightPaneData"><td><xsl:value-of select="item/itemValue"/></td></tr>
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
     <tr class="rightPaneData"><td><xsl:value-of select="." disable-output-escaping="yes" /></td></tr>
  </table>
</xsl:template>

<xsl:template match="vitals2BR">
  <table cellspacing="0" cellpadding="0" width="100%">
     <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Vitals"/></td></tr>
     <tr class="rightPaneData"><td><xsl:value-of select="." disable-output-escaping="yes" /></td></tr>
  </table>
</xsl:template>

<xsl:template match="vitalHistory">
	<table cellspacing="0" cellpadding="0" width="100%">
		<tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Vitals"/></td></tr>
		<tr class="rightPaneData">
			<td>
				<table cellpadding="0" cellspacing="0"  border="1" width="100%" >
		  		<xsl:for-each select="vital">
				  	<tr class="rightPaneData">
				  		<td colspan="3" align="center" bgcolor="F3F6FB" ><b><xsl:value-of select="vitalName"/></b></td>
				  	</tr>
				  	<tr class="rightPaneData">
				  		<td>
				  			<table border="0" width="100%">
				  				<xsl:for-each select="vitalData">
				  				<tr class="rightPaneData">
							  		<td  width="45%" align="left"><xsl:value-of select="vitalValue" disable-output-escaping="yes"/>&#160;&#160;</td>
							  		<td width="40%" align="center"><xsl:value-of select="updatedtime"/>&#160;&#160;</td>
							  		<td width="15%" align="center"><xsl:value-of select="ufname"/>&#160;<xsl:value-of select="ulname"/></td>
				  				</tr>
				  				</xsl:for-each>
				  			</table>
				  		</td>
				  	</tr>
				  
			  </xsl:for-each>
			  </table>	
		  </td>
	   </tr>
	   <tr class="rightPaneData"><td><xsl:value-of select="vitalHist2BR" disable-output-escaping="yes" /></td></tr>
	</table>
</xsl:template>

<xsl:template match="PastOrders">
  <table cellspacing="0" cellpadding="0" width="100%">
     <tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/PastOrders"/></td></tr>
     	<xsl:for-each select="PastOrder">
			<tr class="rightPaneData">
				<td colspan="2">
					<u><xsl:value-of select="OrderName"/></u>
				</td>
			</tr>
			<xsl:if test="OrderResult!=''">
				<tr class="rightPaneData">
					<td colspan="2">
						&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;Result: <xsl:value-of select="OrderResult"/>
					</td>
				</tr>
			</xsl:if>
			<xsl:for-each select="OrderDetail">
				<tr class="rightPaneData">
					<td width="50%" align="left">
					&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;<xsl:value-of select="OrderItemName"/>
					</td>
					<td width="20%" align="left">
						<xsl:value-of select="OrderItemValue"/>
					</td>
					<td width="30%" align="left">
						<xsl:value-of select="OrderItemRange"/>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:if test="OrderNotes!=''">
				<tr class="rightPaneData">
					<td colspan="2">
						<span name="OrderNotes">&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;Notes: <xsl:value-of select="OrderNotes"/></span>
					</td>
				</tr>
			</xsl:if>
		</xsl:for-each>
  </table>
</xsl:template>

<xsl:template match="VisionExamination">
  <table cellspacing="0" cellpadding="0" width="100%">
    <tr><td class="rightPaneHeading"><xsl:value-of select="//return/ChartHeadings/VisionExamination"/><br/> </td> </tr>
    	 <tr class="rightPaneData"> <td>
				<xsl:apply-templates select="UnaidedAcuities"/>
				<xsl:apply-templates select="AidedAcuities"/>
				<xsl:apply-templates select="KRS"/>
				<xsl:apply-templates select="PDs"/>
				<xsl:apply-templates select="KRSADD"/>
				<xsl:apply-templates select="AutoRefractions"/>
				<xsl:apply-templates select="Retinoscopys"/>
				<xsl:apply-templates select="Cycloplegics"/>
				<xsl:apply-templates select="IOPS"/>
				<xsl:apply-templates select="PACHS"/>
				<xsl:apply-templates select="CDS"/>
				<xsl:if test="((Manifests != '') or (PSRs != '') or (FSRs != ''))">
					&#160;&#160;&#160;&#160;&#160; <u> Spectacle </u> <br/>
					<xsl:apply-templates select="PSRs"/>
					<xsl:apply-templates select="Manifests"/>
					<xsl:apply-templates select="FSRs"/>
				</xsl:if>

				<xsl:if test="(PCRs != '')">
					&#160;&#160;&#160;&#160;&#160; <u> Contacts </u> <br/>
					<xsl:apply-templates select="PCRs"/>
				</xsl:if>
		<br/>
    </td></tr>
    <tr height="5"><td></td></tr>
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
    <tr class="rightPaneData"><td>
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

<xsl:if test="notes !=''">
<xsl:if test="procnotes2 !=''">&#160;<xsl:value-of select="procnotes2" disable-output-escaping="yes"/></xsl:if>
</xsl:if>.&#160;
	<xsl:if test="position() != last()">
           <br/>&#160;&#160;&#160;&#160;&#160;&#160;
    </xsl:if>

    </xsl:for-each>
       
    </xsl:for-each>
     <xsl:if test="count(category) != 0">
    <br/>
    </xsl:if>
    <xsl:for-each select="notes">
      <xsl:value-of select="."/>
    </xsl:for-each>
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
	<tr class="rightPaneHeading"><td><xsl:value-of select="//return/ChartHeadings/Assessment"/></td></tr>
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

<xsl:template match="UnaidedAcuities">
	<xsl:if test="AcuityData != ''">
		&#160;&#160;&#160;&#160;&#160;&#160;<u>Unaided Acuities</u> <br />
		<xsl:apply-templates select="AcuityData"/>
	</xsl:if>
</xsl:template>

<xsl:template match="AidedAcuities">
	<xsl:if test="AcuityData != ''">
		&#160;&#160;&#160;&#160;&#160;&#160;<u>Aided Acuities</u> <br />
		<xsl:apply-templates select="AcuityData"/>
	</xsl:if>
</xsl:template>

<xsl:template match="AcuityData">
	<TABLE width="100%">
		<TR>
			<TD width="10px">&#160;&#160;&#160;</TD>
			<TD>
				<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">
					<tr>
						<td width="2%" class="TitleColor">&#160;</td>
						<td width="5%" align="Center" class="TitleColor"><b>DVA</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>NVA</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>PH</b></td>
					</tr>
					<tr>
						<td width="2%" align="Center" class="TitleColor"><b>OD</b></td>
						<xsl:apply-templates select="DVAOD"/>
						<xsl:apply-templates select="NVAOD"/>
						<xsl:apply-templates select="PHOD"/>
					</tr>

					<tr>
						<td width="2%" align="Center" class="TitleColor"><b>OS</b></td>
						<xsl:apply-templates select="DVAOS"/>
						<xsl:apply-templates select="NVAOS"/>
						<xsl:apply-templates select="PHOS"/>
					</tr>

					<tr>
						<td width="2%" align="Center" class="TitleColor"><b>OU</b></td>
						<xsl:apply-templates select="DVAOU"/>
						<xsl:apply-templates select="NVAOU"/>
						<xsl:apply-templates select="PHOU"/>
					</tr>
				</table>
			</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
		</TR>
	</TABLE>
</xsl:template>

<xsl:template match="IOPS">
	<xsl:if test="IOP != ''">
		&#160;&#160;&#160;&#160;&#160;
		<u> IOP </u>
		<br />
		<TABLE cellspacing="0" cellpadding="0" width="100%">
			<TR>
				<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD>
					<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">
						<tr>
							<td width="2%" align="Center" class="TitleColor"><b>IOP</b></td>
							<td width="5%" align="Center" class="TitleColor"><b>OD</b></td>
							<td width="5%" align="Center" class="TitleColor"><b>OS</b></td>
							<td width="5%" align="Center" class="TitleColor"><b>Adj OD</b></td>
	                        <td width="5%" align="Center" class="TitleColor"><b>Adj OS</b></td>
							<td width="5%" align="Center" class="TitleColor"><b>Test</b></td>
							<td width="5%" align="Center" class="TitleColor"><b>Time</b></td>
							<td width="5%" align="Center" class="TitleColor"><b>Category</b></td>
						</tr>
						<xsl:for-each select="IOP">
							<tr>
								<td width="2%" align="Center" class="TitleColor"><xsl:value-of select="DISPLAYINDEX"/></td>
								<td width="5%" align="Center" class="tablecell"><xsl:value-of select="IOPOD"/></td>
								<td width="5%" align="Center" class="tablecell"><xsl:value-of select="IOPOS"/></td>
								<td width="5%" align="Center" class="tablecell"><xsl:value-of select="IOPAOD"/></td>
                                <td width="5%" align="Center" class="tablecell"><xsl:value-of select="IOPAOS"/></td>
								<td width="5%" align="Center" class="tablecell"><xsl:value-of select="IOPTEST"/></td>
								<td width="5%" align="Center" class="tablecell"><xsl:value-of select="IOPTIME"/></td>
								<td width="5%" align="Center" class="tablecell"><xsl:value-of select="IOPCAT"/></td>
							</tr>
						</xsl:for-each>
					</table>
				</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			</TR>
		</TABLE>
	</xsl:if>
</xsl:template>

<xsl:template match="AutoRefractions">
	<xsl:if test="ARCData != ''">
		&#160;&#160;&#160;&#160;&#160; <u> Auto Refraction </u> <br />
		<xsl:apply-templates select="ARCData"/>
	</xsl:if>
</xsl:template>

<xsl:template match="Retinoscopys">
	<xsl:if test="ARCData != ''">
		&#160;&#160;&#160;&#160;&#160; <u> Retinoscopy </u> <br/>
		<xsl:apply-templates select="ARCData"/>
	</xsl:if>
</xsl:template>

<xsl:template match="Cycloplegics">
	<xsl:if test="CylManData != ''">
		&#160;&#160;&#160;&#160;&#160; <u> Cycloplegic </u> <br/>
		<xsl:apply-templates select="CylManData"/>
	</xsl:if>
</xsl:template>

<xsl:template match="Manifests">
	<xsl:if test="CylManData != ''">
		&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;<FONT COLOR="black">Manifest</FONT><br/>
		<xsl:apply-templates select="CylManData"/>
	</xsl:if>
</xsl:template>

<xsl:template match="PSRs">
	<xsl:apply-templates select="PSRFSRData"/>
</xsl:template>

<xsl:template match="FSRs">
	<xsl:apply-templates select="PSRFSRData"/>
</xsl:template>

<!-- GENERIC TEMPLATE FOR AUTO REFRACTION, RETINOSCOPY -->
<xsl:template match="ARCData">
	<TABLE cellspacing="0" cellpadding="0"
		width="100%">
		<TR>
			<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD>
				<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">
					<tr>
						<td width="2%" align="Center" class="TitleColor">
							<b>&#160;</b>
						</td>
						<td width="5%" align="Center" class="TitleColor"><b>Sph</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>Cyl</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>Axis</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>Add</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>DVA</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>NVA</b></td>
					</tr>

					<tr>
						<td width="2%" align="Center" class="TitleColor"><b>OD</b></td>
						<xsl:apply-templates select="SPHOD"/>
						<xsl:apply-templates select="CYLOD"/>
						<xsl:apply-templates select="AXISOD"/>
						<xsl:apply-templates select="ADDOD"/>
						<xsl:apply-templates select="DVAOD"/>
						<xsl:apply-templates select="NVAOD"/>
					</tr>

					<tr>
						<td width="2%" align="Center" class="TitleColor"><b>OS</b></td>
						<xsl:apply-templates select="SPHOS"/>
						<xsl:apply-templates select="CYLOS"/>
						<xsl:apply-templates select="AXISOS"/>
						<xsl:apply-templates select="ADDOS"/>
						<xsl:apply-templates select="DVAOS"/>
						<xsl:apply-templates select="NVAOS"/>
					</tr>
				</table>
			</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
		</TR>
	</TABLE>
</xsl:template>

<!-- GENERIC TEMPLATE FOR Cycloplegic, Manifest -->
<xsl:template match="CylManData">
				<TABLE cellspacing="0" cellpadding="0" width="100%" >
				<TR><TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD >
					<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">
 					<tr>
						<td width="2%" align="Center" class="TitleColor">&#160;</td>
						<td width="5%" align="Center" class="TitleColor"><b>Sph</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>Cyl</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>Axis</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>H Prism</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>V Prism</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>Add</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>DVA</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>NVA</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>PH</b></td>
					</tr>

 					<tr>
						<td width="2%" align="Center" class="TitleColor"><b>OD</b></td>
	 					<xsl:apply-templates select="SPHOD"/>
	 					<xsl:apply-templates select="CYLOD"/>
	 					<xsl:apply-templates select="AXISOD"/>
	 					<xsl:apply-templates select="HPRISMOD"/>
	 					<xsl:apply-templates select="VPRISMOD"/>
	 					<xsl:apply-templates select="ADDOD"/>
	 					<xsl:apply-templates select="DVAOD"/>
	 					<xsl:apply-templates select="NVAOD"/>
	 					<xsl:apply-templates select="PHOD"/>
					</tr>

 					<tr>
						<td width="2%" align="Center" class="TitleColor"><b>OS</b></td>
 						<xsl:apply-templates select="SPHOS"/>
	 					<xsl:apply-templates select="CYLOS"/>
	 					<xsl:apply-templates select="AXISOS"/>
	 					<xsl:apply-templates select="HPRISMOS"/>
	 					<xsl:apply-templates select="VPRISMOS"/>
	 					<xsl:apply-templates select="ADDOS"/>
	 					<xsl:apply-templates select="DVAOS"/>
	 					<xsl:apply-templates select="NVAOS"/>
	 					<xsl:apply-templates select="PHOS"/>
					</tr>
 				</table>
				</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				</TR>
				<xsl:if test="SFittingComments != ''">
					<TR>
						<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
						<TD class="tablecell"><xsl:apply-templates select="SFittingComments"/></TD>
						<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
					</TR>
				</xsl:if>
				</TABLE>
</xsl:template>

<xsl:template match="PSRFSRData">
	<xsl:apply-templates select="ExamTabName"/>
	<TABLE cellspacing="0" cellpadding="0" width="100%">
		<TR>
			<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD>
					<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">

					<tr>
						<td width="2%" align="Center" class="TitleColor">&#160;</td>
						<td width="5%" align="Center" class="TitleColor"><b>Sph</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>Cyl</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>Axis</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>H Prism</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>V Prism</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>Add</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>DVA</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>NVA</b></td>
						<td width="5%" align="Center" class="TitleColor"><b>PH</b></td>
					</tr>

					<tr>
						<td width="2%" align="Center" class="TitleColor"><b>OD</b></td>
						<xsl:apply-templates select="SPHOD"/>
						<xsl:apply-templates select="CYLOD"/>
						<xsl:apply-templates select="AXISOD"/>
						<xsl:apply-templates select="HPRISMOD"/>
						<xsl:apply-templates select="VPRISMOD"/>
						<xsl:apply-templates select="ADDOD"/>
						<xsl:apply-templates select="DVAOD"/>
						<xsl:apply-templates select="NVAOD"/>
						<xsl:apply-templates select="PHOD"/>
					</tr>

					<tr>
						<td width="2%" align="Center" class="TitleColor"><b>OS</b></td>
						<xsl:apply-templates select="SPHOS"/>
						<xsl:apply-templates select="CYLOS"/>
						<xsl:apply-templates select="AXISOS"/>
						<xsl:apply-templates select="HPRISMOS"/>
						<xsl:apply-templates select="VPRISMOS"/>
						<xsl:apply-templates select="ADDOS"/>
						<xsl:apply-templates select="DVAOS"/>
						<xsl:apply-templates select="NVAOS"/>
						<xsl:apply-templates select="PHOS"/>
					</tr>

					<tr>
						<td width="2%" align="Center" class="TitleColor"><b>OU</b></td>
						<td width="5%" align="left"></td>
						<td width="5%" align="left"></td>
						<td width="5%" align="left"></td>
						<td width="5%" align="left"></td>
						<td width="5%" align="left"></td>
						<td width="5%" align="left"></td>
						<xsl:apply-templates select="DVAOU"/>
						<xsl:apply-templates select="NVAOU"/>
						<xsl:apply-templates select="PHOU"/>
					</tr>
				</table>
			</TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
		</TR>
		<xsl:if test="SFittingComments != ''">
			<TR>
				<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD class="tablecell"><xsl:apply-templates select="SFittingComments"/></TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			</TR>
		</xsl:if>
	</TABLE>
</xsl:template>

<xsl:template match="PCRs">
	<xsl:apply-templates select="PCR"/>
</xsl:template>

<xsl:template match="PCR">
	<xsl:apply-templates select="PCRData"/>
	<xsl:apply-templates select="TrialRx"/>
	<xsl:apply-templates select="ORx"/>
	<xsl:apply-templates select="FinalContactRx"/>
</xsl:template>

<xsl:template match="PCRData">
<xsl:apply-templates select="ExamTabName"/>
<TABLE cellspacing="0" cellpadding="0"
	width="100%">
	<TR>
		<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
		<TD>
			<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">

				<tr>
					<td width="2%" align="Center" class="TitleColor">&#160;</td>
					<td width="10%" align="center" class="TitleColor"><b>Product Name</b></td>
					<td width="2%" align="Center" class="TitleColor"><b>BC</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Dia</b></td>
					<td width="2%" align="Center" class="TitleColor"><b>Sph</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Cyl</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Axis</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Add</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>DVA</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>NVA</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Tint</b></td>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OD</b></td>
					<xsl:apply-templates select="PRODUCTNAMEOD"/>
					<xsl:apply-templates select="BCOD"/>
					<xsl:apply-templates select="DIAOD"/>
					<xsl:apply-templates select="SPHOD"/>
					<xsl:apply-templates select="CYLOD"/>
					<xsl:apply-templates select="AXISOD"/>
					<xsl:apply-templates select="ADDOD"/>
					<xsl:apply-templates select="DVAOD"/>
					<xsl:apply-templates select="NVAOD"/>
					<xsl:apply-templates select="TINTOD"/>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OS</b></td>
					<xsl:apply-templates select="PRODUCTNAMEOS"/>
					<xsl:apply-templates select="BCOS"/>
					<xsl:apply-templates select="DIAOS"/>
					<xsl:apply-templates select="SPHOS"/>
					<xsl:apply-templates select="CYLOS"/>
					<xsl:apply-templates select="AXISOS"/>
					<xsl:apply-templates select="ADDOS"/>
					<xsl:apply-templates select="DVAOS"/>
					<xsl:apply-templates select="NVAOS"/>
					<xsl:apply-templates select="TINTOS"/>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OU</b></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<xsl:apply-templates select="DVAOU"/>
					<xsl:apply-templates select="NVAOU"/>
					<td width="5%" align="Center"></td>
				</tr>

			</table>
		</TD>
		<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
	</TR>
	<xsl:if test="CFittingComments != ''">
		<TR>
			<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD class="tablecell"><xsl:apply-templates select="CFittingComments"/></TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
		</TR>
	</xsl:if>
</TABLE>
</xsl:template>

<xsl:template match="TrialRx">
<xsl:apply-templates select="ExamTabName"/>
<TABLE cellspacing="0" cellpadding="0" width="100%">
	<TR>
		<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
		<TD>
			<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">

				<tr>
					<td width="2%" align="Center" class="TitleColor">&#160;</td>
					<td width="10%" align="Center" class="TitleColor"><b>Product Name</b></td>
					<td width="2%" align="Center" class="TitleColor"><b>BC</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Dia</b></td>
					<td width="2%" align="Center" class="TitleColor"><b>Sph</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Cyl</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Axis</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Add</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>DVA</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>NVA</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Tint</b></td>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OD</b></td>
					<xsl:apply-templates select="PRODUCTNAMEOD"/>
					<xsl:apply-templates select="BCOD"/>
					<xsl:apply-templates select="DIAOD"/>
					<xsl:apply-templates select="SPHOD"/>
					<xsl:apply-templates select="CYLOD"/>
					<xsl:apply-templates select="AXISOD"/>
					<xsl:apply-templates select="ADDOD"/>
					<xsl:apply-templates select="DVAOD"/>
					<xsl:apply-templates select="NVAOD"/>
					<xsl:apply-templates select="TINTOD"/>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OS</b></td>
					<xsl:apply-templates select="PRODUCTNAMEOS"/>
					<xsl:apply-templates select="BCOS"/>
					<xsl:apply-templates select="DIAOS"/>
					<xsl:apply-templates select="SPHOS"/>
					<xsl:apply-templates select="CYLOS"/>
					<xsl:apply-templates select="AXISOS"/>
					<xsl:apply-templates select="ADDOS"/>
					<xsl:apply-templates select="DVAOS"/>
					<xsl:apply-templates select="NVAOS"/>
					<xsl:apply-templates select="TINTOS"/>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OU</b></td>
					<td width="5%" align="Center"></td>
					<td width="5%" align="Center"></td>
					<td width="5%" align="Center"></td>
					<td width="5%" align="Center"></td>
					<td width="5%" align="Center"></td>
					<td width="5%" align="Center"></td>
					<td width="5%" align="Center"></td>
					<xsl:apply-templates select="DVAOU"/>
					<xsl:apply-templates select="NVAOU"/>
					<td width="5%" align="Center"></td>
				</tr>

			</table>
		</TD>
		<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
	</TR>
	<xsl:if test="CFittingComments != ''">
		<TR>
			<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD class="tablecell"><xsl:apply-templates select="CFittingComments"/></TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
		</TR>
	</xsl:if>
</TABLE>
</xsl:template>

<xsl:template match="ORx">
<xsl:apply-templates select="ExamTabName"/>
<TABLE cellspacing="0" cellpadding="0" width="100%">
	<TR>
		<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
		<TD>
			<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">
				<tr>
					<td width="2%" align="Center" class="TitleColor">&#160;</td>
					<td width="5%" align="Center" class="TitleColor"><b>Sph</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Cyl</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Axis</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Add</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>DVA</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>NVA</b></td>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OD</b></td>
					<xsl:apply-templates select="SPHOD"/>
					<xsl:apply-templates select="CYLOD"/>
					<xsl:apply-templates select="AXISOD"/>
					<xsl:apply-templates select="ADDOD"/>
					<xsl:apply-templates select="DVAOD"/>
					<xsl:apply-templates select="NVAOD"/>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OS</b></td>
					<xsl:apply-templates select="SPHOS"/>
					<xsl:apply-templates select="CYLOS"/>
					<xsl:apply-templates select="AXISOS"/>
					<xsl:apply-templates select="ADDOS"/>
					<xsl:apply-templates select="DVAOS"/>
					<xsl:apply-templates select="NVAOS"/>
				</tr>

			</table>
		</TD>
		<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
		<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
		<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
		<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
	</TR>
</TABLE>
</xsl:template>

<xsl:template match="FinalContactRx">
<xsl:apply-templates select="ExamTabName"/>
<TABLE cellspacing="0" cellpadding="0"
	width="100%">
	<TR>
		<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
		<TD>
			<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">

				<tr>
					<td width="2%" align="Center" class="TitleColor">&#160;</td>
					<td width="10%" align="Center" class="TitleColor"><b>Product Name</b></td>
					<td width="2%" align="Center" class="TitleColor"><b>BC</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Dia</b></td>
					<td width="2%" align="Center" class="TitleColor"><b>Sph</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Cyl</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Axis</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Add</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>DVA</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>NVA</b></td>
					<td width="5%" align="Center" class="TitleColor"><b>Tint</b></td>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OD</b></td>
					<xsl:apply-templates select="PRODUCTNAMEOD"/>
					<xsl:apply-templates select="BCOD"/>
					<xsl:apply-templates select="DIAOD"/>
					<xsl:apply-templates select="SPHOD"/>
					<xsl:apply-templates select="CYLOD"/>
					<xsl:apply-templates select="AXISOD"/>
					<xsl:apply-templates select="ADDOD"/>
					<xsl:apply-templates select="DVAOD"/>
					<xsl:apply-templates select="NVAOD"/>
					<xsl:apply-templates select="TINTOD"/>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OS</b></td>
					<xsl:apply-templates select="PRODUCTNAMEOS"/>
					<xsl:apply-templates select="BCOS"/>
					<xsl:apply-templates select="DIAOS"/>
					<xsl:apply-templates select="SPHOS"/>
					<xsl:apply-templates select="CYLOS"/>
					<xsl:apply-templates select="AXISOS"/>
					<xsl:apply-templates select="ADDOS"/>
					<xsl:apply-templates select="DVAOS"/>
					<xsl:apply-templates select="NVAOS"/>
					<xsl:apply-templates select="TINTOS"/>
				</tr>

				<tr>
					<td width="2%" align="Center" class="TitleColor"><b>OU</b></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<td width="5%" align="left"></td>
					<xsl:apply-templates select="DVAOU"/>
					<xsl:apply-templates select="NVAOU"/>
					<td width="5%" align="left"></td>
				</tr>

			</table>
		</TD>
		<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
	</TR>
	<xsl:if test="CFittingComments != ''">
		<TR>
			<TD width="10px">&#160;&#160;&#160;&#160;&#160;</TD>
			<TD class="tablecell"><xsl:apply-templates select="CFittingComments"/></TD>
			<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
		</TR>
	</xsl:if>
</TABLE>
</xsl:template>

<xsl:template match="ExamTabName">
	&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;
	<FONT COLOR="black">
		<xsl:value-of select="." />
	</FONT>
	<br />
</xsl:template>
<!-- GENERIC TEMPLATE FOR OD -->
<xsl:template match="SPHOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="CYLOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="AXISOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="HPRISMOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="VPRISMOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="ADDOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="DVAOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="NVAOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="PHOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="BCOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="DIAOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="TINTOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="PRODUCTNAMEOD">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<!-- GENERIC TEMPLATE FOR OS -->
<xsl:template match="SPHOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="CYLOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="AXISOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="HPRISMOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="VPRISMOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="ADDOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="DVAOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="NVAOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="PHOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="BCOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="DIAOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="TINTOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="PRODUCTNAMEOS">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<!-- GENERIC TEMPLATE FOR OU -->
<xsl:template match="SPHOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="CYLOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="AXISOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="HPRISMOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="VPRISMOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="ADDOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="DVAOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="NVAOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="PHOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="BCOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="DIAOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="TINTOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>
<xsl:template match="PRODUCTNAMEOU">
	<td width="5%" align="Center" class="tablecell">
		<xsl:value-of select="." />
	</td>
</xsl:template>

<!-- Generic Template for fitting comments for spectacle and contacts -->
<xsl:template match="SFittingComments">
	<xsl:if test="Comments != ''">
		<b>Comments: </b><xsl:apply-templates select="Comments"/>
	</xsl:if>
</xsl:template>

<xsl:template match="CFittingComments">
	<table border="0" width="100%">
		<xsl:if test="Centration_Movement != ''">
			<tr><td class="tablecell"><b>Centration Movement: </b><xsl:apply-templates select="Centration_Movement"/></td></tr>
		</xsl:if>
		<xsl:if test="Toric_Orientation != ''">
			<tr><td class="tablecell"><b>Toric Orientation: </b><xsl:apply-templates select="Toric_Orientation"/></td></tr>
		</xsl:if>
		<xsl:if test="Nafi_Pattern != ''">
			<tr><td class="tablecell"><b>Nafi Pattern: </b><xsl:apply-templates select="Nafi_Pattern"/></td></tr>
		</xsl:if>
		<xsl:if test="Comments != ''">
			<tr><td class="tablecell"><b>Comments: </b><xsl:apply-templates select="Comments"/></td></tr>
		</xsl:if>
	</table>
</xsl:template>

<!-- Template for K-Readings -->
<xsl:template match="KRS">
	<xsl:for-each select="KR">
		&#160;&#160;&#160;&#160;&#160;
		<u>K-Readings</u>
		<br />
		<TABLE width="100%">
			<xsl:if test="DiopterMM != ''">
				<TR>
					<TD width="10px">&#160;&#160;&#160;</TD>
					<TD class="tablecell"><xsl:apply-templates select="DiopterMM"/></TD>
					<TD width="50px">&#160;&#160;&#160;</TD>
				</TR>
			</xsl:if>
			<TR>
				<TD width="10px">&#160;&#160;&#160;</TD>
				<TD>
					<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">
						<tr>
							<td width="2%" class="TitleColor">&#160;</td>
							<td width="5%" align="Center" class="TitleColor" colspan="2"><b>Flat K</b></td>
							<td width="5%" align="Center" class="TitleColor" colspan="2"><b>Steep K</b></td>
						</tr>
						<tr>
							<td width="2%" align="Center" class="TitleColor"><b></b></td>
							<td width="5%" align="Center" class="tablecell"><xsl:value-of select="FlatK1OD" /></td>
							<td width="5%" align="Center" class="tablecell"><xsl:value-of select="FlatK2OD" /></td>
							<td width="5%" align="Center" class="tablecell"><xsl:value-of select="SteepK1OD" /></td>
							<td width="5%" align="Center" class="tablecell"><xsl:value-of select="SteepK2OD" /></td>
						</tr>

						<tr>
							<td width="2%" align="Center" class="TitleColor"><b></b></td>
							<td width="5%" align="Center" class="tablecell"><xsl:value-of select="FlatK1OS" /></td>
							<td width="5%" align="Center" class="tablecell"><xsl:value-of select="FlatK2OS" /></td>
							<td width="5%" align="Center" class="tablecell"><xsl:value-of select="SteepK1OS" /></td>
							<td width="5%" align="Center" class="tablecell"><xsl:value-of select="SteepK2OS" /></td>
						</tr>

					</table>
				</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			</TR>
		</TABLE>
	</xsl:for-each>
</xsl:template>

<!-- Template for PD (Dist/Near) -->
<xsl:template match="PDs">
	<xsl:for-each select="PD">
		&#160;&#160;&#160;&#160;&#160;
		<u>PD</u>
		<br />
		<TABLE width="100%">
			<TR>
				<TD width="10px">&#160;&#160;&#160;</TD>
				<TD>
					<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">
						<tr>
							<td width="2%" class="TitleColor">&#160;</td>
							<td width="5%" align="Center" class="TitleColor"><b>Dist</b></td>
							<td width="5%" align="Center" class="TitleColor"><b>Near</b></td>
						</tr>
						<tr>
							<td width="2%" align="Center" class="TitleColor"><b>OU</b></td>
							<xsl:apply-templates select="DVAOU"/>
							<xsl:apply-templates select="NVAOU"/>
						</tr>

						<tr>
							<td width="2%" align="Center" class="TitleColor"><b>OD</b></td>
							<xsl:apply-templates select="DVAOD"/>
							<xsl:apply-templates select="NVAOD"/>
						</tr>

						<tr>
							<td width="2%" align="Center" class="TitleColor"><b>OS</b></td>
							<xsl:apply-templates select="DVAOS"/>
							<xsl:apply-templates select="NVAOS"/>
						</tr>
					</table>
				</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			</TR>
		</TABLE>
	</xsl:for-each>
</xsl:template>

<!-- Vertex Dist, Spec BC, Work Dist -->
<xsl:template match="KRSADD">
	<xsl:for-each select="KRADD">
		<xsl:if test="VertexDist != ''">
			&#160;&#160;&#160;&#160;&#160;&#160;<u>Vertex Dist</u>
			<br />
			<table border="0" class="tablecell" width="100%">
				<tr>
					<TD width="10px">&#160;&#160;&#160;&#160;&#160;&#160;&#160;</TD>
					<TD class="tablecell"><xsl:apply-templates select="VertexDist"/> mm</TD>
					<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				</tr>
			</table>
		</xsl:if>
		<xsl:if test="SpecBC != ''">
			&#160;&#160;&#160;&#160;&#160;&#160;<u>Spec BC</u>
			<br />
			<table border="0" class="tablecell" width="100%">
				<tr>
					<TD width="10px">&#160;&#160;&#160;&#160;&#160;&#160;&#160;</TD>
					<TD class="tablecell"><xsl:apply-templates select="SpecBC"/></TD>
					<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				</tr>
			</table>
		</xsl:if>
		<xsl:if test="WorkDist != ''">
			&#160;&#160;&#160;&#160;&#160;&#160;<u>Work Dist</u>
			<br />
			<table border="0" class="tablecell" width="100%">
				<tr>
					<TD width="10px">&#160;&#160;&#160;&#160;&#160;&#160;&#160;</TD>
					<TD class="tablecell"><xsl:apply-templates select="WorkDist"/> in</TD>
					<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				</tr>
			</table>
		</xsl:if>
	</xsl:for-each>
</xsl:template>

<!-- Template for pachymetry -->
<xsl:template match="PACHS">
	<xsl:for-each select="PACH">
		&#160;&#160;&#160;&#160;&#160;
		<u>Pachymetry</u>
		<br />
		<TABLE width="100%">
			<TR>
				<TD width="10px">&#160;&#160;&#160;</TD>
				<TD>
					<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">
						<tr>
							<td width="2%" class="TitleColor">&#160;</td>
							<td width="5%" align="Center" class="TitleColor"><b>OD</b></td>
							<td width="5%" align="Center" class="TitleColor"><b>OS</b></td>
						</tr>
						<tr>
							<td width="2%" align="Center" class="TitleColor"><b></b></td>
							<td class="tablecell" align="Center"><xsl:apply-templates select="ODHORZ"/></td>
							<td class="tablecell" align="Center"><xsl:apply-templates select="OSHORZ"/></td>
						</tr>
					</table>
				</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			</TR>
		</TABLE>
	</xsl:for-each>
</xsl:template>

<!-- Template for pachymetry -->
<xsl:template match="CDS">
	<xsl:for-each select="CD">
		&#160;&#160;&#160;&#160;&#160;
		<u>CD Ratio</u>
		<br />
		<TABLE width="100%">
			<TR>
				<TD width="10px">&#160;&#160;&#160;</TD>
				<TD>
					<table cellSpacing="0" cellPadding="1" border="1" bordercolorlight="#9CB8EB" bordercolordark="#9CB8EB" style="border-collapse:collapse">
						<tr>
							<td width="2%" class="TitleColor">&#160;</td>
							<td width="5%" align="Center" class="TitleColor"><b>Horz</b></td>
							<td width="5%" align="Center" class="TitleColor"><b>Vert</b></td>
						</tr>
						<tr>
							<td width="2%" align="Center" class="TitleColor"><b>OD</b></td>
							<td class="tablecell" align="Center"><xsl:apply-templates select="ODHORZ"/></td>
							<td class="tablecell" align="Center"><xsl:apply-templates select="ODVERT"/></td>
						</tr>
						<tr>
							<td width="2%" align="Center" class="TitleColor"><b>OS</b></td>
							<td class="tablecell" align="Center"><xsl:apply-templates select="OSHORZ"/></td>
							<td class="tablecell" align="Center"><xsl:apply-templates select="OSVERT"/></td>
						</tr>
					</table>
				</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
				<TD width="50px">&#160;&#160;&#160;&#160;&#160;</TD>
			</TR>
		</TABLE>
	</xsl:for-each>
</xsl:template>

<!-- Template for CarePlan-->
<xsl:template match="CarePlanProblem">
	<table style="width:100%" cellspacing="0" cellpadding="0">
	<tr>
  		<td style="FONT-SIZE:2pt" WIDTH="4%"> </td> 
		<td colspan="2"><FONT  SIZE="2">Problem:<xsl:apply-templates select="CarePlanProblemName"/></FONT></td> 
	</tr>
	<tr>
	  		<td style="FONT-SIZE:2pt" WIDTH="8%"> </td> 
	  		<td><FONT  SIZE="2">Goal:</FONT><FONT COLOR="black" SIZE="2"><xsl:apply-templates select="CarePlanGoal"/></FONT>
	  		<FONT  SIZE="2"> Objective:</FONT><FONT COLOR="black" SIZE="2"><xsl:apply-templates select="CarePlanObjective"/></FONT>
	  		</td> 
			
	</tr>
	<tr>
			<td style="FONT-SIZE:2pt" WIDTH="8%"> </td> 
			<td><FONT COLOR="black" SIZE="2">Notes:<xsl:apply-templates select="CarePlanNotes"/></FONT>
			</td> 

	</tr>

	</table>
	<BR/>
</xsl:template>

</xsl:stylesheet>
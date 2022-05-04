package com.apixio.model.owl.interfaces;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.donotimplement.FeeForServiceInsuranceClaimType;

import java.util.Collection;

/**
 OWL annotation driven imports 
**/


/**
 * 
 * <p>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: FeeForServiceInsuranceClaim <br>
 * @version generated on Fri Mar 11 09:53:02 PST 2016 by jctoledo
 */

public interface FeeForServiceInsuranceClaim  extends InsuranceClaim {

/**
 * OWL annotation driven methods
 **/
    /**
     * source encounter
     */
    XUUID getSourceEncounterId();
    void setSourceEncounterId(XUUID anID);
    /**
     * Gets all property values for the AddIndicator property.<p>
     *
     *
     * @returns a collection of values for the AddIndicator property.
     */
    Boolean getAddIndicator();


    /**
     * Adds a AddIndicator property value.<p>
     *
     *
     * @param newAddIndicator the AddIndicator property value to be added
     */
    void setAddIndicator(Boolean newAddIndicator);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000215
     */

    /**
     * Gets all property values for the DeleteIndicator property.<p>
     *
     *
     * @returns a collection of values for the DeleteIndicator property.
     */
    Boolean getDeleteIndicator();


    /**
     * Adds a DeleteIndicator property value.<p>
     *
     *
     * @param newDeleteIndicator the DeleteIndicator property value to be added
     */
    void setDeleteIndicator(Boolean newDeleteIndicator);

    /**
     * Gets all property values for the RenderingProviders property.<p>
     *
     * @returns a collection of values for the RenderingProviders property.
     * Collection<? extends MedicalProfessional> getRenderingProviders();
     *
     */
    Collection<MedicalProfessional> getRenderingProviders();

    /**
     * Add an element to the list
     *
     */
    Collection<MedicalProfessional> addRenderingProviders(MedicalProfessional anItem);

    /**
     * get element from list by Id
     *
     */
    MedicalProfessional getRenderingProvidersById(XUUID anID);

    /**
     * Adds a RenderingProviders property value.<p>
     *
     * @param newRenderingProviders the RenderingProviders property value to be added
     * void setRenderingProviders(Collection<? extends MedicalProfessional> newRenderingProviders);
     *
     */
    void setRenderingProviders(Collection<MedicalProfessional> newRenderingProviders);


    /**
     * Gets all property values for the BillingProviders property.<p>
     *
     * @returns a collection of values for the BillingProviders property.
     * Collection<? extends MedicalProfessional> getBillingProviders();
     *
     */
    Collection<MedicalProfessional> getBillingProviders();

    /**
     * Add an element to the list
     *
     */
    Collection<MedicalProfessional> addBillingProviders(MedicalProfessional anItem);

    /**
     * get element from list by Id
     *
     */
    MedicalProfessional getBillingProvidersById(XUUID anID);

    /**
     * Adds a BillingProviders property value.<p>
     *
     * @param newBillingProviders the BillingProviders property value to be added
     * void setBillingProviders(Collection<? extends MedicalProfessional> newBillingProviders);
     *
     */
    void setBillingProviders(Collection<MedicalProfessional> newBillingProviders);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000161
     */

    /**
     * Gets all property values for the ServicesRendereds property.<p>
     *
     * @returns a collection of values for the ServicesRendereds property.
     * Collection<? extends ClinicalCode> getServicesRendereds();
     *
     */
    Collection<ClinicalCode> getServicesRendereds();

    /**
     * Add an element to the list
     *
     */
    Collection<ClinicalCode> addServicesRendereds(ClinicalCode anItem);

    /**
     * get element from list by Id
     *
     */
    ClinicalCode getServicesRenderedsById(XUUID anID);

    /**
     * Adds a ServicesRendereds property value.<p>
     *
     * @param newServicesRendereds the ServicesRendereds property value to be added
     * void setServicesRendereds(Collection<? extends ClinicalCode> newServicesRendereds);
     *
     */
    void setServicesRendereds(Collection<ClinicalCode> newServicesRendereds);
    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000162
     */

    /**
     * Gets all property values for the DateOfServiceRange property.<p>
     *
     *
     * @returns a collection of values for the DateOfServiceRange property.
     */
    DateRange getDateOfServiceRange();


    /**
     * Adds a DateOfServiceRange property value.<p>
     *
     *
     * @param newDateOfServiceRange the DateOfServiceRange property value to be added
     */
    void setDateOfServiceRange(DateRange newDateOfServiceRange);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000213
     */

    /**
     * Gets all property values for the DiagnosisCodes property.<p>
     *
     * @returns a collection of values for the DiagnosisCodes property.
     * Collection<? extends ClinicalCode> getDiagnosisCodes();
     * ICD codes associated with a RAPS diagnosis cluster
     *
     */
    Collection<ClinicalCode> getDiagnosisCodes();

    /**
     * Add an element to the list
     * ICD codes associated with a RAPS diagnosis cluster
     *
     */
    Collection<ClinicalCode> addDiagnosisCodes(ClinicalCode anItem);

    /**
     * get element from list by Id
     * ICD codes associated with a RAPS diagnosis cluster
     *
     */
    ClinicalCode getDiagnosisCodesById(XUUID anID);

    /**
     * Adds a DiagnosisCodes property value.<p>
     *
     * @param newDiagnosisCodes the DiagnosisCodes property value to be added
     * void setDiagnosisCodes(Collection<? extends ClinicalCode> newDiagnosisCodes);
     * ICD codes associated with a RAPS diagnosis cluster
     *
     */
    void setDiagnosisCodes(Collection<ClinicalCode> newDiagnosisCodes);
    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000214
     */

    /**
     * Gets all property values for the FeeForServiceClaimTypeValue property.<p>
     *
     *
     * @returns a collection of values for the FeeForServiceClaimTypeValue property.
     */
    FeeForServiceInsuranceClaimType getFeeForServiceClaimTypeValue();


    /**
     * Adds a FeeForServiceClaimTypeValue property value.<p>
     *
     *
     * @param newFeeForServiceClaimTypeValue the FeeForServiceClaimTypeValue property value to be added
     */
    void setFeeForServiceClaimTypeValue(FeeForServiceInsuranceClaimType newFeeForServiceClaimTypeValue);

    /**
     * Gets all property values for the ProviderType property.<p>
     *
     *
     * @returns a collection of values for the ProviderType property.
     */
    ClinicalCode getProviderType();


    /**
     * Adds a ProviderType property value.<p>
     *
     *
     * @param newProviderType the ProviderType property value to be added
     */
    void setProviderType(ClinicalCode newProviderType);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000216
     */

    /**
     * Gets all property values for the BillType property.<p>
     *
     *
     * @returns a collection of values for the BillType property.
     */
    ClinicalCode getBillType();


    /**
     * Adds a BillType property value.<p>
     *
     *
     * @param newBillType the BillType property value to be added
     */
    void setBillType(ClinicalCode newBillType);



}

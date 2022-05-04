package com.apixio.model.owl.interfaces;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.donotimplement.ChemicalEntity;
import com.apixio.model.owl.interfaces.donotimplement.CodedEntity;

import java.util.Collection;

/**
 * OWL annotation driven imports
 **/


/**
 * <p/>
 * Generated by Apixio's mod of Protege (http://protege.stanford.edu). <br>
 * Source Class: Medication <br>
 *
 * @version generated on Fri Mar 11 09:53:01 PST 2016 by jctoledo
 */

public interface Medication extends CodedEntity, ChemicalEntity {

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000168
     */

    /**
     * Gets all property values for the BrandName property.<p>
     *
     * @returns a collection of values for the BrandName property.
     */
    String getBrandName();


    /**
     * Adds a BrandName property value.<p>
     *
     * @param newBrandName the BrandName property value to be added
     */
    void setBrandName(String newBrandName);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000176
     */

    /**
     * Gets all property values for the GenericName property.<p>
     *
     * @returns a collection of values for the GenericName property.
     */
    String getGenericName();


    /**
     * Adds a GenericName property value.<p>
     *
     * @param newGenericName the GenericName property value to be added
     */
    void setGenericName(String newGenericName);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000436
     */

    /**
     * Gets all property values for the MedicationDosage property.<p>
     *
     * @returns a collection of values for the MedicationDosage property.
     */
    String getMedicationDosage();


    /**
     * Adds a MedicationDosage property value.<p>
     *
     * @param newMedicationDosage the MedicationDosage property value to be added
     */
    void setMedicationDosage(String newMedicationDosage);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000437
     */

    /**
     * Gets all property values for the MedicationAmount property.<p>
     *
     * @returns a collection of values for the MedicationAmount property.
     */
    String getMedicationAmount();


    /**
     * Adds a MedicationAmount property value.<p>
     *
     * @param newMedicationAmount the MedicationAmount property value to be added
     */
    void setMedicationAmount(String newMedicationAmount);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000445
     */

    /**
     * Gets all property values for the DrugStrength property.<p>
     *
     * @returns a collection of values for the DrugStrength property.
     */
    String getDrugStrength();


    /**
     * Adds a DrugStrength property value.<p>
     *
     * @param newDrugStrength the DrugStrength property value to be added
     */
    void setDrugStrength(String newDrugStrength);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000446
     */

    /**
     * Gets all property values for the DrugForm property.<p>
     *
     * @returns a collection of values for the DrugForm property.
     */
    String getDrugForm();


    /**
     * Adds a DrugForm property value.<p>
     *
     * @param newDrugForm the DrugForm property value to be added
     */
    void setDrugForm(String newDrugForm);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000447
     */

    /**
     * Gets all property values for the DrugUnits property.<p>
     *
     * @returns a collection of values for the DrugUnits property.
     */
    String getDrugUnits();


    /**
     * Adds a DrugUnits property value.<p>
     *
     * @param newDrugUnits the DrugUnits property value to be added
     */
    void setDrugUnits(String newDrugUnits);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000448
     */

    /**
     * Gets all property values for the RouteOfAdministration property.<p>
     *
     * @returns a collection of values for the RouteOfAdministration property.
     */
    String getRouteOfAdministration();


    /**
     * Adds a RouteOfAdministration property value.<p>
     *
     * @param newRouteOfAdministration the RouteOfAdministration property value to be added
     */
    void setRouteOfAdministration(String newRouteOfAdministration);

    /* ***************************************************
     * Property http://apixio.com/ontology/alo#000449
     */

    /**
     * Gets all property values for the DrugIngredients property.<p>
     *
     * @returns a collection of values for the DrugIngredients property.
     * Collection<? extends String> getDrugIngredients();
     */
    Collection<String> getDrugIngredients();

    /**
     * Add an element to the list
     */
    Collection<String> addDrugIngredients(String anItem);

    /**
     * get element from list by Id
     */
    String getDrugIngredientsById(XUUID anID);

    /**
     * Adds a DrugIngredients property value.<p>
     *
     * @param newDrugIngredients the DrugIngredients property value to be added
     * void setDrugIngredients(Collection<? extends String> newDrugIngredients);
     */
    void setDrugIngredients(Collection<String> newDrugIngredients);


}

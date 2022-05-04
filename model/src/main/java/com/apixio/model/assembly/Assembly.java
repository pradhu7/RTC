package com.apixio.model.assembly;

import java.util.List;

/**
 * Assembly is the bridge between generic AssemblyLogic code and the actual assembly
 * instances (e.g., instance of Patients or Events).  There is one instance of the
 * implementation object for all instances of assemblies (e.g., only one instance of,
 * say, PatientAssembly, that handles all actual Patient instances).
 */
public interface Assembly<A,P>
{
    /**
     * Given an assembly object, return its never-changing ID (e.g., for a Patient the
     * patient's UUID should be returned).
     */
    public String getID(A assemblyObject);

    /**
     * Because Assemblers must recreate Parts (from persisted byte[]) of the right type
     * we need a way to get the correct Assembler for the category of Part byte[]s it
     * must reconstruct.  This method does this work of going from category of Parts
     * to Assembler that can reconstruct them.
     */
    public Assembler<A,P> getAssembler(String category);

    /**
     * Given a list of previously extracted (via Assembler.separate) parts, combine them
     * together to get back the (original?) assembly.  THIS MIGHT NOT ACTUALLY BE USEFUL
     * IN PRACTICE unless *all* parts are extracted!
     */
    public A combine(List<P> p);
}

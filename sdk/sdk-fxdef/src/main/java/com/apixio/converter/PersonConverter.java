package com.apixio.converter;

import com.apixio.fxifc.Dob;
import com.apixio.fxifc.Person;
import com.apixio.sdk.Converter;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdkexample.protos.Tests.PbDob;
import com.apixio.sdkexample.protos.Tests.PbPerson;

public class PersonConverter implements Converter<Person,PbPerson>
{
    @Override
    public Meta getMetadata()
    {
        return new Meta(Person.class, PbPerson.class, false);
    }

    @Override
    public void setEnvironment(FxEnvironment env)
    {
    }

    @Override
    public String getID()
    {
        return "apixio.Person";
    }

    /**
     * Convert from interface to protobuf.  No exceptions should be thrown.
     */
    @Override
    public PbPerson convertToProtobuf(Person person)
    {
        PbPerson.Builder builder = PbPerson.newBuilder();
        Dob              dob     = person.getDateOfBirth();

        builder.setFirstname(person.getFirstname());
        builder.setLastname(person.getLastname());

        if (dob != null)
            builder.setDateOfBirth(
                PbDob.newBuilder().setYear(dob.getYear()).setMonth(dob.getMonth()).setDay(dob.getDay()).
                build());

        return builder.build();
    }

    /**
     * Convert from protobuf to interface.  No exceptions should be thrown.
     */
    @Override
    public Person convertToInterface(PbPerson pbPerson)
    {
        PbDob pbDob = pbPerson.getDateOfBirth();
        Dob   dob   = null;

        if (pbDob != null)
            dob = new DobImpl(pbDob.getYear(), pbDob.getMonth(), pbDob.getDay());

        return new PersonImpl(pbPerson.getFirstname(), pbPerson.getLastname(), dob);
    }

    /**
     * implement interfaces
     */
    public static class PersonImpl implements Person
    {
        private String  firstname;
        private String  lastname;
        private Dob     dob;

        public PersonImpl(String firstname, String lastname, Dob dob)
        {
            this.firstname = firstname;
            this.lastname  = lastname;
            this.dob       = dob;
        }

        public String getFirstname()
        {
            return firstname;
        }
        public String getLastname()
        {
            return lastname;
        }
        public Dob getDateOfBirth()
        {
            return dob;
        }
    }

    public static class DobImpl implements Dob
    {
        private int year;
        private int month;
        private int day;

        public DobImpl(int year, int month, int day)
        {
            this.year  = year;
            this.month = month;
            this.day   = day;
        }

        public int getYear()
        {
            return year;
        }
        public int getMonth()
        {
            return month;
        }
        public int getDay()
        {
            return day;
        }
    }


}

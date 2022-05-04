package com.apixio.model.utility;

import com.apixio.XUUID;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by jctoledo on 10/26/16.
 */
public class XUUIDTest {
    @Test
    public void makeIDFromSeed(){
        /**
         * Tests to see if given some string the same xuuid can be generated
         */
        String aString="SOMESTRING";
        UUID uuid = UUID.nameUUIDFromBytes(aString.getBytes());
        XUUID xuuid = XUUID.create("test",uuid,true,false);

        String aString2 = "SOMESTRING";
        UUID uuid2 = UUID.nameUUIDFromBytes(aString2.getBytes());
        XUUID xuuid2 = XUUID.create("test",uuid2,true,false);

        assertEquals(xuuid.toString(), xuuid2.toString());


        String aString3="SOMESTRING";
        UUID uuid3 = UUID.nameUUIDFromBytes(aString3.getBytes());
        XUUID xuuid3 = XUUID.create("test",uuid3,true,false);

        String aString4 = "GNIRTSEMOS";
        UUID uuid4 = UUID.nameUUIDFromBytes(aString4.getBytes());
        XUUID xuuid4 = XUUID.create("test",uuid4,true,false);

        assertNotEquals(xuuid3.toString(), xuuid4.toString());



        String aString5="SOMESTRING";
        UUID uuid5 = UUID.nameUUIDFromBytes(aString5.getBytes());
        XUUID xuuid5 = XUUID.create("test",uuid5,true,false);

        String aString6 = "somestring";
        UUID uuid6 = UUID.nameUUIDFromBytes(aString6.getBytes());
        XUUID xuuid6 = XUUID.create("test",uuid6,true,false);

        assertNotEquals(xuuid5, xuuid6);



    }

}

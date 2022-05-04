package com.apixio.model.blob;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class BlobTypeTest
{
    @Test    
    public void test1()
    {
        String uuid = UUID.randomUUID().toString();
        String imageType = "pdf";
        String resolution = "low";
        int pageNumber = 1;

        BlobType blobType = new BlobType.Builder(uuid, imageType).resolution(resolution).pageNumber(pageNumber).build();

        System.out.println(blobType);
        String ID = blobType.getID();
        System.out.println(ID);

        System.out.println(blobType.getDocUUID());
        System.out.println(BlobType.getDocUUID(ID));
        assertEquals(uuid, blobType.getDocUUID());
        assertEquals(uuid, BlobType.getDocUUID(ID));

        System.out.println(blobType.getImageType());
        System.out.println(BlobType.getImageType(ID));
        assertEquals(imageType, blobType.getImageType());
        assertEquals(imageType, BlobType.getImageType(ID));

        System.out.println(blobType.getResolution());
        System.out.println(BlobType.getResolution(ID));
        assertEquals(resolution, blobType.getResolution());
        assertEquals(resolution, BlobType.getResolution(ID));

        System.out.println(blobType.getPageNumber());
        System.out.println(BlobType.getPageNumber(ID));
        assertEquals(pageNumber, blobType.getPageNumber());
        assertEquals(pageNumber, BlobType.getPageNumber(ID));

        BlobType clone = BlobType.getBlobData(ID);
        System.out.println(clone);
    }

    @Test
    public void test2()
    {
        String uuid = UUID.randomUUID().toString();
        String imageType = "pdf";
        String resolution = null;
        int pageNumber = 1;

        BlobType blobType = new BlobType.Builder(uuid, imageType).pageNumber(pageNumber).build();

        System.out.println(blobType);
        String ID = blobType.getID();
        System.out.println(ID);

        System.out.println(blobType.getDocUUID());
        System.out.println(BlobType.getDocUUID(ID));
        assertEquals(uuid, blobType.getDocUUID());
        assertEquals(uuid, BlobType.getDocUUID(ID));

        System.out.println(blobType.getImageType());
        System.out.println(BlobType.getImageType(ID));
        assertEquals(imageType, blobType.getImageType());
        assertEquals(imageType, BlobType.getImageType(ID));

        System.out.println(blobType.getResolution());
        System.out.println(BlobType.getResolution(ID));
        assertEquals(resolution, blobType.getResolution());
        assertEquals(resolution, BlobType.getResolution(ID));

        System.out.println(blobType.getPageNumber());
        System.out.println(BlobType.getPageNumber(ID));
        assertEquals(pageNumber, blobType.getPageNumber());
        assertEquals(pageNumber, BlobType.getPageNumber(ID));

        BlobType clone = BlobType.getBlobData(ID);
        System.out.println(clone);
    }

    @Test
    public void test3()
    {
        String uuid = UUID.randomUUID().toString();
        String imageType = "pdf";
        String resolution = "low";
        int pageNumber = -1;

        BlobType blobType = new BlobType.Builder(uuid, imageType).resolution(resolution).build();

        System.out.println(blobType);
        String ID = blobType.getID();
        System.out.println(ID);

        System.out.println(blobType.getDocUUID());
        System.out.println(BlobType.getDocUUID(ID));
        assertEquals(uuid, blobType.getDocUUID());
        assertEquals(uuid, BlobType.getDocUUID(ID));

        System.out.println(blobType.getImageType());
        System.out.println(BlobType.getImageType(ID));
        assertEquals(imageType, blobType.getImageType());
        assertEquals(imageType, BlobType.getImageType(ID));

        System.out.println(blobType.getResolution());
        System.out.println(BlobType.getResolution(ID));
        assertEquals(resolution, blobType.getResolution());
        assertEquals(resolution, BlobType.getResolution(ID));

        System.out.println(blobType.getPageNumber());
        System.out.println(BlobType.getPageNumber(ID));
        assertEquals(pageNumber, blobType.getPageNumber());
        assertEquals(pageNumber, BlobType.getPageNumber(ID));

        BlobType clone = BlobType.getBlobData(ID);
        System.out.println(clone);
    }

    @Test
    public void test4()
    {
        String uuid = UUID.randomUUID().toString();
        String imageType = "pdf";
        String resolution = null;
        int pageNumber = -1;

        BlobType blobType = new BlobType.Builder(uuid, imageType).build();

        System.out.println(blobType);
        String ID = blobType.getID();
        System.out.println(ID);

        System.out.println(blobType.getDocUUID());
        System.out.println(BlobType.getDocUUID(ID));
        assertEquals(uuid, blobType.getDocUUID());
        assertEquals(uuid, BlobType.getDocUUID(ID));

        System.out.println(blobType.getImageType());
        System.out.println(BlobType.getImageType(ID));
        assertEquals(imageType, blobType.getImageType());
        assertEquals(imageType, BlobType.getImageType(ID));

        System.out.println(blobType.getResolution());
        System.out.println(BlobType.getResolution(ID));
        assertEquals(resolution, blobType.getResolution());
        assertEquals(resolution, BlobType.getResolution(ID));

        System.out.println(blobType.getPageNumber());
        System.out.println(BlobType.getPageNumber(ID));
        assertEquals(pageNumber, blobType.getPageNumber());
        assertEquals(pageNumber, BlobType.getPageNumber(ID));

        BlobType clone = BlobType.getBlobData(ID);
        System.out.println(clone);
    }
}


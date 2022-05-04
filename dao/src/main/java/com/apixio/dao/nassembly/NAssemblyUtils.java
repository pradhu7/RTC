package com.apixio.dao.nassembly;

class NAssemblyUtils
{
    static void validateNotEmpty(String st)
    {
        if (st == null)
            throw new IllegalArgumentException("Can't be null");
        else if (st.isEmpty())
            throw new IllegalArgumentException("Can't be empty");
    }

    static void validateNotEmpty(String[] stList)
    {
        if (stList == null)
            throw new IllegalArgumentException("Can't be null");
        else if (stList.length == 0)
            throw new IllegalArgumentException("Can't be empty");
    }
}

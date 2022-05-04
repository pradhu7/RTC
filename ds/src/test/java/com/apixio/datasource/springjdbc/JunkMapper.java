package com.apixio.datasource.springjdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class JunkMapper implements RowMapper<Junk>
{
    public Junk mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        return new Junk(rs.getInt(1), rs.getString(2));
    }
}

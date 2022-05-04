package com.apixio.datasource.springjdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

public class JunkUUIDMapper implements RowMapper<JunkUUID>
{
    public JunkUUID mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        return new JunkUUID(UUID.fromString(rs.getString(1)), rs.getString(2));
    }
}

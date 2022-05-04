package com.apixio.dao.utility.sql;

import com.apixio.restbase.config.MacroUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class to support SQL DAOs using macros
 */
public class SqlDaoUtility {

    public static String makeSql(Map<String,String> SQL_MACROS, String tpl, String... args)
    {
        String sql = MacroUtil.replaceMacros(SQL_MACROS, false, tpl);

        if (args.length > 0)
            sql = String.format(sql, args);

        return sql;
    }

    public static String strList(List<String> ids)
    {
        return ids.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
    }

    public static String orderBy(List<SqlOrdering> sortFields) {
        if (sortFields.isEmpty()) return "";
        else {
            String orderBy = "ORDER BY";
            String sorts = sortFields.stream().map(p -> p.getFieldName() + " " + (p.getUseAsc() ? "ASC" : "DESC"))
                    .collect(Collectors.joining(", "));
            return orderBy + " " + sorts;
        }
    }

}

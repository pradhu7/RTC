package com.apixio.datasource.springjdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Ignore;
import org.springframework.jdbc.core.JdbcTemplate;

@Ignore("CommandLine")
public class JdbcTest
{

    private JdbcDS          ds;
    private JdbcTransaction tx;

    public static void main(String... args)
    {
        String   db     = args[0];
        JdbcTest tester = new JdbcTest(db);

        if (isMySQL(db))
            tester.testEntitiesInt();
        else if (isPostgres(db))
            tester.testEntitiesUUID();

        //        tester.testPool();
        //        tester.testTx();
        //        tester.testEntitiesTx();
    }

    private JdbcTest(String db)
    {
        ds = new JdbcDS(makeConfig(db));
        tx = ds.getTransactionManager();
    }

    private void testEntitiesInt()
    {
        EntitiesDS<Junk>   dao = new EntitiesDS<>(ds, new JunkMapper(), null);
        Map<String,Object> np  = new HashMap<>();
        int                nid;

        //mysql:  create table junk (junk_id integer primary key auto_increment, junk_name varchar(255));

        nid = dao.createEntity("insert into junk (junk_name) values (:name)", Binding.byName("name", "foobynm"), true);

        System.out.println("Entity: "   + dao.loadEntity("select * from junk where junk_id = ?", Binding.byPosition(2)));
        System.out.println("Entities: " + dao.loadEntities("select * from junk", null));
        System.out.println("Count: "    + dao.getValue("select count(*) from junk", null, Integer.class));
        System.out.println("sum: "      + dao.getValue("select sum(junk_id) from junk", null, Integer.class));

        dao.deleteEntities("delete from junk where junk_id = ?", Binding.byPosition(3));
        dao.updateEntities("update junk set junk_name = ? where junk_id = ?", Binding.byPosition("boofoor", 5));
    }

    private void testEntitiesUUID()
    {
        EntitiesDS<JunkUUID> dao = new EntitiesDS<>(ds, new JunkUUIDMapper(), null);
        Map<String,Object>   np  = new HashMap<>();
        UUID                 id;

        //pg:  create table junk (junk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(), junk_name varchar(255));

        id = dao.createEntity("insert into junk (junk_name) values (:name)", Binding.byName("name", "foobynm"), true, "junk_id", UUID.class);

        System.out.println("Entity: "   + dao.loadEntity("select * from junk where junk_id = ?", Binding.byPosition(id.toString())));
        System.out.println("Entities: " + dao.loadEntities("select * from junk", null));
        System.out.println("Count: "    + dao.getValue("select count(*) from junk", null, Long.class));

        dao.deleteEntities("delete from junk where junk_id = ?", Binding.byPosition(id.toString()));
        dao.updateEntities("update junk set junk_name = ? where junk_id = ?", Binding.byPosition("boofoor", id.toString()));
    }

    private void testTx()
    {
        int rc = ds.getJdbcTemplate().queryForObject("select count(*) from junk", Integer.class);
        System.out.println("rc = " + rc);

        System.out.println("in tx: " + tx.inTransaction());
        tx.begin();
        ds.getJdbcTemplate().execute("insert into junk (junk_name) values('hi')");

        tx.abort();
        System.out.println("after abort:  in tx: " + tx.inTransaction());

        tx.begin();
        ds.getJdbcTemplate().execute("insert into junk (junk_name) values('bye')");
        ds.getJdbcTemplate().execute("insert into junk (junk_name) values('now')");
        tx.commit();

        System.out.println("after begin & commit:  in tx: " + tx.inTransaction());
    }

    private void testEntitiesTx()
    {
        EntitiesDS<Junk> je = new EntitiesDS<>(ds, new JunkMapper());

        tx.begin();

        System.out.println("Entity: " + je.loadEntity("select * from junk where junk_id = ?", Binding.byPosition(2)));
        System.out.println("Entity: " + je.loadEntity("select * from junk where junk_id = ?", Binding.byPosition(2)));

        tx.commit();
    }


    private void testPool()
    {
        for (int i = 0; i < 50; i++)
            runThread(ds, i);
    }

    private void runThread(JdbcDS base, int i)
    {
        Threader t = new Threader(base, i);

        (new Thread(t)).start();
    }

    private class Threader implements Runnable
    {
        private JdbcDS base;
        private int    name;

        Threader(JdbcDS base, int n)
        {
            this.base = base;
            this.name = n;
        }

        @Override
        public void run()
        {
            JdbcTemplate    jdbc = base.getJdbcTemplate();
            JdbcTransaction tx   = base.getTransactionManager();

            tx.begin();

            System.out.println("thread " + name + ":  before sleep rc = " + jdbc.queryForObject("select count(*) from junk", Integer.class));

            jdbc.execute("insert into junk(junk_name) values('" + name + "')");

            try
            {
                Thread.sleep(5000L);
            }
            catch (InterruptedException x)
            {
            }

            System.out.println("thread " + name + ":  after sleep rc = " + jdbc.queryForObject("select count(*) from junk", Integer.class));

            if ((name % 2) == 0)
                tx.abort();
            else
                tx.commit();
        }
    }

    // ################################################################

    private static boolean isMySQL(String db)
    {
        return db.equals("mysql");
    }
    private static boolean isPostgres(String db)
    {
        return db.equals("pg");
    }

    private static Map<String, Object> makeConfig(String db)
    {
        Map<String, Object> config = new HashMap<>();

        if (isMySQL(db))
        {
            config.put(JdbcDS.JDBC_CONNECTIONURL,   "jdbc:mysql://localhost:3306/signalmgmt");
            config.put(JdbcDS.JDBC_DRIVERCLASSNAME, "com.mysql.jdbc.Driver");
            config.put(JdbcDS.JDBC_USERNAME,        "scott");
            config.put(JdbcDS.JDBC_PASSWORD,        "scott");
        }
        else if (isPostgres(db))
        {
            config.put(JdbcDS.JDBC_CONNECTIONURL,   "jdbc:postgresql://localhost:26257/signalmgmt?sslmode=disable");
            config.put(JdbcDS.JDBC_DRIVERCLASSNAME, "org.postgresql.Driver");
            config.put(JdbcDS.JDBC_USERNAME,        "root");
        }

        config.put(JdbcDS.POOL_MAXTOTAL,  25);
        config.put(JdbcDS.SYS_VERBOSESQL, Boolean.TRUE);

        return config;
    }

}

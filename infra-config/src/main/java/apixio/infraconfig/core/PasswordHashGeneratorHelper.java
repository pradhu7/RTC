package apixio.infraconfig.core;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Factory.Argon2Types;
import de.mkammerer.argon2.Argon2Helper;
import org.joda.time.DateTime;

import java.util.logging.Logger;

public class PasswordHashGeneratorHelper {
    public static Argon2 generator = Argon2Factory.create(Argon2Types.ARGON2id);
    public static String hashPrefix = "$argon2";
    private static int memory = 65536;
    private static int parallelism = 1;
    private static int iterations = Argon2Helper.findIterations(generator, 1000, memory, parallelism);
    private static Logger log = Logger.getLogger(PasswordHashGeneratorHelper.class.getName());

    public static String hash(String password) {
        DateTime startTime = DateTime.now();
        char[] charPassword = password.toCharArray();
        String passwordHash;
        try {
            passwordHash = generator.hash(iterations, memory, parallelism, charPassword);
        } finally {
            generator.wipeArray(charPassword);
        }
        DateTime stopTime = DateTime.now();
        log.info(String.format("Generated argon2 hash in %d ms", (stopTime.getMillis() - startTime.getMillis())));
        return passwordHash;
    }

    public static Boolean verify(String password, String hash) {
        return generator.verify(hash, password.toCharArray());
    }

    public static int getIterations() {
        return iterations;
    }
}

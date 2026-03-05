import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Standalone utility for generating BCrypt password hashes.
 *
 * <p>This class is intended for offline / developer use — for example,
 * pre-hashing an admin password before inserting it into the database
 * seed script ({@code init.sql}).  It relies on Spring Security's
 * {@link BCryptPasswordEncoder} with the default strength (10 rounds).
 *
 * <h3>Usage</h3>
 * <pre>
 *   javac -cp spring-security-crypto-*.jar HashGenerator.java
 *   java  -cp .:spring-security-crypto-*.jar HashGenerator
 * </pre>
 *
 * @author Sourabh
 */
public class HashGenerator {

    /**
     * Program entry point.
     *
     * <p>Creates a {@link BCryptPasswordEncoder}, hashes the hard-coded
     * password {@code "Admin@123"}, and prints the resulting BCrypt hash
     * to standard output.  The printed value can be copied directly into
     * an SQL {@code INSERT} statement for seeding an admin user.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        System.out.println(enc.encode("Admin@123"));
    }
}
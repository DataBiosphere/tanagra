package bio.terra.tanagra.service.search.utils;

import java.util.Random;

/**
 * Utility method for generating random numbers. Supports testing mode where the generator is
 * re-seeded for each test, to produce an identical sequence that can be used in expected/actual
 * comparisons.
 */
public final class RandomNumberGenerator {
  private RandomNumberGenerator() {}

  // default un-seeded random number generator used in production
  private static final Random UNSEEDED_RANDOM = new Random();
  // seeded random number generator used during testing
  private static Random seededRandom = new Random();

  // system property name indicating that we should use a seeded random (set by test harness before
  // each test method)
  private static final String USE_RANDOM_SEED_SYSTEM_PROPERTY = "GENERATE_SQL_RANDOM_SEED";

  // whether to use the seeded random (true when testing), or unseeded (false when not testing)
  private static final boolean USE_SEEDED_RANDOM = useSeededRandom();

  // hard-coded (arbitrary) number to re-seed the generator with at the beginning of each test
  private static final long RANDOM_SEED = 2022;

  /** Return a random number generator. Used in SQL query generation. */
  public static Random getRandom() {
    return USE_SEEDED_RANDOM ? getSeededRandom() : UNSEEDED_RANDOM;
  }

  /**
   * Check if the system property is set. This method is called to set a final static boolean, so
   * that we only need to check the system property once in the non-testing case, not each time we
   * need a random number.
   */
  private static boolean useSeededRandom() {
    String useRandomSeedSysProp = System.getProperty(USE_RANDOM_SEED_SYSTEM_PROPERTY);
    return useRandomSeedSysProp != null && !useRandomSeedSysProp.isEmpty();
  }

  /** Return a seeded random generator. */
  private static Random getSeededRandom() {
    synchronized (RandomNumberGenerator.class) {
      // check the sys prop to see if we need to re-start the seeded random.
      // we want to do this for each test, so that individual tests always see the same random
      // sequence, regardless of what order the tests are run in.
      String useRandomSeedSysProp = System.getProperty(USE_RANDOM_SEED_SYSTEM_PROPERTY);
      if ("true".equals(useRandomSeedSysProp)) {
        System.setProperty(USE_RANDOM_SEED_SYSTEM_PROPERTY, "false");
        seededRandom = new Random(RANDOM_SEED);
      }
    }
    return seededRandom;
  }
}

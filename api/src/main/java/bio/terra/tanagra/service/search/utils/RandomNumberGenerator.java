package bio.terra.tanagra.service.search.utils;

import java.util.Random;

public final class RandomNumberGenerator {
  private RandomNumberGenerator() {}

  private static final Random UNSEEDED_RANDOM = new Random();
  private static Random seededRandom = new Random();
  private static final long RANDOM_SEED = 2022;

  public static Random getRandom() {
    return USE_SEEDED_RANDOM ? getSeededRandom() : UNSEEDED_RANDOM;
  }

  // System property name indicating that we should use a seeded random (used for testing)
  private static final String USE_RANDOM_SEED_SYSTEM_PROPERTY = "GENERATE_SQL_RANDOM_SEED";
  private static final boolean USE_SEEDED_RANDOM = useSeededRandom();

  private static boolean useSeededRandom() {
    String useRandomSeedSysProp = System.getProperty(USE_RANDOM_SEED_SYSTEM_PROPERTY);
    return useRandomSeedSysProp != null && !useRandomSeedSysProp.isEmpty();
  }

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

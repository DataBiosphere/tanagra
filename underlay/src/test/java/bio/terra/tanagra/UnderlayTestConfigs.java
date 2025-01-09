package bio.terra.tanagra;

public enum UnderlayTestConfigs {
  AOUSC2023Q3R2("aouSC2023Q3R2_oneverily_dev"),
  AOUSR2019Q4R4("aouSR2019q4r4_broad"),
  CMSSYNPUF("cmssynpuf_broad"),
  SD20230331("sd20230331_verily"),
  SD20230831("sd20230831_verily");

  private final String fileName;

  UnderlayTestConfigs(String fileName) {
    this.fileName = fileName;
  }

  public String fileName() {
    return fileName;
  }
}

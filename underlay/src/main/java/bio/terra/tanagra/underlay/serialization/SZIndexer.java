package bio.terra.tanagra.underlay.serialization;

public class SZIndexer {
  public String underlay;
  public SZBigQuery bigQuery;
  public Dataflow dataflow;

  public static class Dataflow {
    public String serviceAccountEmail;
    public String dataflowLocation;
    public String gcsTempDirectory;
    public String workerMachineType;
    public boolean usePublicIps;
    public String vpcSubnetworkName;
  }
}

package bio.terra.tanagra.serialization2;

public class SZIndexer {
  public String underlay;
  public SZBigQuery bigQuery;
  public Dataflow dataflow;

  public static class Dataflow {
    public String serviceAccountEmail;
    public String gcsTempDirectory;
    public String workerMachineType;
    public boolean usePublicIps;
    public String vpcSubnetworkName;
  }
}

package bio.terra.tanagra.app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(
    // We don't make use of DataSource in this application, so exclude it from scanning.
    exclude = DataSourceAutoConfiguration.class)
@ComponentScan(
    // Scan all packages within this service
    basePackages = "bio.terra.tanagra")
// Spring needs Main to not be a utility class.
@SuppressWarnings({"PMD.UseUtilityClass", "HideUtilityClassConstructor"})
public class Main {
  public static void main(String[] args) {
    new SpringApplicationBuilder(Main.class).run(args);
  }
}

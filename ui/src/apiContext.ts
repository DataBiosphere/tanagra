import React from "react";
import * as tanagra from "./tanagra-api";

// TODO(tjennison): Figure out a more comprehensive solutions for faking APIs.
class FakeUnderlaysApi {
  async listUnderlays(): Promise<tanagra.ListUnderlaysResponse> {
    return new Promise<tanagra.ListUnderlaysResponse>((resolve) => {
      resolve({
        underlays: [
          {
            name: "underlay_name",
            entityNames: ["entity 1", "entity 2"],
          },
        ],
      });
    });
  }
}

class FakeEntityInstancesApi {
  async searchEntityInstances(): Promise<tanagra.SearchEntityInstancesResponse> {
    return new Promise<tanagra.SearchEntityInstancesResponse>((resolve) => {
      resolve({
        instances: [
          {
            concept_name: {
              stringVal: "test concept",
            },
            concept_id: {
              int64Val: 1234,
            },
          },
        ],
      });
    });
  }

  async generateDatasetSqlQuery(): Promise<tanagra.SqlQuery> {
    return new Promise<tanagra.SqlQuery>((resolve) => {
      resolve({ query: "SELECT * FROM table WHERE xyz;" });
    });
  }
}

function apiForEnvironment<Real, Fake>(
  real: { new (c: tanagra.Configuration): Real },
  fake: { new (): Fake }
) {
  const fn = () => {
    if (process.env.REACT_APP_USE_FAKE_API === "y") {
      return new fake();
    }

    const config: tanagra.ConfigurationParameters = { basePath: "" };
    return new real(new tanagra.Configuration(config));
  };
  return React.createContext(fn());
}

export const UnderlaysApiContext = apiForEnvironment(
  tanagra.UnderlaysApi,
  FakeUnderlaysApi
);
export const EntityInstancesApiContext = apiForEnvironment(
  tanagra.EntityInstancesApi,
  FakeEntityInstancesApi
);

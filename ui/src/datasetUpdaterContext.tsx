import * as _ from "lodash";
import { createContext, ReactNode, useContext, useMemo } from "react";
import { Criteria, Dataset } from "./dataset";

const DatasetUpdaterContext = createContext<DatasetUpdater | null>(null);

export function useDatasetUpdater(): DatasetUpdater {
  const updater = useContext(DatasetUpdaterContext);
  if (!updater) {
    throw new Error("invalid updater context");
  }
  return updater;
}

class DatasetUpdater {
  constructor(
    public dataset: Dataset,
    public setDataset: (dataset: Dataset) => void
  ) {}

  update(callback: (dataset: Dataset) => void): void {
    const newDataset = _.cloneDeep(this.dataset);
    callback(newDataset);
    this.setDataset(newDataset);
  }

  updateCriteria<Type extends Criteria>(
    groupId: string,
    criteriaId: string,
    callback: (criteria: Type) => void
  ): void {
    this.update((dataset: Dataset) => {
      const group = dataset.findGroup(groupId);
      const criteria = group?.findCriteria(criteriaId);
      if (!criteria) {
        throw new Error("criteria not found");
      }
      callback(criteria as Type);
    });
  }
}

type DatasetUpdaterProviderProps = {
  dataset: Dataset;
  setDataset(dataset: Dataset): void;
  children?: ReactNode;
};

export function DatasetUpdaterProvider(props: DatasetUpdaterProviderProps) {
  const updater = useMemo(
    () => new DatasetUpdater(props.dataset, props.setDataset),
    [props.dataset, props.setDataset]
  );

  return (
    <DatasetUpdaterContext.Provider value={updater}>
      {props.children}
    </DatasetUpdaterContext.Provider>
  );
}

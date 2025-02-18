import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import { createCriteria, lookupCriteria, LookupEntry } from "cohort";
import Checkbox from "components/checkbox";
import Loading from "components/loading";
import {
  TreeGrid,
  TreeGridColumn,
  TreeGridId,
  useArrayAsTreeGridData,
} from "components/treegrid";
import ActionBar from "actionBar";
import { DataKey } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useUnderlay, useCohortGroupSectionAndGroup } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useCallback, useMemo, useState } from "react";
import useSWRMutation from "swr/mutation";
import { useImmer } from "use-immer";
import { TextField } from "@mui/material";
import { useNavigate } from "util/searchState";
import { cohortURL, useIsSecondBlock } from "router";
import { insertCohortCriteria, useCohortContext } from "cohortContext";
import { isValid } from "util/valid";

type LookupEntryItem = {
  config?: JSX.Element;
  code: DataKey;
  name?: string;
  entry?: LookupEntry;
};

export function AddByCode() {
  const underlay = useUnderlay();
  const underlaySource = useUnderlaySource();
  const context = useCohortContext();
  const navigate = useNavigate();
  const { cohort, section } = useCohortGroupSectionAndGroup();
  const secondBlock = useIsSecondBlock();

  const [query, setQuery] = useState<string | undefined>();
  const [selected, updateSelected] = useImmer(new Set<DataKey>());

  const configMap = useMemo(
    () =>
      new Map(underlay.criteriaSelectors.map((s) => [s.name, s.displayName])),
    [underlay.criteriaSelectors]
  );

  const lookupEntriesState = useSWRMutation<LookupEntryItem[]>(
    {
      component: "AddByCode",
      query,
    },
    async () => {
      const codes = [
        ...new Set(
          query
            ?.split(/[\s,]+/)
            .map((c) => c.trim())
            .filter((c) => c.length)
        ),
      ];
      if (!codes) {
        return [];
      }

      const entries = (
        await lookupCriteria(
          underlaySource,
          underlay.criteriaSelectors.filter((s) => s.isEnabledForCohorts),
          codes
        )
      )?.data;
      if (!entries) {
        return [];
      }

      const entryMap = new Map(entries.map((e) => [e.code, e]));
      const mappedEntries = codes.map((c) => {
        const entry = entryMap.get(c);
        if (!entry) {
          return {
            config: <Chip color="error" label="Not found" size="small" />,
            code: c,
            name: "There were no matches for this code",
          };
        }
        return {
          config: <Chip label={configMap.get(entry.config)} size="small" />,
          code: entry.code,
          name: entry.name,
          entry: entry,
        };
      });

      updateSelected((s) => {
        s.clear();
        mappedEntries.forEach((e) => {
          if (e.entry) {
            s.add(e.code);
          }
        });
      });
      return mappedEntries;
    }
  );

  const data = useArrayAsTreeGridData(lookupEntriesState?.data ?? [], "code");

  const onInsert = useCallback(() => {
    const configMap = new Map<string, LookupEntryItem[]>();
    lookupEntriesState.data?.forEach((e) => {
      if (!e.entry || !selected.has(e.code)) {
        return;
      }

      const list = configMap.get(e.entry.config) ?? [];
      list.push(e);
      configMap.set(e.entry.config, list);
    });

    const criteria = Array.from(configMap.entries()).map(([c, entries]) => {
      const config = underlay.criteriaSelectors.find(
        (config) => config.name === c
      );
      if (!config) {
        throw new Error(`Config ${c} not found.`);
      }

      return createCriteria(
        underlaySource,
        config,
        entries.map((e) => e.entry?.data).filter(isValid)
      );
    });

    const group = insertCohortCriteria(
      context,
      section.id,
      criteria,
      secondBlock
    );
    navigate("../../../" + cohortURL(cohort.id, section.id, group.id));
  }, [context, cohort.id, section.id, navigate]);

  return (
    <GridLayout rows fillRow={1}>
      <ActionBar title={"Adding criteria by codes"} />
      <GridLayout
        rows
        sx={{
          backgroundColor: (theme) => theme.palette.background.paper,
        }}
      >
        <GridBox
          sx={{
            px: 5,
            py: 3,
            height: "auto",
          }}
        >
          <GridLayout cols fillCol={0} spacing={1}>
            <TextField
              variant="outlined"
              multiline
              fullWidth
              autoFocus
              autoComplete="off"
              minRows={3}
              maxRows={10}
              placeholder={
                "Enter codes to lookup separated by commas, spaces, or line breaks"
              }
              value={query}
              sx={{
                backgroundColor: (theme) => theme.palette.info.main,
                borderRadius: "16px",
                "& .MuiOutlinedInput-root": {
                  borderRadius: "16px",
                },
              }}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                setQuery(event.target.value);
              }}
            />
            <GridLayout rows rowAlign="bottom">
              <Button
                variant="contained"
                onClick={() => lookupEntriesState.trigger()}
              >
                Lookup
              </Button>
            </GridLayout>
          </GridLayout>
        </GridBox>
        <Loading immediate showProgressOnMutate status={lookupEntriesState}>
          {lookupEntriesState.data?.length ? (
            <TreeGrid
              columns={columns}
              data={data}
              rowCustomization={(id: TreeGridId) => {
                if (!lookupEntriesState.data) {
                  return undefined;
                }

                const item = data.get(id)?.data as LookupEntryItem;
                if (!item) {
                  return undefined;
                }

                const sel = selected.has(id);
                return [
                  {
                    column: 2,
                    prefixElements: (
                      <Checkbox
                        size="small"
                        fontSize="inherit"
                        checked={sel}
                        disabled={!item.entry}
                        onChange={() => {
                          updateSelected((selected) => {
                            if (sel) {
                              selected.delete(id);
                            } else {
                              selected.add(id);
                            }
                          });
                        }}
                      />
                    ),
                  },
                ];
              }}
            />
          ) : null}
        </Loading>
      </GridLayout>
      <GridLayout
        colAlign="right"
        sx={{
          p: 1,
        }}
      >
        <Button
          variant="contained"
          size="large"
          disabled={selected.size === 0}
          onClick={() => onInsert()}
        >
          Add selected criteria
        </Button>
      </GridLayout>
    </GridLayout>
  );
}

const columns: TreeGridColumn[] = [
  {
    key: "config",
    width: "160px",
  },
  {
    key: "code",
    width: "130px",
    title: "Code",
  },
  {
    key: "name",
    width: "100%",
    title: "Name",
  },
];

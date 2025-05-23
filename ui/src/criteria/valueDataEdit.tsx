import TuneIcon from "@mui/icons-material/Tune";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Typography from "@mui/material/Typography";
import { HintDataSelect } from "components/hintDataSelect";
import { containedIconButtonSx } from "components/iconButton";
import Loading from "components/loading";
import { DataRange, RangeSlider } from "components/rangeSlider";
import { HintData } from "data/source";
import { DataKey } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { produce } from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { ReactNode, useMemo } from "react";
import useSWRImmutable from "swr/immutable";
import { isValid } from "util/valid";
import {
  ANY_VALUE,
  ANY_VALUE_DATA,
  ValueConfig,
  ValueData,
  ValueSelection,
  defaultValueData,
  maxBound,
  minBound,
} from "criteria/valueData";

export type ValueDataEditProps = {
  hintEntity: string;
  // If hintData is supplied it will be used instead of fetching it.
  hintData?: HintData[];

  relatedEntity?: string;
  hintKey?: DataKey;

  singleValue?: boolean;
  valueConfigs: ValueConfig[];

  // The list of ValueData is optional so callers aren't responsible for
  // providing a default value, which is awkward for multiselect when a list is
  // required.
  valueData?: ValueData[];
  update: (data?: ValueData[]) => void;

  // If a title is set, it's assumed that the value data is part of a list and
  // the UI is configured accordingly.
  title?: string;
};

export function ValueDataEdit(props: ValueDataEditProps) {
  const underlaySource = useUnderlaySource();

  const entity = underlaySource.lookupEntity(props.hintEntity);

  const valueDataList = useMemo((): ValueData[] => {
    if (props.valueData) {
      return props.valueData;
    }

    return props.singleValue ? [ANY_VALUE_DATA] : [];
  }, [props.valueData, props.singleValue]);

  const hintDataState = useSWRImmutable(
    {
      type: "hintData",
      hintEntity: props.hintEntity,
      relatedEntity: props.relatedEntity,
      key: props.hintKey,
      hintData: props.hintData,
    },
    async (key) => {
      const hintData =
        props.hintData ??
        (props.valueConfigs
          ? await underlaySource.getAllHintData(key.hintEntity, {
              relatedEntity: key.relatedEntity,
              relatedId: key.key,
            })
          : undefined);
      return {
        hintData,
      };
    }
  );

  const onSelect = (event: SelectChangeEvent<string>) => {
    const {
      target: { value: sel },
    } = event;
    const attribute =
      props.valueConfigs.find((c) => c.attribute === sel)?.attribute ??
      ANY_VALUE;
    if (valueDataList[0] && attribute === valueDataList[0]?.attribute) {
      return;
    }

    const hintData = hintDataState.data?.hintData?.find(
      (hint) => hint.attribute === attribute
    );

    const entityAttribute = entity.attributes.find((a) => a.name === attribute);

    if (hintData) {
      props.update([defaultValueData(hintData, entityAttribute)]);
    } else {
      props.update([ANY_VALUE_DATA]);
    }
  };

  const selectedConfigs = useMemo(() => {
    return props.valueConfigs
      .map((valueConfig) => {
        const hintData = hintDataState.data?.hintData?.find(
          (hint) => hint.attribute === valueConfig.attribute
        );
        if (!hintData) {
          return null;
        }

        const attribute = entity.attributes.find(
          (a) => a.name === hintData.attribute
        );

        let valueData = valueDataList.find(
          (data) => data.attribute === valueConfig.attribute
        );
        if (props.singleValue && !valueData) {
          return null;
        }
        if (!valueData) {
          valueData = defaultValueData(hintData, attribute);
        }

        return {
          valueConfig,
          valueData,
          hintData,
          attribute,
        };
      })
      .filter(isValid);
  }, [
    props.singleValue,
    props.valueConfigs,
    valueDataList,
    hintDataState.data,
    entity,
  ]);

  const onValueSelect = (sel: ValueSelection[], valueData: ValueData) => {
    props.update(
      produce(valueDataList, (data) => {
        if (!props.singleValue && sel.length === 0) {
          return data.filter(
            (vd) =>
              vd.attribute != valueData.attribute && vd.attribute != ANY_VALUE
          );
        }

        const existing = data.find(
          (vd) =>
            vd.attribute === valueData.attribute || vd.attribute === ANY_VALUE
        );
        if (existing) {
          existing.attribute = valueData.attribute;
          existing.selected = sel;
        } else {
          valueData.selected = sel;
          data.push(valueData);
        }
      })
    );
  };

  const onUpdateRange = (
    range: DataRange,
    index: number,
    min: number,
    max: number,
    valueData: ValueData
  ) => {
    props.update(
      produce(valueDataList, (data) => {
        const config = selectedConfigs.find(
          (c) => c.hintData.attribute === valueData.attribute
        );

        if (
          !props.singleValue &&
          config?.hintData?.integerHint &&
          minBound(config?.attribute, config?.hintData) === min &&
          maxBound(config?.attribute, config?.hintData) === max
        ) {
          return data.filter(
            (vd) =>
              vd.attribute != valueData.attribute && vd.attribute != ANY_VALUE
          );
        }

        const existing = data.find(
          (vd) =>
            vd.attribute === valueData.attribute || vd.attribute === ANY_VALUE
        );
        if (existing) {
          existing.attribute = valueData.attribute;
          existing.range.min = min;
          existing.range.max = max;
        } else {
          valueData.range.min = min;
          valueData.range.max = max;
          data.push(valueData);
        }
      })
    );
  };

  const hasHints = !!hintDataState.data?.hintData?.length;

  // Use two Loading components so that the big spinner will only be shown for
  // single select value data but the controls are hidden in both cases.
  return (
    <GridLayout rows height="auto">
      {props.title ? (
        <GridLayout cols spacing={0.5} rowAlign="middle">
          <Typography variant="body2">{props.title}</Typography>
          <Loading status={hintDataState} size="small">
            {hasHints ? (
              <GridBox>
                <Typography variant="body2">
                  <IconButton
                    sx={
                      props.valueData
                        ? containedIconButtonSx("primary")
                        : undefined
                    }
                    onClick={() =>
                      props.update(
                        props.valueData ? undefined : [ANY_VALUE_DATA]
                      )
                    }
                    size="small"
                  >
                    <TuneIcon fontSize="inherit" />
                  </IconButton>
                </Typography>
              </GridBox>
            ) : null}
          </Loading>
          <GridBox />
        </GridLayout>
      ) : null}
      <Loading status={hintDataState} noProgress={!!props.title}>
        {hasHints && (!props.title || isValid(props.valueData)) ? (
          <GridLayout rows height="auto">
            {!!valueDataList.length && props.singleValue ? (
              <FormControl variant="outlined">
                <Select
                  variant="outlined"
                  value={valueDataList[0].attribute}
                  input={<OutlinedInput />}
                  disabled={!hintDataState.data?.hintData?.length}
                  onChange={onSelect}
                >
                  <MenuItem key={ANY_VALUE} value={ANY_VALUE}>
                    Any value
                  </MenuItem>
                  {props.valueConfigs?.map((c) =>
                    hintDataState.data?.hintData?.find(
                      (hint) => hint.attribute === c.attribute
                    ) ? (
                      <MenuItem key={c.attribute} value={c.attribute}>
                        {c.title}
                      </MenuItem>
                    ) : null
                  )}
                </Select>
              </FormControl>
            ) : null}
            {selectedConfigs.map((c, i) => {
              let component: ReactNode = null;
              if (c.hintData.enumHintOptions) {
                component = (
                  <HintDataSelect
                    key={c.valueConfig.attribute}
                    hintData={c.hintData}
                    selected={c.valueData.selected}
                    onSelect={(sel) => onValueSelect(sel, c.valueData)}
                  />
                );
              }
              if (c.hintData.integerHint) {
                component = (
                  <RangeSlider
                    key={c.valueConfig.attribute}
                    index={0}
                    minBound={minBound(c.attribute, c.hintData)}
                    maxBound={maxBound(c.attribute, c.hintData)}
                    range={c.valueData.range}
                    unit={c.valueConfig.unit}
                    onUpdate={(range, index, min, max) =>
                      onUpdateRange(range, index, min, max, c.valueData)
                    }
                  />
                );
              }
              if (!component || props.singleValue) {
                return component;
              }

              return (
                <GridLayout key={c.valueData.attribute} rows height="auto">
                  {i !== 0 ? (
                    <Divider variant="middle">
                      <Chip label="AND" />
                    </Divider>
                  ) : null}
                  <GridLayout cols rowAlign="middle" spacing={3} height="auto">
                    {c.valueConfig.title ? (
                      <Typography variant="body1">
                        {c.valueConfig.title}
                      </Typography>
                    ) : null}
                    {component}
                  </GridLayout>
                </GridLayout>
              );
            })}
            <GridBox />
          </GridLayout>
        ) : null}
      </Loading>
    </GridLayout>
  );
}

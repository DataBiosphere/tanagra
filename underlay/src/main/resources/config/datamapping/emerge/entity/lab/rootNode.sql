SELECT cast(parse_numeric(lc.criteria_meta_seq) as INT64) as id
FROM `${omopDataset}.lab_criteria` lc
WHERE pc.parent_seq = 0

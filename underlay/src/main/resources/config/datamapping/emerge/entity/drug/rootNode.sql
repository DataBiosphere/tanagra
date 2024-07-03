SELECT cast(parse_numeric(dc.criteria_meta_seq) as INT64) as id
FROM `${omopDataset}.med_criteria` dc
WHERE pc.parent_seq = 0

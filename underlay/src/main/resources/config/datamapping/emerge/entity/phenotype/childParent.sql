SELECT criteria_meta_seq AS child,
       cast(parse_numeric(parent_seq) as INT64)  AS parent
FROM `${omopDataset}.phenotype_criteria`
WHERE parent_seq != '0'

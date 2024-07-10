SELECT criteria_meta_seq AS child, parent_seq AS parent
FROM `${omopDataset}.phewas_criteria`
WHERE parent_seq != 0
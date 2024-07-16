SELECT l.ind_seq as person_id,
       cast(parse_numeric(lc.criteria_meta_seq) AS INT64) AS lab_id
FROM `${omopDataset}.labs` AS l
JOIN `${omopDataset}.lab_criteria` lc
    ON l.lab_name = lc.name

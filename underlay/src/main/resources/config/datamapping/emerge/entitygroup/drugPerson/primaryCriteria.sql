SELECT m.ind_seq as person_id,
       cast(parse_numeric(mc.criteria_meta_seq) AS INT64) AS drug_id
FROM `${omopDataset}.meds` AS m
JOIN `${omopDataset}.med_criteria` mc
    ON lower(m.drug_name) = lower(mc.name)

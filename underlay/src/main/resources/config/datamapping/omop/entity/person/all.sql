SELECT
    p.person_id,
    p.year_of_birth,
    p.birth_datetime,
    p.person_source_value,
    p.gender_concept_id,
    gc.concept_name AS gender_concept_name,
    p.race_concept_id,
    rc.concept_name AS race_concept_name,
    p.ethnicity_concept_id,
    ec.concept_name AS ethnicity_concept_name

FROM `${omopDataset}.person` p

LEFT JOIN `${omopDataset}.concept` gc
ON gc.concept_id = p.gender_concept_id

LEFT JOIN `${omopDataset}.concept` rc
ON rc.concept_id = p.race_concept_id

LEFT JOIN `${omopDataset}.concept` ec
ON ec.concept_id = p.ethnicity_concept_id

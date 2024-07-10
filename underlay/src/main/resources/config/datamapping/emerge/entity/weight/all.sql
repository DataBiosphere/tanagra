SELECT
    ROW_NUMBER() OVER() AS weight_id,
    bo.IND_SEQ as person_id,
    bo.weight AS value_as_number,
    1100 as value_as_concept_id,
    'Weight' as value_as_concept_name,
    bo.age_at_event AS age_at_occurrence
FROM `${omopDataset}.bmi` bo


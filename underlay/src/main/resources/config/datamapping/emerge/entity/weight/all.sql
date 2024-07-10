SELECT
    ROW_NUMBER() OVER() AS weight_occurrence_id,
    bo.IND_SEQ as person_id,
    1100 as bmi_concept_id,
    'Weight' as bmi_concept_name,
    bo.weight AS value_as_number,
    bo.age_at_event AS age_at_occurrence
FROM `${omopDataset}.bmi` bo

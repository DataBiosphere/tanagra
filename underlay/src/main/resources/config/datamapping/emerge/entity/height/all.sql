SELECT
    ROW_NUMBER() OVER() AS height_occurrence_id,
    bo.IND_SEQ as person_id,
    1500 as height_concept_id,
    'Height' as height_concept_name,
    bo.height AS value_as_number,
    bo.age_at_event AS age_at_occurrence
FROM `${omopDataset}.bmi` bo

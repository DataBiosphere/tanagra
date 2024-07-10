SELECT
    ROW_NUMBER() OVER() AS height_id,
    bo.IND_SEQ as person_id,
    bo.height AS value_as_number,
    1500 as value_as_concept_id,
    'Height' as value_as_concept_name,
    bo.age_at_event AS age_at_occurrence
FROM `${omopDataset}.bmi` bo


SELECT
    ROW_NUMBER() OVER() AS row_id,
    bo.IND_SEQ as person_id,
    bo.height AS value_as_number,
    bo.age_at_event AS age_at_occurrence
FROM `${omopDataset}.bmi` bo

SELECT
    ROW_NUMBER() OVER() AS bmi_occurrence_id,
    bo.IND_SEQ as person_id,
    1000 as bmi_concept_id,
    'Body Mass Index (BMI)' as bmi_concept_name,
    bo.bmi AS value_as_number,
    bo.age_at_event AS age_at_occurrence
FROM `${omopDataset}.bmi` bo

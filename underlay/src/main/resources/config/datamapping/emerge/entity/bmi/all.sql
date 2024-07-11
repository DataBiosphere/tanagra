SELECT
  ROW_NUMBER() OVER() AS row_id,
  bo.ind_seq as person_id,
  bo.bmi as value_as_number,
  bo.age_at_event as age_at_occurrence
FROM `${omopDataset}.bmi` AS bo

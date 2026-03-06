SELECT parent_id AS parent, id AS child
FROM `${staticTablesDataset}.prep_survey_enhanced`
WHERE parent_id IS NOT NULL
  AND survey_type = 'COVID_VACCINE'

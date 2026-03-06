SELECT parent_id AS parent, id AS child
FROM `${staticTablesDataset}.prep_survey`
WHERE parent_id != 0 AND survey = 'HealthCareAccessUtiliza'

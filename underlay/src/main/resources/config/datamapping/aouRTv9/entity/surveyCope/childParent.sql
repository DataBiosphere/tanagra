SELECT parent_id AS parent, id AS child
FROM `${omopDataset}.prep_survey`
WHERE parent_id != 0 AND survey IN ('May2020Covid19Participa', 'July2020Covid19Particip', 'February2021COVID19Part')

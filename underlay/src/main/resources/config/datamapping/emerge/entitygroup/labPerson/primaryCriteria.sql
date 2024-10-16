SELECT l.ind_seq as person_id,
       lc.id AS lab_id
FROM `${omopDataset}.labs` AS l
JOIN (SELECT
        ROW_NUMBER() OVER(ORDER BY loinc_code, lab_name) as id,
        lab_name,
        loinc_code
      FROM `${omopDataset}.labs`
      GROUP BY 2,3
     ) lc  on l.lab_name=lc.lab_name

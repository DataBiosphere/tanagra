SELECT
    ROW_NUMBER() OVER() AS lab_occurrence_id,
        l.IND_SEQ as person_id,
    lc.id as lab_concept_id,
    l.loinc_code as standard_code,
    l.lab_name AS lab_concept_name,
    CAST(l.lab_value_num as FLOAT64) as value_as_number,
    lvt.lab_value_txt as value_as_string,
    lvt.lab_value_txt_id as value_as_string_id,
    l.lab_value_unit as unit_name,
    CAST(FLOOR(cast(l.age_at_event as NUMERIC)) AS INT64) AS age_at_occurrence
FROM `${omopDataset}.labs` AS l
LEFT JOIN (
        SELECT
            loinc_code,
            trim(lab_value_txt) as lab_value_txt,
            ROW_NUMBER() OVER(PARTITION BY loinc_code) as lab_value_txt_id
        FROM `${omopDataset}.labs`
        WHERE lab_value_txt IS NOT NULL
        GROUP BY 1,2
        ) lvt ON l.loinc_code=lvt.loinc_code and trim(l.lab_value_txt) = lvt.lab_value_txt
JOIN (
    SELECT
        ROW_NUMBER() OVER(ORDER BY loinc_code, lab_name) as id,
        lab_name,
        loinc_code,
   FROM `${omopDataset}.labs`
   GROUP BY 2,3
   ) lc ON l.lab_name=lc.lab_name

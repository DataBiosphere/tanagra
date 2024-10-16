SELECT
    ROW_NUMBER() OVER(ORDER BY l.loinc_code,l.lab_name) as id,
    l.lab_name as name,
    l.loinc_code as concept_code,
    'LAB' as type,
    CASE WHEN l.loinc_code IS NULL THEN 'Custom'
         ELSE 'Standard' END as is_standard,
    CASE WHEN l.loinc_code IS NULL THEN l.lab_name
         ELSE concat(l.loinc_code, ' ', l.lab_name) END as label,
    case WHEN l.loinc_code IS NULL THEN 'VUMC-Lab'
         ELSE 'LOINC' END as vocabulary_id
FROM `${omopDataset}.labs` l
group by 2,3

SELECT
     concept_id AS id,
     concept_id,
     concept_name AS name,
     vocabulary_id AS type,
     (CASE WHEN standard_concept IS NULL THEN 'Source' WHEN standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept,
     concept_code,
     CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
FROM `${omopDataset}.concept`
WHERE vocabulary_id = 'CPT4'
  AND concept_code NOT IN (
    SELECT pc.code FROM `${staticTablesDataset}.prep_cpt` pc WHERE pc.type='CPT4'
  )
  AND concept_id IN (
    SELECT DISTINCT procedure_source_concept_id as code from `${omopDataset}.procedure_occurrence`
    UNION DISTINCT
    SELECT DISTINCT observation_source_concept_id as code from `${omopDataset}.observation`
    UNION DISTINCT
    SELECT DISTINCT measurement_source_concept_id as code from `${omopDataset}.measurement`
    UNION DISTINCT
    SELECT DISTINCT drug_source_concept_id as code from `${omopDataset}.drug_exposure`
    UNION DISTINCT
    SELECT DISTINCT device_source_concept_id as code from `${omopDataset}.device_exposure`
  )


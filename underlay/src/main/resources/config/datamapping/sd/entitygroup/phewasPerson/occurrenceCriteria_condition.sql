SELECT co.condition_occurrence_id, co.person_id, phewas.id AS phewas_id
FROM `${omopDataset}.condition_occurrence` AS co

JOIN `${omopDataset}.concept` AS c
ON c.concept_id = co.condition_source_concept_id

LEFT JOIN `${omopDataset}.phewas_criteria_icd` AS pci
ON pci.icd9_code = c.concept_code

LEFT JOIN (
    SELECT criteria_meta_seq AS id, REGEXP_EXTRACT(label, r'^PHEWAS_([0-9.]+)-') AS code
    FROM `${omopDataset}.phewas_criteria` AS pc
) phewas
ON phewas.code = pci.phewas_code

WHERE phewas.id IS NOT NULL
AND c.vocabulary_id IN ('ICD9CM', 'ICD9Proc')
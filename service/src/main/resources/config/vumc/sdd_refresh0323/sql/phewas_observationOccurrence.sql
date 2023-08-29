SELECT oo.observation_id AS observation_occurrence_id, oo.person_id, phewas.id AS phewas_id
FROM `victr-tanagra-test.sd_20230328.observation` AS oo

JOIN `victr-tanagra-test.sd_20230328.concept` AS c
ON c.concept_id = oo.observation_source_concept_id

LEFT JOIN `victr-tanagra-test.sd_20230328.phewas_criteria_icd` AS pci
ON pci.icd9_code = c.concept_code

LEFT JOIN (
    SELECT criteria_meta_seq AS id, REGEXP_EXTRACT(label, r'^PHEWAS_([0-9.]+)-') AS code
    FROM `victr-tanagra-test.sd_20230328.phewas_criteria` AS pc
) phewas
ON phewas.code = pci.phewas_code

WHERE phewas.id IS NOT NULL
AND c.vocabulary_id IN ('ICD9CM', 'ICD9Proc')
SELECT mo.measurement_id AS measurement_occurrence_id, mo.person_id, phewas.id AS phewas_id
FROM `sd-vumc-tanagra-test.sd_20230331.measurement` AS mo

JOIN `sd-vumc-tanagra-test.sd_20230331.concept` AS c
ON c.concept_id = mo.measurement_source_concept_id

LEFT JOIN `sd-vumc-tanagra-test.sd_20230331.phewas_criteria_icd` AS pci
ON pci.icd9_code = c.concept_code

LEFT JOIN (
    SELECT criteria_meta_seq AS id, REGEXP_EXTRACT(label, r'^PHEWAS_([0-9.]+)-') AS code
    FROM `sd-vumc-tanagra-test.sd_20230331.phewas_criteria` AS pc
) phewas
ON phewas.code = pci.phewas_code

WHERE phewas.id IS NOT NULL
AND c.vocabulary_id IN ('ICD9CM', 'ICD9Proc')
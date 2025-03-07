SELECT pcr.concept_id_2 as id
FROM `${staticTablesDataset}.prep_concept_relationship` pcr
WHERE pcr.concept_id_1 = (SELECT concept_id
                          FROM `${staticTablesDataset}.prep_concept` c
                          WHERE c.concept_class_id = 'ICD10PCS Hierarchy'
                            AND c.concept_code IS NULL
)

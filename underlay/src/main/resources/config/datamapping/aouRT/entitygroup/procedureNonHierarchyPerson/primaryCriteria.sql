SELECT
  po.person_id,
  po.procedure_concept_id
FROM `${omopDataset}.procedure_occurrence` AS po
WHERE po.procedure_concept_id
  IN (SELECT concept_id
      FROM `${omopDataset}.concept` c
      WHERE c.domain_id = 'Procedure'
        AND c.vocabulary_id = 'HCPCS'
        AND c.standard_concept = 'S'
      UNION DISTINCT
      SELECT concept_id
      FROM `${omopDataset}.concept` c
      WHERE c.domain_id = 'Procedure'
        AND c.vocabulary_id = 'LOINC'
        AND c.standard_concept = 'S')

SELECT
  po.person_id,
  po.procedure_concept_id
FROM `${omopDataset}.procedure_occurrence` AS po
WHERE po.procedure_concept_id
  IN (SELECT concept_id
      FROM `${omopDataset}.concept` c
      WHERE c.domain_id = 'Procedure'
        AND c.vocabulary_id IN ('LOINC', 'HCPCS')
        AND c.standard_concept = 'S'
      )
  AND po.procedure_concept_id IS NOT NULL
  AND po.procedure_concept_id != 0

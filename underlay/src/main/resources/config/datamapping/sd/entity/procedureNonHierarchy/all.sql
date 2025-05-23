SELECT DISTINCT
    c.concept_id,
    c.concept_name,
    c.vocabulary_id,
    'Standard' as standard_concept,
    c.concept_code
FROM (SELECT procedure_concept_id FROM `${omopDataset}.procedure_occurrence`
      WHERE procedure_concept_id IS NOT NULL
        AND procedure_concept_id != 0) po
JOIN `${omopDataset}.concept` c ON po.procedure_concept_id = c.concept_id
    AND c.domain_id = 'Procedure'
    AND c.vocabulary_id IN ('LOINC', 'HCPCS')
    AND c.standard_concept = 'S'

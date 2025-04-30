SELECT DISTINCT
    c.concept_id,
    c.concept_name,
    c.vocabulary_id,
    'Standard' as standard_concept,
    c.concept_code
FROM `${omopDataset}.procedure_occurrence` po
JOIN `${omopDataset}.concept` c ON po.procedure_concept_id = c.concept_id
    AND c.domain_id = 'Procedure'
    AND c.vocabulary_id IN ('LOINC', 'HCPCS')
    AND c.standard_concept = 'S'

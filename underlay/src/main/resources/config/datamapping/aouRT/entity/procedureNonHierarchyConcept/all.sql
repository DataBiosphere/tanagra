SELECT DISTINCT
    concept_id,
    concept_name,
    vocabulary_id,
    'Standard' as standard_concept,
    concept_code
FROM `${omopDataset}.procedure_occurrence` po
JOIN `${omopDataset}.concept` c ON po.procedure_concept_id = c.concept_id
    AND c.domain_id = 'Procedure'
    AND c.vocabulary_id = 'LOINC'
    AND c.standard_concept = 'S'
UNION DISTINCT
SELECT DISTINCT
    concept_id,
    concept_name,
    vocabulary_id,
    'Standard' as standard_concept,
    concept_code
FROM `${omopDataset}.procedure_occurrence` po
JOIN `${omopDataset}.concept` c ON po.procedure_concept_id = c.concept_id
    AND c.domain_id = 'Procedure'
    AND c.vocabulary_id = 'HCPCS'
    AND c.standard_concept = 'S'

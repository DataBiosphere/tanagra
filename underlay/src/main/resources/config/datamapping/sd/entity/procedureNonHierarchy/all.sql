SELECT DISTINCT
<<<<<<< HEAD
    c.concept_id,
    c.concept_name,
    c.vocabulary_id,
    'Standard' as standard_concept,
    c.concept_code
=======
    concept_id,
    concept_name,
    vocabulary_id,
    'Standard' as standard_concept,
    concept_code
>>>>>>> bacf7747 (add non-hierarchy for condition. procedure)
FROM `${omopDataset}.procedure_occurrence` po
JOIN `${omopDataset}.concept` c ON po.procedure_concept_id = c.concept_id
    AND c.domain_id = 'Procedure'
    AND c.vocabulary_id IN ('LOINC', 'HCPCS')
    AND c.standard_concept = 'S'

SELECT DISTINCT
    concept_id,
    concept_name,
    vocabulary_id,
    'Standard' as standard_concept,
    concept_code
FROM (SELECT DISTINCT drug_concept_id FROM `${omopDataset}.drug_exposure`) de
JOIN `${omopDataset}.concept` c ON de.drug_concept_id = c.concept_id
    AND c.domain_id = 'Drug'
    AND c.vocabulary_id IN ('CVX', 'HCPCS')
    AND c.standard_concept = 'S'

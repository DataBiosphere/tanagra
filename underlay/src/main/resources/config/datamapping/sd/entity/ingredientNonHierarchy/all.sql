SELECT DISTINCT
    c.concept_id,
    c.concept_name,
    c.vocabulary_id,
    'Standard' as standard_concept,
    c.concept_code
FROM (SELECT DISTINCT drug_concept_id FROM `${omopDataset}.drug_exposure`
      WHERE drug_concept_id IS NOT NULL
        AND drug_concept_id != 0) de
JOIN `${omopDataset}.concept` c ON de.drug_concept_id = c.concept_id
    AND c.domain_id = 'Drug'
    AND c.vocabulary_id IN ('CVX', 'HCPCS')
    AND c.standard_concept = 'S'

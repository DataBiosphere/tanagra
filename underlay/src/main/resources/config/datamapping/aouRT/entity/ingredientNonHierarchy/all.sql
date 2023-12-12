SELECT DISTINCT c.*
FROM `${omopDataset}.drug_exposure` de
JOIN `${omopDataset}.concept` c ON de.drug_concept_id = c.concept_id
    AND c.domain_id = 'Drug'
    AND c.vocabulary_id = 'CVX'
    AND c.standard_concept = 'S'
UNION DISTINCT
SELECT DISTINCT c.*
FROM `${omopDataset}.drug_exposure` de
JOIN `${omopDataset}.concept` c ON de.drug_concept_id = c.concept_id
    AND c.domain_id = 'Drug'
    AND c.vocabulary_id = 'HCPCS'
    AND c.standard_concept = 'S'

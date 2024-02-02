SELECT concept_id, concept_synonym_name
FROM `${omopDataset}.concept_synonym`
WHERE NOT REGEXP_CONTAINS(concept_synonym_name, r'\p{Han}') --remove items with Chinese characters

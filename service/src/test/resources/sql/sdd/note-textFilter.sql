SELECT n.concept_code AS concept_code, n.id AS id, n.name AS name, n.vocabulary AS vocabulary FROM `verily-tanagra-dev.sdstatic_index_020123`.note AS n WHERE CONTAINS_SUBSTR(n.text, 'admis') LIMIT 30

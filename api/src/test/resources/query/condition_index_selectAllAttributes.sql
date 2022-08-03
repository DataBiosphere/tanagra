SELECT c.standard_concept AS standard_concept,
 c.vocabulary AS vocabulary, c.t_display_vocabulary AS t_display_vocabulary,
 c.name AS name,
 c.concept_code AS concept_code,
 c.id AS id

 FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4_indexes`.condition AS c

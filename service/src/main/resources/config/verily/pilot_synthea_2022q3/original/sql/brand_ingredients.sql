/*
   1. User searches for Brand Tylenol
   2. User expands Tylenol to see which ingredients are in Tylenol
   3. Ingredient Acetaminophen is shown

   Acetaminophen has over 300 descendants (eg "acetaminophen 100 MG/ML Oral Suspension"). Acetaminophen's
   concept_class_id is Ingredient; descendants' concept_class_ids are Clinical Drug Comp, Quant Clinical Drug, etc.

   Most descendants have a relationship with Brand Tylenol in concept_relationship. However, we only want to show
   Acetaminophen in step 3. So only keep relationships with concept_class_id=Ingredient.
*/
SELECT cr.*
FROM
    `verily-tanagra-dev.pilot_synthea_2022q3.concept_relationship` cr,
    `verily-tanagra-dev.pilot_synthea_2022q3.concept` c2
WHERE
    cr.concept_id_2 = c2.concept_id AND c2.concept_class_id = 'Ingredient'
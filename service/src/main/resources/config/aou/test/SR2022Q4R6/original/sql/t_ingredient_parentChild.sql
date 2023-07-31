SELECT
    cr.concept_id_1 AS parent,
    cr.concept_id_2 AS child
FROM `all-of-us-ehr-dev.SR2022Q4R6.concept_relationship` cr
JOIN `all-of-us-ehr-dev.SR2022Q4R6.concept` c1 ON c1.concept_id = cr.concept_id_1
JOIN `all-of-us-ehr-dev.SR2022Q4R6.concept` c2 ON c2.concept_id = cr.concept_id_2
WHERE
    c1.concept_id != c2.concept_id
    AND c1.domain_id = 'Drug' AND c2.domain_id = 'Drug'
    AND (

        /*
           Populate Ingredients' ancestors', which are ATCs. See
           https://www.ohdsi.org/web/wiki/doku.php?id=documentation:vocabulary:atc#:~:text=the%20original%20ATC%20hierarchy%20is%20extended%20by%20Standard%20Drug%20Products%20of%20RxNorm%20and%20RxNorm%20Extension%20vocabularies%2C%20which%20are%20assigned%20to%20be%20descendants%20of%20the%205th%20ATC%20Classes.
           Eg:
             ATC 1st (highest ancestor) Subsumes ATC 2nd
             ATC 5th (direct parent of RxNorm) Maps to RxNorm
         */
        (c1.vocabulary_id IN ('ATC') AND cr.relationship_id IN ('Subsumes', 'Maps to'))

        OR

        /*
           Populate Ingredients and their descendants: Branded Drug, Clinical Drug, etc. Ingredients and their
           descendants are RxNorm. drug_exposure contains Ingredients or their descendants.
           Use [1] and [2] to define hierarchy. Eg "Marketed Product" is most specific and is bottom of hierarchy.
           [1] https://www.ohdsi.org/web/wiki/doku.php?id=documentation:cdm:drug_exposure#:~:text=These%20are%20indicated,is%20available%20%E2%80%9CIngredient%E2%80%9D
           [2] https://www.ohdsi.org/web/wiki/doku.php?id=documentation:vocabulary:drug#:~:text=are%20not%20implemented.-,Relationships,-As%20usual%2C%20all
         */
        (
            c1.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
            AND c2.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
            /* Only keep relationships where c1 is "parent" of c2 */
            AND cr.relationship_id IN ('Constitutes', 'Contained in' , 'Has form', 'Has quantified form', 'RxNorm ing of', 'RxNorm inverse is a', 'Precise ing of')
        )

    )

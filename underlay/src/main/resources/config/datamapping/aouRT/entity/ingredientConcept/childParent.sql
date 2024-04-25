/* ATC1 (roots) to ATC2 */
SELECT cr.concept_id_1 AS parent, cr.concept_id_2 AS child
FROM `${omopDataset}.concept_relationship` cr
JOIN `${omopDataset}.concept` c1
    ON c1.concept_id = cr.concept_id_1
    AND c1.VOCABULARY_ID = 'ATC' AND c1.CONCEPT_CLASS_ID = 'ATC 1st' AND c1.STANDARD_CONCEPT = 'C'
JOIN `${omopDataset}.concept` c2
    ON c2.concept_id = cr.concept_id_2
    AND c2.VOCABULARY_ID = 'ATC' AND c2.CONCEPT_CLASS_ID = 'ATC 2nd' AND c2.STANDARD_CONCEPT = 'C'
WHERE cr.relationship_id = 'Subsumes'

UNION ALL

/* ATC2 to ATC3 */
SELECT cr.concept_id_1 AS parent, cr.concept_id_2 AS child
FROM `${omopDataset}.concept_relationship` cr
JOIN `${omopDataset}.concept` c1
    ON c1.concept_id = cr.concept_id_1
    AND c1.VOCABULARY_ID = 'ATC' AND c1.CONCEPT_CLASS_ID = 'ATC 2nd' AND c1.STANDARD_CONCEPT = 'C'
JOIN `${omopDataset}.concept` c2
    ON c2.concept_id = cr.concept_id_2
    AND c2.VOCABULARY_ID = 'ATC' AND c2.CONCEPT_CLASS_ID = 'ATC 3rd' AND c2.STANDARD_CONCEPT = 'C'
WHERE cr.relationship_id = 'Subsumes'

UNION ALL

/* ATC3 to ATC4 */
SELECT cr.concept_id_1 AS parent, cr.concept_id_2 AS child
FROM `${omopDataset}.concept_relationship` cr
JOIN `${omopDataset}.concept` c1
    ON c1.concept_id = cr.concept_id_1
    AND c1.VOCABULARY_ID = 'ATC' AND c1.CONCEPT_CLASS_ID = 'ATC 3rd' AND c1.STANDARD_CONCEPT = 'C'
JOIN `${omopDataset}.concept` c2
    ON c2.concept_id = cr.concept_id_2
    AND c2.VOCABULARY_ID = 'ATC' AND c2.CONCEPT_CLASS_ID = 'ATC 4th' AND c2.STANDARD_CONCEPT = 'C'
WHERE cr.relationship_id = 'Subsumes'

UNION ALL

/* ATC4 to RxNorm ingredient (via shared ATC5 child) */
SELECT c_atc4.concept_id AS parent, c_rxing.concept_id AS child
FROM `${omopDataset}.concept_relationship` cr_atc4_atc5
JOIN `${omopDataset}.concept` c_atc4
    ON c_atc4.concept_id = cr_atc4_atc5.concept_id_1
    AND c_atc4.VOCABULARY_ID = 'ATC' AND c_atc4.CONCEPT_CLASS_ID = 'ATC 4th' AND c_atc4.STANDARD_CONCEPT = 'C'
JOIN `${omopDataset}.concept` c_atc5
    ON c_atc5.concept_id = cr_atc4_atc5.concept_id_2
    AND c_atc5.VOCABULARY_ID = 'ATC' AND c_atc5.CONCEPT_CLASS_ID = 'ATC 5th' AND c_atc5.STANDARD_CONCEPT = 'C'

LEFT JOIN `${omopDataset}.concept_relationship` cr_rxing_atc5
    ON cr_rxing_atc5.concept_id_2 = c_atc5.concept_id
    AND cr_rxing_atc5.relationship_id IN ('RxNorm - ATC name','Mapped from', 'RxNorm - ATC')
JOIN `${omopDataset}.concept` c_rxing
    ON c_rxing.concept_id = cr_rxing_atc5.concept_id_1
    AND c_rxing.VOCABULARY_ID = 'RxNorm' AND c_rxing.CONCEPT_CLASS_ID = 'Ingredient' AND c_rxing.STANDARD_CONCEPT = 'S'

WHERE cr_atc4_atc5.relationship_id = 'Subsumes'

UNION ALL

/* ATC4 to RxNorm ingredient (via RxNorm precise ingredient to shared ATC5 grandchild) */
SELECT c_atc4.concept_id AS parent, c_rxing.concept_id AS child
FROM `${omopDataset}.concept_relationship` cr_atc4_atc5
JOIN `${omopDataset}.concept` c_atc4
    ON c_atc4.concept_id = cr_atc4_atc5.concept_id_1
    AND c_atc4.VOCABULARY_ID = 'ATC' AND c_atc4.CONCEPT_CLASS_ID = 'ATC 4th' AND c_atc4.STANDARD_CONCEPT = 'C'
JOIN `${omopDataset}.concept` c_atc5
    ON c_atc5.concept_id = cr_atc4_atc5.concept_id_2
    AND c_atc5.VOCABULARY_ID = 'ATC' AND c_atc5.CONCEPT_CLASS_ID = 'ATC 5th' AND c_atc5.STANDARD_CONCEPT = 'C'

LEFT JOIN `${omopDataset}.concept_relationship` cr_rxprecing_atc5
    ON cr_rxprecing_atc5.concept_id_2 = c_atc5.concept_id
    AND cr_rxprecing_atc5.relationship_id = 'RxNorm - ATC'
JOIN `${omopDataset}.concept` cr_rxprecing
    ON cr_rxprecing.concept_id = cr_rxprecing_atc5.concept_id_1
    AND cr_rxprecing.VOCABULARY_ID = 'RxNorm' AND cr_rxprecing.CONCEPT_CLASS_ID = 'Precise Ingredient'

LEFT JOIN `${omopDataset}.concept_relationship` cr_rxing_rxprecing
    ON cr_rxing_rxprecing.concept_id_2 = cr_rxprecing.concept_id
    AND cr_rxing_rxprecing.relationship_id = 'Has form'
JOIN `${omopDataset}.concept` c_rxing
    ON c_rxing.concept_id = cr_rxing_rxprecing.concept_id_1
    AND c_rxing.VOCABULARY_ID = 'RxNorm' AND c_rxing.CONCEPT_CLASS_ID = 'Ingredient' and c_rxing.STANDARD_CONCEPT = 'S'
WHERE cr_atc4_atc5.relationship_id = 'Subsumes'

UNION ALL

/* RxNorm ingredient to all descendants */
SELECT ca.ancestor_concept_id AS parent, ca.descendant_concept_id AS child
FROM `${omopDataset}.concept_ancestor` ca
JOIN `${omopDataset}.concept` c1
    ON c1.concept_id = ca.ancestor_concept_id
WHERE c1.vocabulary_id = 'RxNorm'
    AND c1.concept_class_id = 'Ingredient'

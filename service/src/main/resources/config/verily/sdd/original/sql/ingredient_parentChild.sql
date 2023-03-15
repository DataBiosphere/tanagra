/* sd_static doesn't have ingredient hierarchy yet. Use ancillary.drug_hierarchy for now. */
SELECT
    child.concept_id AS child, parent.concept_id AS parent
FROM
    `victr-tanagra-test.ancillary.drug_hierarchy` child,
    `victr-tanagra-test.ancillary.drug_hierarchy` parent
WHERE
    parent.concept_id IS NOT NULL AND
    (parent.type='ATC' OR parent.type='RXNORM') AND
    (child.type='ATC' OR child.type='RXNORM') AND
    parent.id=child.parent_id
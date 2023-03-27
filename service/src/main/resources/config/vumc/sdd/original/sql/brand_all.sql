SELECT *
FROM `victr-tanagra-test.sd_static.concept`
WHERE
        domain_id = 'Drug' AND concept_class_id = 'Brand Name'
  AND vocabulary_id IN ('RxNorm', 'RxNorm Extension')
  AND DATE_DIFF(valid_end_date, CURRENT_DATE(), DAY) > 0
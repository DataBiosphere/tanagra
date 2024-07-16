SELECT io.IND_CODE_SEQ as icd9_occurrence_id, io.IND_SEQ as person_id, pc.id as phewas_id
FROM `${omopDataset}.icd_codes` AS io
         LEFT JOIN `${omopDataset}.phewas_criteria_icd` AS pci
                   ON pci.ICD9 = io.code
         LEFT JOIN (
    SELECT criteria_meta_seq AS id, REGEXP_EXTRACT(label, r'^PHEWAS_([0-9.]+)-') AS code,*
    FROM `${omopDataset}.phewas_criteria`
) pc ON pc.code = pci.icd9
WHERE pc.id IS NOT NULL

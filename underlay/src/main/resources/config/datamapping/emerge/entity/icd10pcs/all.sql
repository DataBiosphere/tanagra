SELECT
    pc.criteria_meta_seq as id,
    case when starts_with(pc.label,'Root ICD10' ) then regexp_extract(pc.name, '^[A-Z0-9-]* (.*)')
         else regexp_extract(pc.name, '.*-(.*)') end as name,
    'ICD10CM' as type,
    case when starts_with(pc.label,'Root ICD10' ) then regexp_extract(pc.name, '^([A-Z0-9-]*) .*')
         else regexp_extract(pc.name, '(.*)-.*') end as concept_code,
    case when starts_with(pc.label,'Root ICD10' ) then
              concat(regexp_extract(pc.name, '^([A-Z0-9-]*) .*'),' ',regexp_extract(pc.name, '^[A-Z0-9-]* (.*)'))
         else concat(regexp_extract(pc.name, '(.*)-.*'),' ',regexp_extract(pc.name, '.*-(.*)')) end as label
FROM `${omopDataset}.icd10_criteria` pc
WHERE pc.parent_seq >= (
        select criteria_meta_seq from `${omopDataset}.icd10_criteria`
        where starts_with(label, 'ICD10PCS')
    )

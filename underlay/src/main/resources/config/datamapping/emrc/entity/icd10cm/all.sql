SELECT
    pc.criteria_meta_seq as id,
    pc.criteria_meta_seq as concept_id,
    case when starts_with(pc.label,'Root ICD10' ) then regexp_extract(pc.name, '^[A-Z0-9-]* (.*)')
         else regexp_extract(pc.name, '.*-(.*)') end as name,
    'ICD10CM' as type,
    'Source' as is_standard,
    case when starts_with(pc.label,'Root ICD10' ) then regexp_extract(pc.name, '^([A-Z0-9-]*) .*')
         else regexp_extract(pc.name, '(.*)-.*') end as concept_code,
    case when starts_with(pc.label,'Root ICD10' ) then
              concat(regexp_extract(pc.name, '^([A-Z0-9-]*) .*'),' ',regexp_extract(pc.name, '^[A-Z0-9-]* (.*)'))
         else concat(regexp_extract(pc.name, '(.*)-.*'),' ',regexp_extract(pc.name, '.*-(.*)')) end as label
FROM `${omopDataset}.icd10_criteria` pc
WHERE not regexp_contains(pc.name, 'PCS') and pc.is_root = false

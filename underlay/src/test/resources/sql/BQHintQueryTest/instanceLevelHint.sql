
    SELECT
        *      
    FROM
        ${ILDH_measurementOccurrence_measurementLoinc}      
    WHERE
        entity_id = @relatedEntityId0      
    ORDER BY
        attribute_name,
        enum_value,
        enum_display,
        enum_count DESC

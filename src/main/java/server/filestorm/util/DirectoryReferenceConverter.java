package server.filestorm.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import server.filestorm.model.type.fileManagement.DirectoryReference;

import com.fasterxml.jackson.databind.ObjectMapper;

@Converter(autoApply = true)
public class DirectoryReferenceConverter implements AttributeConverter<DirectoryReference, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(DirectoryReference directoryReference) {
        try {
            return objectMapper.writeValueAsString(directoryReference);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert DirectoryReference to JSON", e);
        }
    }

    @Override
    public DirectoryReference convertToEntityAttribute(String json) {
        try {
            return objectMapper.readValue(json, DirectoryReference.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to DirectoryReference", e);
        }
    }
}

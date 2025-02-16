package com.angrysurfer.core.config;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class UserConfigConverter {
    private static final Logger logger = LoggerFactory.getLogger(UserConfigConverter.class);
    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;

    public UserConfigConverter() {
        this.jsonMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
    }

    public void convertJsonToXml(String jsonFilePath, String xmlFilePath) {
        try {
            // Read JSON file and convert to object
            File jsonFile = new File(jsonFilePath);
            Object jsonData = jsonMapper.readValue(jsonFile, Object.class);

            // Write object as XML
            File xmlFile = new File(xmlFilePath);
            xmlMapper.writerWithDefaultPrettyPrinter().writeValue(xmlFile, jsonData);

            logger.info("Successfully converted JSON to XML");
        } catch (IOException e) {
            logger.error("Error converting JSON to XML", e);
            throw new RuntimeException("Failed to convert configuration", e);
        }
    }

    public void convertXmlToJson(String xmlFilePath, String jsonFilePath) {
        try {
            // Read XML file and convert to object
            File xmlFile = new File(xmlFilePath);
            Object xmlData = xmlMapper.readValue(xmlFile, Object.class);

            // Write object as JSON
            File jsonFile = new File(jsonFilePath);
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, xmlData);

            logger.info("Successfully converted XML to JSON");
        } catch (IOException e) {
            logger.error("Error converting XML to JSON", e);
            throw new RuntimeException("Failed to convert configuration", e);
        }
    }

    // Utility method to detect file type and convert accordingly
    public void convert(String inputPath, String outputPath) {
        if (inputPath.toLowerCase().endsWith(".json")) {
            convertJsonToXml(inputPath, outputPath);
        } else if (inputPath.toLowerCase().endsWith(".xml")) {
            convertXmlToJson(inputPath, outputPath);
        } else {
            throw new IllegalArgumentException("Unsupported file format");
        }
    }

    // Example usage
    public static void main(String[] args) {
        UserConfigConverter converter = new UserConfigConverter();
        String jsonPath = "C:/Users/MarkP/dev/BeatGeneratorApp/java/swing/beatsui/src/main/java/com/angrysurfer/beatsui/config/beats-config.json";
        String xmlPath = "C:/Users/MarkP/dev/BeatGeneratorApp/java/swing/beatsui/src/main/java/com/angrysurfer/beatsui/config/beats-config.xml";
        converter.convertJsonToXml(jsonPath, xmlPath);

    }
}

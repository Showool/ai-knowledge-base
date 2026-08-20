package com.jason.ai.knowledgebase.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;

class LogbackConfigurationTest {

    @Test
    void logbackConfigurationAlwaysUsesConsoleAndRollingFile() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        ClassPathResource resource = new ClassPathResource("logback-spring.xml");
        Document document;
        try (InputStream input = resource.getInputStream()) {
            document = factory.newDocumentBuilder().parse(input);
        }

        String xml = resource.getContentAsString(StandardCharsets.UTF_8);
        assertThat(document.getElementsByTagName("appender").getLength()).isEqualTo(2);
        assertThat(document.getElementsByTagName("springProfile").getLength()).isZero();
        assertThat(xml).contains("appender-ref ref=\"CONSOLE\"")
                .contains("appender-ref ref=\"FILE\"")
                .contains("SizeAndTimeBasedRollingPolicy")
                .doesNotContain("OpenTelemetryAppender");
    }
}

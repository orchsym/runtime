package org.apache.nifi.i18n;

import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.nar.i18n.MessagesProvider;
import org.apache.nifi.web.api.dto.AllowableValueDTO;
import org.apache.nifi.web.api.dto.DocumentedTypeDTO;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.apache.nifi.web.api.dto.PropertyDescriptorDTO;
import org.apache.nifi.web.api.dto.RelationshipDTO;
import org.apache.nifi.web.api.dto.ReportingTaskDTO;
import org.apache.nifi.web.api.entity.AllowableValueEntity;

/**
 * @author GU Guoqiang
 *
 */
public class DtoI18nHelper {

    public static void fix(final Locale requestLocale, DocumentedTypeDTO dto) {
        if (requestLocale == null) {
            return;
        }
        final String type = dto.getType();
        final String description = MessagesProvider.getDescription(requestLocale, type);
        if (StringUtils.isNotBlank(description)) {
            dto.setDescription(description);
        }
        if (dto.isPreview()) {
            String previewMessage = MessagesProvider.getFrameworkValue(requestLocale, "Components.Preview.Message");
            String oldDesc = dto.getDescription();

            String previewDesc = "!!! " + previewMessage + " !!!  ";
            if (StringUtils.isNotEmpty(oldDesc)) {
                previewDesc += oldDesc;
            }
            dto.setDescription(previewDesc);
        }

        // tags
        final Set<String> tagsSet = MessagesProvider.getTagsSet(requestLocale, type);
        if (tagsSet != null && !tagsSet.isEmpty()) {
            dto.setTags(tagsSet);
        }

        // marks
        Set<String> marksCategories = MessagesProvider.getMarksCategoriesSet(requestLocale, type);
        if (marksCategories == null || marksCategories.isEmpty()) {
            // same as Category.getLabel, will try to load the messages.properties(en) by default;
            marksCategories = MessagesProvider.getMarksCategoriesSet(Locale.ENGLISH, type);
        }
        if (marksCategories != null && !marksCategories.isEmpty()) {
            dto.setCategories(marksCategories); // update
        }
        String marksVendor = MessagesProvider.getMarksVendor(requestLocale, type);
        if (StringUtils.isBlank(marksVendor)) {
            marksVendor = MessagesProvider.getMarksVendor(Locale.ENGLISH, type);
        }
        if (!StringUtils.isBlank(marksVendor)) {
            dto.setVendor(marksVendor);
        }

    }

    public static void fix(final Locale requestLocale, final String type, PropertyDescriptorDTO descriptor) {
        if (requestLocale == null) {
            return;
        }
        final String propName = descriptor.getName();

        final String propDisplayName = MessagesProvider.getPropDisplayName(requestLocale, type, propName);
        if (StringUtils.isNotBlank(propDisplayName)) {
            descriptor.setDisplayName(propDisplayName);
        }
        final String propDesc = MessagesProvider.getPropDesc(requestLocale, type, propName);
        if (StringUtils.isNotBlank(propDesc)) {
            descriptor.setDescription(propDesc);
        }

        String scopeDescription = MessagesProvider.getFrameworkValue(requestLocale, MessagesProvider.getELScopeDescKey("NoScope"));
        final String expressionLanguageScope = descriptor.getExpressionLanguageScope();
        if (StringUtils.isNotBlank(expressionLanguageScope)) {
            scopeDescription = MessagesProvider.getFrameworkValue(requestLocale, MessagesProvider.getELScopeDescKey(expressionLanguageScope));
        }
        descriptor.setExpressionLanguageScope(scopeDescription); // change to i18n

        final List<AllowableValueEntity> allowableValues = descriptor.getAllowableValues();
        if (allowableValues != null && allowableValues.size() > 0) {
            for (AllowableValueEntity entry : allowableValues) {
                final AllowableValueDTO allowableValue = entry.getAllowableValue();
                if (allowableValue != null) {
                    final String value = allowableValue.getValue();

                    final String allowableValueDisplayName = MessagesProvider.getAllowableValueDisplayName(requestLocale, type, propName, value);
                    if (StringUtils.isNotBlank(allowableValueDisplayName)) {
                        allowableValue.setDisplayName(allowableValueDisplayName);
                    }

                    final String allowableValueDesc = MessagesProvider.getAllowableValueDesc(requestLocale, type, propName, value);
                    if (StringUtils.isNotBlank(allowableValueDesc)) {
                        allowableValue.setDescription(allowableValueDesc);
                    }
                }
            }
        }
    }

    public static void fix(final Locale requestLocale, ProcessorDTO processor) {
        if (requestLocale == null) {
            return;
        }

        final String type = processor.getType();

        final String description = MessagesProvider.getDescription(requestLocale, type);
        if (StringUtils.isNotBlank(description)) {
            processor.setDescription(description);
        }

        if (processor.getRelationships() != null)
            for (RelationshipDTO relation : processor.getRelationships()) {
                final String relationshipDesc = MessagesProvider.getRelationshipDesc(requestLocale, type, relation.getName());
                if (StringUtils.isNotBlank(relationshipDesc)) {
                    relation.setDescription(relationshipDesc);
                }
            }

        if (processor.getConfig() != null && processor.getConfig().getDescriptors() != null)
            for (Entry<String, PropertyDescriptorDTO> entry : processor.getConfig().getDescriptors().entrySet())
                fix(requestLocale, type, entry.getValue());

    }

    public static void fix(final Locale requestLocale, ReportingTaskDTO reportTask) {
        if (requestLocale == null) {
            return;
        }

        final String type = reportTask.getType();
        if (reportTask.getDescriptors() != null)
            for (Entry<String, PropertyDescriptorDTO> entry : reportTask.getDescriptors().entrySet())
                fix(requestLocale, type, entry.getValue());
    }
}

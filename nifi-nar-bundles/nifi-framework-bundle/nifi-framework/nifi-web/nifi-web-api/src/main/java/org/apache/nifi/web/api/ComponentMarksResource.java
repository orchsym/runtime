/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.web.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.nifi.components.ComponentsContext;
import org.apache.nifi.i18n.DtoI18nHelper;
import org.apache.nifi.i18n.Messages;
import org.apache.nifi.web.NiFiServiceFacade;
import org.apache.nifi.web.api.dto.DocumentedTypeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.sourceforge.pinyin4j.PinyinHelper;

/**
 * RESTful endpoint for retrieving system diagnostics.
 */
@Path(ComponentMarksResource.PATH)
@Api(value = ComponentMarksResource.PATH, //
        description = "Endpoint for accessing components marks.")
public class ComponentMarksResource extends ApplicationResource {
    private static final Logger logger = LoggerFactory.getLogger(ComponentMarksResource.class);
    public static final String PATH = "/component-marks";

    enum Category {
        DATA_PROCESS, CONVERT_CONTROL, DATABASE, FILE_PROCESS, NETWORK, BIG_DATA, EXECUTE,

        DATA_FETCH, DATA_OUTPUT, MESSAGE_EVENT, DATA_CONVERT, PROCESS_CONTROL, COMMUNICATION, CLOUD, FILE, LOG,;

        public String getLabel(Locale locale) {
            return Messages.getString(locale, "Components.Category." + this.name());
        }

        public static Category get(Locale locale, String label) {
            for (Category c : Category.values()) {
                if (label.equals(c.getLabel(locale))) {
                    return c;
                }
            }
            return null;
        }
    }

    private NiFiServiceFacade serviceFacade;

    private static final Map<Category, String> levelOneIconMap;
    private static final Map<Category, String> levelTwoIconMap;
    private static final Map<Category, CategoryItem> CATEGORY_BUILT_IN_ITEMS;
    private static final Map<Locale, List<CategoryItem>> localeItemsMap = new Hashtable<>();

    static {
        levelOneIconMap = new HashMap<>();
        levelOneIconMap.put(Category.DATA_PROCESS, "001.svg");
        levelOneIconMap.put(Category.CONVERT_CONTROL, "002.svg");
        levelOneIconMap.put(Category.DATABASE, "003.svg");
        levelOneIconMap.put(Category.NETWORK, "004.svg");
        levelOneIconMap.put(Category.FILE_PROCESS, "005.svg");
        levelOneIconMap.put(Category.BIG_DATA, "006.svg");
        levelOneIconMap.put(Category.EXECUTE, "007.svg");

        levelTwoIconMap = new HashMap<>();
        levelTwoIconMap.put(Category.DATA_FETCH, "01.svg");
        levelTwoIconMap.put(Category.DATA_OUTPUT, "03.svg");
        levelTwoIconMap.put(Category.MESSAGE_EVENT, "02.svg");
        levelTwoIconMap.put(Category.DATA_CONVERT, "04.svg");
        levelTwoIconMap.put(Category.PROCESS_CONTROL, "05.svg");
        levelTwoIconMap.put(Category.DATABASE, "06.svg");
        levelTwoIconMap.put(Category.COMMUNICATION, "08.svg");
        levelTwoIconMap.put(Category.CLOUD, "09.svg");
        levelTwoIconMap.put(Category.FILE, "10.svg");
        levelTwoIconMap.put(Category.LOG, "11.svg");
        levelTwoIconMap.put(Category.BIG_DATA, "12.svg");
        levelTwoIconMap.put(Category.EXECUTE, "13.svg");

        CATEGORY_BUILT_IN_ITEMS = loadCategoryBuiltInItems();
    }

    private static Map<Category, CategoryItem> loadCategoryBuiltInItems() {
        Map<Category, CategoryItem> items = new LinkedHashMap<>();
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final InputStream stream = ComponentMarksResource.class.getResourceAsStream("/json/classification.json");
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(stream);
            parser.nextToken();
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                CategoryItem item = mapper.readValue(parser, CategoryItem.class);
                final Category cat = Category.valueOf(item.name);
                if (cat == null) {
                    logger.error("Can't find the matched category: " + item.name);
                    continue;
                }
                items.put(cat, item);

            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return items;
    }

    private Map<String, CategoryItem> copyBuiltInItems(final Locale requestLocale) {
        return CATEGORY_BUILT_IN_ITEMS.values().stream().map(item -> {
            CategoryItem ci = new CategoryItem();
            ci.icon = item.icon;
            final Category cat = Category.valueOf(item.name);
            if (cat != null) {
                ci.name = cat.getLabel(requestLocale);
            } else {
                ci.name = item.name;
            }
            ci.classification = item.classification.stream().map(subItem -> {
                SubCategoryItem sci = new SubCategoryItem();
                sci.icon = subItem.icon;
                final Category subCat = Category.valueOf(subItem.name);
                if (cat != null) {
                    sci.name = subCat.getLabel(requestLocale);
                } else {
                    sci.name = subItem.name;
                }
                sci.components = new ArrayList<>(subItem.components);
                return sci;
            }).collect(Collectors.toList());
            return ci;
        }).collect(Collectors.toMap(t -> t.name, Function.identity()));
    }

    // private void authorizeSystem() {
    // FIXME, don't check the auth, should be available always

    // serviceFacade.authorizeAccess(lookup -> {
    // final Authorizable system = lookup.getSystem();
    // system.authorize(authorizer, RequestAction.READ, NiFiUserUtils.getNiFiUser());
    // });
    // }

    /**
     * Gets the components marks for this NiFi instance.
     *
     * @return A marks entity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(//
            value = "Gets the components marks for the system is running on", //
            response = MarkEntity.class //
    )
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = StatsResource.CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = StatsResource.CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = StatsResource.CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = StatsResource.CODE_MESSAGE_409) //
    })
    public Response getComponentsMarks(//
            @QueryParam("type") final String filterType, //
            @QueryParam("name") final String filterName, //
            @QueryParam("vendor") final String filterVendor//
    ) {
        // authorizeSystem();

        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }

        final Locale requestLocale = this.getRequestLocale();

        // create the response
        Set<DocumentedTypeDTO> processorTypes = serviceFacade.getProcessorTypes(null, null, null);
        Set<DocumentedTypeDTO> serviceTypes = serviceFacade.getControllerServiceTypes(null, null, null, null, null, null, null);

        if (requestLocale != null) {
            processorTypes.forEach(dto -> DtoI18nHelper.fix(requestLocale, dto));
            serviceTypes.forEach(dto -> DtoI18nHelper.fix(requestLocale, dto));
        }

        List<MarkEntity> processorMarks = filterAndCreateMarkEntities(processorTypes, "processor", filterType, filterName, filterVendor);
        List<MarkEntity> serviceMarks = filterAndCreateMarkEntities(serviceTypes, "controllerService", filterType, filterName, filterVendor);

        List<MarkEntity> result = new ArrayList<>();
        result.addAll(processorMarks);
        result.addAll(serviceMarks);
        Collections.sort(result, entityComparator);

        // generate the response
        Gson gson = new Gson();
        String resultStr = gson.toJson(result);
        return Response.ok(resultStr).build();
    }

    /**
     * Gets the components classification for this NiFi instance.
     *
     * @return A marks entity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/classification")
    @ApiOperation(//
            value = "Gets the components classification for the system is running on", //
            response = MarkEntity.class //
    )
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = StatsResource.CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = StatsResource.CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = StatsResource.CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = StatsResource.CODE_MESSAGE_409) //
    })
    public Response getComponentsClassification() {
        // authorizeSystem();

        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }

        final boolean enableCompPreview = ComponentsContext.isPreviewEnabled();

        final Locale requestLocale = this.getRequestLocale();
        List<CategoryItem> items = localeItemsMap.get(requestLocale);
        if (items == null) {
            synchronized (logger) {

                Set<DocumentedTypeDTO> processorTypes = serviceFacade.getProcessorTypes(null, null, null);
                Set<DocumentedTypeDTO> processorTypesWithCategories = processorTypes.stream()//
                        .filter(dto -> dto.getCategories() != null && dto.getCategories().size() > 0)//
                        .filter(dto -> {
                            // filter for component palette
                            if (enableCompPreview) {
                                return true;
                            }
                            return !ComponentsContext.isPreviewType(dto.getType());
                        }).collect(Collectors.toSet());

                if (requestLocale != null)
                    processorTypesWithCategories.forEach(dto -> DtoI18nHelper.fix(requestLocale, dto));

                final Collection<CategoryItem> categoryItems = createCategoryItems(requestLocale, processorTypesWithCategories);

                // merge
                items = mergeBuiltIn(requestLocale, categoryItems);

                final List<String> loadedCompList = processorTypes.stream().map(d -> ComponentsContext.FUN_TYPE_NAME.apply(d.getType())).collect(Collectors.toList());
                for (CategoryItem item : items) {
                    item.classification.forEach(sub -> {

                        final List<String> components = sub.components;
                        // filter, if unloaded
                        final Iterator<String> iterator = components.iterator();
                        while (iterator.hasNext()) {
                            final String name = iterator.next();
                            if (!loadedCompList.contains(name)) {
                                iterator.remove();
                            }
                        }
                    });
                }

                // sort
                Comparator<Item> itemComparator = itemGeneraleComparator;
                if (Locale.CHINESE.getLanguage().equals(requestLocale.getLanguage())) {
                    itemComparator = itemZhComparator;
                }
                items.sort(itemComparator);
                for (CategoryItem item : items) {
                    item.classification.sort(itemComparator);
                    item.classification.forEach(c -> Collections.sort(c.components));
                }

                localeItemsMap.put(requestLocale, items);
            }

        }

        // generate the response
        Gson gson = new Gson();
        String resultStr = gson.toJson(items);
        return Response.ok(resultStr).build();
    }

    private Collection<CategoryItem> createCategoryItems(final Locale requestLocale, Set<DocumentedTypeDTO> processorTypes) {
        Map<String, CategoryItem> categoryItems = new HashMap<String, CategoryItem>();
        for (DocumentedTypeDTO dto : processorTypes) {

            for (String category : dto.getCategories()) {
                String[] categoryArr = category.split("/");
                String nameIndex1 = categoryArr.length > 0 ? categoryArr[0] : "";
                String nameIndex2 = categoryArr.length > 1 ? categoryArr[1] : "";
                CategoryItem categoryItem = categoryItems.get(nameIndex1);
                if (categoryItem == null) {
                    categoryItem = new CategoryItem();
                    categoryItem.name = nameIndex1;
                    final Category cat = Category.get(requestLocale, nameIndex1);
                    if (cat != null)
                        categoryItem.icon = levelOneIconMap.get(cat);
                }
                List<SubCategoryItem> classification = categoryItem.classification;
                int index = indexForName(classification, nameIndex2);
                SubCategoryItem subCategoryItem = null;
                if (index == -1) {
                    subCategoryItem = new SubCategoryItem();
                    subCategoryItem.name = nameIndex2;
                    final Category cat = Category.get(requestLocale, nameIndex2);
                    if (cat != null)
                        subCategoryItem.icon = levelTwoIconMap.get(cat);
                    classification.add(subCategoryItem);
                } else {
                    subCategoryItem = classification.get(index);
                }
                subCategoryItem.components.add(ComponentsContext.FUN_TYPE_NAME.apply(dto.getType()));
                categoryItems.put(nameIndex1, categoryItem);
            }
        }
        return categoryItems.values();
    }

    private List<CategoryItem> mergeBuiltIn(final Locale requestLocale, Collection<CategoryItem> categoryItems) {
        final Map<String, CategoryItem> builtInItems = copyBuiltInItems(requestLocale);

        // merge
        categoryItems.stream().filter(c -> builtInItems.containsKey(c.name)).forEach(item -> {
            final CategoryItem biItem = builtInItems.get(item.name);
            if (biItem != null) {
                if (biItem.icon == null) {
                    biItem.icon = item.icon;
                }

                final Map<String, SubCategoryItem> biSubItemsMap = biItem.classification.stream().collect(Collectors.toMap(s -> s.name, Function.identity()));

                // merge
                item.classification.stream().filter(c -> biSubItemsMap.containsKey(c.name)).forEach(c -> {
                    SubCategoryItem biSubItem = biSubItemsMap.get(c.name);
                    if (biSubItem != null) {
                        if (biSubItem.icon == null) {
                            biSubItem.icon = c.icon;
                        }
                        biSubItem.components.addAll(c.components);
                    } else {
                        biItem.classification.add(c); // add directly
                    }
                });

                // add others
                biItem.classification.addAll(item.classification.stream().filter(c -> !biSubItemsMap.containsKey(c.name)).collect(Collectors.toList()));

            } else {
                builtInItems.put(item.name, item); // add directly
            }
        });

        // add others
        categoryItems.stream().filter(c -> !builtInItems.containsKey(c.name)).forEach(c -> builtInItems.put(c.name, c));

        return builtInItems.values().stream().collect(Collectors.toList());
    }

    private List<MarkEntity> filterAndCreateMarkEntities(Set<DocumentedTypeDTO> dtos, String componentType, String filterType, String filterName, String filterVendor) {
        List<MarkEntity> marksEntities = new ArrayList<>();
        if (filterType != null && !componentType.equalsIgnoreCase(filterType)) {
            return marksEntities;
        }
        Pattern namePattern = null;
        if (filterName != null && !filterName.isEmpty()) {
            namePattern = Pattern.compile(filterName.replace("*", ".*"), Pattern.CASE_INSENSITIVE | Pattern.CANON_EQ);// convert to regexp
        }
        for (DocumentedTypeDTO dto : dtos) {

            String type = dto.getType();
            String name = type;
            final int packageIndex = type.lastIndexOf('.');
            if (packageIndex > 0) {
                name = name.substring(packageIndex + 1);
            }
            if (filterName != null && !filterName.trim().isEmpty()) {
                if (filterName.contains("*") && namePattern != null) { // with pattern
                    final Matcher nameMatcher = namePattern.matcher(name);
                    if (!nameMatcher.find()) {
                        continue;
                    }
                } else if (!name.equalsIgnoreCase(filterName)) {
                    continue;
                }
            }
            if (filterVendor != null && !dto.getVendor().equalsIgnoreCase(filterVendor)) {
                continue;
            }

            MarkEntity entity = new MarkEntity();
            entity.name = name;
            entity.type = componentType;
            entity.createdDate = dto.getCreatedDate();
            entity.vendor = dto.getVendor();
            entity.categories = dto.getCategories();
            entity.note = dto.getNote();
            marksEntities.add(entity);
        }
        return marksEntities;
    }

    int indexForName(List<SubCategoryItem> array, String name) {
        int index = -1;
        for (int i = 0; i < array.size(); i++) {
            SubCategoryItem item = array.get(i);
            if (item.name.equals(name)) {
                index = i;
                break;
            }
        }
        return index;
    }

    // setters

    public void setServiceFacade(NiFiServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    static Comparator<MarkEntity> entityComparator = new Comparator<MarkEntity>() {
        @Override
        public int compare(MarkEntity s1, MarkEntity s2) {
            final int typeComparator = s1.type.compareToIgnoreCase(s2.type);
            if (typeComparator != 0) {
                return typeComparator;
            }
            return s1.name.compareTo(s2.name);
        }
    };

    static class MarkEntity {
        public String type = ""; // "processor" or "controllerService"
        public String vendor = "";
        public String createdDate = "";
        public Set<String> categories;
        public String name = "";
        public String note = "";
    }

    static Comparator<Item> itemGeneraleComparator = new Comparator<Item>() {
        @Override
        public int compare(Item s1, Item s2) {
            return s1.name.compareToIgnoreCase(s2.name);
        }
    };

    static Comparator<Item> itemZhComparator = new Comparator<Item>() {
        @Override
        public int compare(Item s1, Item s2) {
            int first = compare(s1.name.charAt(0), s2.name.charAt(0));
            if (first != 0) {
                return first;
            }

            // check the second
            if (s1.name.length() > 1 && s2.name.length() > 1) {
                return compare(s1.name.charAt(1), s2.name.charAt(1));
            }
            return itemGeneraleComparator.compare(s1, s2);
        }

        int compare(char c1, char c2) {
            final String[] c1Py = PinyinHelper.toHanyuPinyinStringArray(c1);
            final String[] c2Py = PinyinHelper.toHanyuPinyinStringArray(c2);
            if (c1Py != null && c2Py != null) {
                final String c1PyStr = Arrays.stream(c1Py).collect(Collectors.joining(""));
                final String c2PyStr = Arrays.stream(c2Py).collect(Collectors.joining(""));
                return c1PyStr.compareToIgnoreCase(c2PyStr);
            }
            return String.valueOf(c1).compareToIgnoreCase(String.valueOf(c2));
        }
    };

    static class Item {
        public String icon;
        public String name;
    }

    static class CategoryItem extends Item {
        public List<SubCategoryItem> classification = new ArrayList<>();

    }

    static class SubCategoryItem extends Item {
        public List<String> components = new ArrayList<>();;
    }
}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.nifi.authorization.Authorizer;
import org.apache.nifi.authorization.RequestAction;
import org.apache.nifi.authorization.resource.Authorizable;
import org.apache.nifi.authorization.user.NiFiUserUtils;
import org.apache.nifi.web.NiFiServiceFacade;
import org.apache.nifi.web.api.dto.DocumentedTypeDTO;

import com.google.gson.Gson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.sourceforge.pinyin4j.PinyinHelper;

/**
 * RESTful endpoint for retrieving system diagnostics.
 */
@Path("/component-marks")
@Api(value = "/component-marks", description = "Endpoint for accessing components marks.")
public class ComponentMarksResource extends ApplicationResource {

    // first level index
    public static final String FIRST_DATA_PROCESS = "数据处理";
    public static final String FIRST_TRANSFORM_CONTROL = "转换控制";
    public static final String FIRST_DATA_BASE = "数据库";
    public static final String FIRST_FILE_PROCESS = "文件处理";
    public static final String FIRST_NETWORK = "网络";
    public static final String FIRST_BIG_DATA = "大数据";
    public static final String FIRST_EXECUTE = "执行";
    // second level index
    public static final String SECNOD_DATA_FETCH = "数据抓取";
    public static final String SECNOD_DATA_OUTPUT = "数据输出";
    public static final String SECNOD_MESSAGE_EVENT = "消息事件";
    public static final String SECNOD_DATA_TRANSFORM = "数据转换";
    public static final String SECNOD_PROCESS_CONTROL = "流程控制";
    public static final String SECNOD_DATA_BASE = "数据库";
    public static final String SECNOD_TELE_COMMUNICATION = "网络通信";
    public static final String SECNOD_CLOUD = "云";
    public static final String SECNOD_FILE = "文件";
    public static final String SECNOD_LOG = "日志";
    public static final String SECNOD_BIG_DATA = "大数据";
    public static final String SECNOD_EXECUTE = "执行";

    private NiFiServiceFacade serviceFacade;
    private Authorizer authorizer;

    private Map<String, String> levelOneIconMap;
    private Map<String, String> levelTwoIconMap;

    private void authorizeSystem() {
        serviceFacade.authorizeAccess(lookup -> {
            final Authorizable system = lookup.getSystem();
            system.authorize(authorizer, RequestAction.READ, NiFiUserUtils.getNiFiUser());
        });
    }

    private void makeupIconMap() {
        if (levelOneIconMap == null) {
            levelOneIconMap = new HashMap<>();
            levelOneIconMap.put(FIRST_DATA_PROCESS, "001.svg");
            levelOneIconMap.put(FIRST_TRANSFORM_CONTROL, "002.svg");
            levelOneIconMap.put(FIRST_DATA_BASE, "003.svg");
            levelOneIconMap.put(FIRST_NETWORK, "004.svg");
            levelOneIconMap.put(FIRST_FILE_PROCESS, "005.svg");
            levelOneIconMap.put(FIRST_BIG_DATA, "006.svg");
            levelOneIconMap.put(FIRST_EXECUTE, "007.svg");
        }
        if (levelTwoIconMap == null) {
            levelTwoIconMap = new HashMap<>();
            levelTwoIconMap.put(SECNOD_DATA_FETCH, "01.svg");
            levelTwoIconMap.put(SECNOD_DATA_OUTPUT, "03.svg");
            levelTwoIconMap.put(SECNOD_MESSAGE_EVENT, "02.svg");
            levelTwoIconMap.put(SECNOD_DATA_TRANSFORM, "04.svg");
            levelTwoIconMap.put(SECNOD_PROCESS_CONTROL, "05.svg");
            levelTwoIconMap.put(SECNOD_DATA_BASE, "06.svg");
            levelTwoIconMap.put(SECNOD_TELE_COMMUNICATION, "08.svg");
            levelTwoIconMap.put(SECNOD_CLOUD, "09.svg");
            levelTwoIconMap.put(SECNOD_FILE, "10.svg");
            levelTwoIconMap.put(SECNOD_LOG, "11.svg");
            levelTwoIconMap.put(SECNOD_BIG_DATA, "12.svg");
            levelTwoIconMap.put(SECNOD_EXECUTE, "13.svg");
        }
    }

    /**
     * Gets the components marks for this NiFi instance.
     *
     * @return A marks entity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the components marks for the system is running on", response = MarkEntity.class)
    public Response getComponentsMarks(@QueryParam("type") final String filterType, @QueryParam("name") final String filterName, @QueryParam("vendor") final String filterVendor) {

        authorizeSystem();
        // create the response
        Set<DocumentedTypeDTO> processorTypes = serviceFacade.getProcessorTypes(null, null, null);
        Set<DocumentedTypeDTO> serviceTypes = serviceFacade.getControllerServiceTypes(null, null, null, null, null, null, null);

        ArrayList<MarkEntity> processorMarks = getMarkEntities(processorTypes, "processor", filterType, filterName, filterVendor);
        ArrayList<MarkEntity> serviceMarks = getMarkEntities(serviceTypes, "controllerService", filterType, filterName, filterVendor);

        ArrayList<MarkEntity> result = new ArrayList<>();
        result.addAll(processorMarks);
        result.addAll(serviceMarks);

        // generate the response
        Collections.sort(result, ComponentMarksResource.getCompByName());
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
    @ApiOperation(value = "Gets the components classification for the system is running on", response = MarkEntity.class)
    public Response getComponentsClassification() {

        makeupIconMap();
        authorizeSystem();
        // create the response
        Set<DocumentedTypeDTO> processorTypes = serviceFacade.getProcessorTypes(null, null, null);
        Map<String, CategoryItem> categoryItems = new HashMap<String, CategoryItem>();
        for (DocumentedTypeDTO dto : processorTypes) {
            String type = dto.getType();
            String[] typeArr = type.split("\\.");
            String processorName = typeArr.length > 0 ? typeArr[typeArr.length - 1] : "";
            Set<String> categories = dto.getCategories();
            if (categories == null) {
                continue;
            }
            for (String category : categories) {
                String[] categoryArr = category.split("/");
                String nameIndex1 = categoryArr.length > 0 ? categoryArr[0] : "";
                String nameIndex2 = categoryArr.length > 1 ? categoryArr[1] : "";
                CategoryItem categoryItem = categoryItems.get(nameIndex1);
                if (categoryItem == null) {
                    categoryItem = new CategoryItem();
                    categoryItem.name = nameIndex1;
                    categoryItem.icon = levelOneIconMap.get(nameIndex1);
                }
                ArrayList<SubCategoryItem> classification = categoryItem.classification;
                int index = indexForName(classification, nameIndex2);
                SubCategoryItem subCategoryItem = null;
                if (index == -1) {
                    subCategoryItem = new SubCategoryItem();
                    subCategoryItem.name = nameIndex2;
                    subCategoryItem.icon = levelTwoIconMap.get(nameIndex2);
                    classification.add(subCategoryItem);
                } else {
                    subCategoryItem = classification.get(index);
                }
                subCategoryItem.components.add(processorName);
                categoryItems.put(nameIndex1, categoryItem);
            }
        }

        // sort
        CategoryItem[] items = categoryItems.values().toArray(new CategoryItem[0]);
        Arrays.sort(items, new Comparator<CategoryItem>() {
            @Override
            public int compare(CategoryItem o1, CategoryItem o2) {
                return compareName(o1.name, o2.name);
            }
        });
        for (CategoryItem item : items) {
            ArrayList<SubCategoryItem> classification = item.classification;
            SubCategoryItem[] subCategoryItems = classification.toArray(new SubCategoryItem[0]);
            Arrays.sort(subCategoryItems, new Comparator<SubCategoryItem>() {
                @Override
                public int compare(SubCategoryItem o1, SubCategoryItem o2) {
                    return compareName(o1.name, o2.name);
                }
            });
            item.classification = new ArrayList<>(Arrays.asList((subCategoryItems)));
            for (SubCategoryItem subItem : item.classification) {
                Collections.sort(subItem.components);
            }
        }
        // generate the response
        Gson gson = new Gson();
        String resultStr = gson.toJson(items);
        return Response.ok(resultStr).build();
    }

    private ArrayList<MarkEntity> getMarkEntities(Set<DocumentedTypeDTO> dtos, String componentType, String filterType, String filterName, String filterVendor) {

        ArrayList<MarkEntity> marksEntities = new ArrayList<>();
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

    public static Comparator<MarkEntity> getCompByName() {
        Comparator<MarkEntity> comp = new Comparator<MarkEntity>() {
            @Override
            public int compare(MarkEntity s1, MarkEntity s2) {
                return s1.name.compareTo(s2.name);
            }
        };
        return comp;
    }

    int compareName(String name1, String name2) {
        char c1 = name1.charAt(0);
        char c2 = name2.charAt(0);
        return concatPinyinStringArray(PinyinHelper.toHanyuPinyinStringArray(c1)).compareTo(concatPinyinStringArray(PinyinHelper.toHanyuPinyinStringArray(c2)));
    }

    String concatPinyinStringArray(String[] pinyinArray) {
        StringBuffer pinyinSbf = new StringBuffer();
        if ((pinyinArray != null) && (pinyinArray.length > 0)) {
            for (int i = 0; i < pinyinArray.length; i++) {
                pinyinSbf.append(pinyinArray[i]);
            }
        }
        return pinyinSbf.toString();
    }

    int indexForName(ArrayList<SubCategoryItem> array, String name) {
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

    public void setAuthorizer(Authorizer authorizer) {
        this.authorizer = authorizer;
    }

    class MarkEntity {
        public String type = ""; // "processor" or "controllerService"
        public String vendor = "";
        public String createdDate = "";
        public Set<String> categories;
        public String name = "";
        public String note = "";
    }

    class CategoryItem {
        public String icon;
        public String name;
        public ArrayList<SubCategoryItem> classification = new ArrayList<>();

    }

    class SubCategoryItem {
        public String icon;
        public String name;
        public ArrayList<String> components = new ArrayList<>();;
    }
}

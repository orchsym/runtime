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
package org.apache.nifi.annotation.documentation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be applied to a {@link org.apache.nifi.processor.Processor Processor},
 * {@link org.apache.nifi.controller.ControllerService ControllerService}, or
 * {@link org.apache.nifi.reporting.ReportingTask ReportingTask} in order to
 * associate tags (keywords) with the component. These tags do not affect the
 * component in any way but serve as additional documentation and can be used to
 * sort/filter Processors.
 *
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Marks {

    String vendor() default "Orchsym"; //eg: "Orchsym", "BaishanCloud"
    String[] categories() default ""; //eg:  {"数据处理/数据抓取", "网络/网络通信"}
    String createdDate() default ""; //eg: "2018-9-20"
    String note() default ""; //备注，扩展字段
 }

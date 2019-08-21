/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.registry.api;

import java.util.ArrayList;
import java.util.Map; 

public class APISpec {

	public String swagger = "2.0";
 	public InfoSpec info;
 	public String host;
 	public ArrayList<String> schemes;
 	public String basePath;
 	// public ArrayList<String> consumes;
 	public ArrayList<String> produces;

 	/*          uri        method         */
 	public Map<String, Map<String, PathSpec>> paths;

 	/*   model name,    properties,     */
 	public Map<String, Map<String, Map<String, PropertySpec>>> definitions;
}

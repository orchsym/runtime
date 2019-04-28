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

public interface ICodeMessages {

    String CODE_MESSAGE_400 = "Studio was unable to complete the request because it was invalid. The request should not be retried without modification.";
    String CODE_MESSAGE_401 = "Client could not be authenticated.";
    String CODE_MESSAGE_403 = "Client is not authorized to make this request.";
    String CODE_MESSAGE_404 = "The specified resource could not be found.";
    String CODE_MESSAGE_409 = "The request was valid but Studio was not in the appropriate state to process it. Retrying the same request later may be successful.";

}

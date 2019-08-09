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
package org.apache.nifi.web.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.nifi.controller.FlowController;
import org.apache.nifi.web.NiFiServiceFacade;
import org.apache.nifi.web.api.dto.PositionDTO;
import org.apache.nifi.web.api.entity.ProcessGroupEntity;

/**
 * @author GU Guoqiang
 *
 */
public final class PositionCalcUtil {

    static final int SIZE_IN_LINE = 10;
    static final int GAP = 20;

    // the module size about (400,180) with gap 20
    static final Integer STEP_X = 400 + GAP;
    static final Integer STEP_Y = 180 + GAP;

    public static PositionDTO newAvailablePosition(final NiFiServiceFacade serviceFacade) {
        final Set<ProcessGroupEntity> processGroups = serviceFacade.getProcessGroups(FlowController.ROOT_GROUP_ID_ALIAS);

        final List<PositionDTO> positions = processGroups.stream()//
                .filter(e -> e.getComponent() != null && e.getComponent().getPosition() != null)//
                .map(e -> e.getComponent().getPosition())//
                .collect(Collectors.toList());

        return newAvailablePosition(positions);
    }

    public static PositionDTO newAvailablePosition(final List<PositionDTO> positions) {
        PositionDTO position = new PositionDTO(0.0, 0.0);

        // one line one group
        final Map<Double, List<PositionDTO>> lineGroup = positions.stream()//
                .map(p -> {
                    // get the similar position
                    Integer x = new Double(Math.round(p.getX() / STEP_X)).intValue() * STEP_X;
                    Integer y = new Double(Math.round(p.getY() / STEP_Y)).intValue() * STEP_Y;

                    return new PositionDTO(x.doubleValue(), y.doubleValue());
                })//
                .collect(Collectors.groupingBy(PositionDTO::getY));

        // sorted line
        final List<Double> lineList = lineGroup.keySet().stream().sorted().collect(Collectors.toList());

        if (lineList.size() > 0) {
            final Double lastLineY = lineList.get(lineList.size() - 1);

            final List<PositionDTO> lastLinePositions = lineGroup.get(lastLineY);
            if (null != lastLinePositions && !lastLinePositions.isEmpty() && lastLinePositions.size() < SIZE_IN_LINE) {
                final List<PositionDTO> sortedPositions = lastLinePositions.stream().sorted((p1, p2) -> p1.getX().compareTo(p2.getX())).collect(Collectors.toList());

                final PositionDTO lastPosition = sortedPositions.get(sortedPositions.size() - 1);
                position = nextRight(lastPosition);
            } else { // new line
                position.setY(lastLineY + STEP_Y);
            }
        }
        return position;
    }

    public static PositionDTO nextRight(PositionDTO base) {
        final PositionDTO position = new PositionDTO(0.0, 0.0);
        if (null != base) {
            position.setX(base.getX() + STEP_X);
            position.setY(base.getY());
        }

        return position;
    }

    public static PositionDTO nextDown(PositionDTO base) {
        final PositionDTO position = new PositionDTO(0.0, 0.0);
        if (null != base) {
            position.setX(base.getX());
            position.setY(base.getY() + STEP_Y);
        }

        return position;
    }
}

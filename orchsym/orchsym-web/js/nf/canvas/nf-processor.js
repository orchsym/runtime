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

/* global define, module, require, exports */

(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define(['jquery',
                'd3',
                'nf.Connection',
                'nf.Common',
                'nf.Client',
                'nf.ClusterSummary',
                'nf.CanvasUtils'],
            function ($, d3, nfConnection, nfCommon, nfClient, nfClusterSummary, nfCanvasUtils) {
                return (nf.Processor = factory($, d3, nfConnection, nfCommon, nfClient, nfClusterSummary, nfCanvasUtils));
            });
    } else if (typeof exports === 'object' && typeof module === 'object') {
        module.exports = (nf.Processor =
            factory(require('jquery'),
                require('d3'),
                require('nf.Connection'),
                require('nf.Common'),
                require('nf.Client'),
                require('nf.ClusterSummary'),
                require('nf.CanvasUtils')));
    } else {
        nf.Processor = factory(root.$,
            root.d3,
            root.nf.Connection,
            root.nf.Common,
            root.nf.Client,
            root.nf.ClusterSummary,
            root.nf.CanvasUtils);
    }
}(this, function ($, d3, nfConnection, nfCommon, nfClient, nfClusterSummary, nfCanvasUtils) {
    'use strict';

    var nfConnectable;
    var nfDraggable;
    var nfSelectable;
    var nfQuickSelect;
    var nfContextMenu;

    var PREVIEW_NAME_LENGTH = 25;

    // default dimensions for each type of component
    var dimensions = {
        width: 350,
        height: 130
    };

    var smallDimensions = {
        width: 100,
        height: 88
    };

    // ---------------------------------
    // processors currently on the graph
    // ---------------------------------

    var processorMap;

    // -----------------------------------------------------------
    // cache for components that are added/removed from the canvas
    // -----------------------------------------------------------

    var removedCache;
    var addedCache;

    // --------------------
    // component containers
    // --------------------

    var processorContainer;

    // --------------------------
    // privately scoped functions
    // --------------------------

    /**
     * Selects the processor elements against the current processor map.
     */
    var select = function () {
        return processorContainer.selectAll('g.processor').data(processorMap.values(), function (d) {
            return d.id;
        });
    };

    var getDragSelection = function () {
        var dragSelection = d3.select('rect.drag-selection');
        // lazily create the drag selection box
        if (dragSelection.empty()) {
            // get the current selection
            var selection = d3.selectAll('g.component.selected');

            // determine the appropriate bounding box
            var minX = null, maxX = null, minY = null, maxY = null;
            selection.each(function (d) {
                var ele = d3.select(this)
                var smallView = ele.select('g.processor-canvas-small-processor');
                if (minX === null || d.position.x < minX) {
                    minX = d.position.x;
                }
                if (minY === null || d.position.y < minY) {
                    minY = d.position.y;
                }
                if(smallView.empty()) {
                    var componentMaxX = d.position.x + d.dimensions.width;
                    var componentMaxY = d.position.y + d.dimensions.height;
                }else {
                    var componentMaxX = d.position.x + 100;
                    var componentMaxY = d.position.y + 100;
                }
                if (maxX === null || componentMaxX > maxX) {
                    maxX = componentMaxX;
                }
                if (maxY === null || componentMaxY > maxY) {
                    maxY = componentMaxY;
                }
            });
            // create a selection box for the move
            d3.select('#canvas').append('rect')
                .attr('rx', 6)
                .attr('ry', 6)
                .attr('x', minX)
                .attr('y', minY)
                .attr('class', 'drag-selection')
                .attr('pointer-events', 'none')
                .attr('width', maxX - minX)
                .attr('height', maxY - minY)
                .attr('stroke-width', function () {
                    return 1 / nfCanvasUtils.getCanvasScale();
                })
                .attr('stroke-dasharray', function () {
                    return 4 / nfCanvasUtils.getCanvasScale();
                })
                .datum({
                    original: {
                        x: minX,
                        y: minY
                    },
                    x: minX,
                    y: minY
                });
        } else {
            // update the position of the drag selection
            dragSelection.attr('x', function (d) {
                d.x += d3.event.dx;
                return d.x;
            })
                .attr('y', function (d) {
                    d.y += d3.event.dy;
                    return d.y;
                });
        }
    }

    var updateComponentsPosition = function (dragSelection) {
        var updates = d3.map();

        // determine the drag delta
        var dragData = dragSelection.datum();
        var delta = {
            x: dragData.x - dragData.original.x,
            y: dragData.y - dragData.original.y
        };

        // if the component didn't move, return
        // if (delta.x === 0 && delta.y === 0) {
        //     return;
        // }

        var selectedConnections = d3.selectAll('g.connection.selected');
        var selectedComponents = d3.selectAll('g.component.selected');

        // ensure every component is writable
        if (nfCanvasUtils.canModify(selectedConnections) === false || nfCanvasUtils.canModify(selectedComponents) === false) {
            nfDialog.showOkDialog({
                headerText: nf._.msg('nf-draggable.ComponentPosition'),
                dialogContent: nf._.msg('nf-draggable.Message2')
            });
            return;
        }

        // go through each selected connection
        selectedConnections.each(function (d) {
            var connectionUpdate = nfDraggable.updateConnectionPosition(d, delta);
            if (connectionUpdate !== null) {
                updates.set(d.id, connectionUpdate);
            }
        });
        // go through each selected component
        selectedComponents.each(function (d) {
            // consider any self looping connections
            var connections = nfConnection.getComponentConnections(d.id);
            $.each(connections, function (_, connection) {
                if (!updates.has(connection.id) && nfCanvasUtils.getConnectionSourceComponentId(connection) === nfCanvasUtils.getConnectionDestinationComponentId(connection)) {
                    var connectionUpdate = nfDraggable.updateConnectionPosition(nfConnection.get(connection.id), delta);
                    if (connectionUpdate !== null) {
                        updates.set(connection.id, connectionUpdate);
                    }
                }
            });

            // consider the component itself
            updates.set(d.id, nfDraggable.updateComponentPosition(d, delta));
        });

        nfDraggable.refreshConnections(updates);
    };

    var paintSmallProcessor = function (processor, processorData) {
        // processor border
        processor.append('rect')
            .attrs({
                'class': 'border',
                'width': function (d) {
                    return smallDimensions.width;
                },
                'height': function (d) {
                    return smallDimensions.height;
                },
                'fill': 'transparent',
                'stroke': 'transparent'
            });
        //processor body
        processor.append('rect')
            .attrs({
                'class': 'body',
                'width': function (d) {
                    return smallDimensions.width;
                },
                'height': function (d) {
                    return smallDimensions.height;
                },
                'filter': 'url(#component-drop-shadow)',
                'stroke-width': 0
            });
        var details = processor.append('g').attr('class', 'processor-canvas-details');
        var smallView = processor.append('g').attr('class', 'processor-canvas-small-processor');
        // circle
        processor.append('circle')
            .attrs({
                'r': 30,
                'cx': 50,
                'cy': 44,
                'fill': '#506773',
            })

        // processor icon
        processor.append('text')
            .attrs({
                'x': 35,
                'y': 55,
                'fill': '#FFFFFF',
                'class': 'processor-icon',
            })
            .text('\ue826');

        // run status icon
        processor.append('text')
            .attrs({
                'class': 'run-status-icon',
                'x': 10,
                'y': 20
            });

        // tooltip icon
        processor.append('text')
            .attrs({
                'class': 'io-tooltip-icon',
                'x': 11,
                'y': 75,
                'fill': '#5BB85D',
                'font-family': 'FontAwesome',
            })
            .text('\uf192');


       
        // processor name
        processor.append('text')
            .attrs({
                'x': 50,
                'y': 110,
                'width': 200,
                'height': 14,
                'class': 'processor-name',
                'style': 'text-anchor: middle;',
            });
        // bulletin background
        processor.append('rect')
            .attrs({
                'class': 'bulletin-background',
                'x': function (d) {
                    return 100 - 24;
                },
                'y': function(d) {
                    return 88 - 24;
                },
                'width': 24,
                'height': 24
            });

        // bulletin icon
        processor.append('text')
            .attrs({
                'class': 'bulletin-icon',
                'x': function (d) {
                    return 100 - 17;
                },
                'y': function (d) {
                    return 88 - 7;
                },
            })
            .text('\uf24a');

        // processor disabled modal
        processor.append('rect')
            .attrs({
                'class': 'processor-disabled-modal',
                'width': function (d) {
                    return smallDimensions.width;
                },
                'height': function (d) {
                    return smallDimensions.height;
                },
                'filter': 'url(#component-drop-shadow)',
                'stroke-width': 0,
                'opacity': '0.4',
                'fill': '#CCCCCC',
                'style': 'display:none',
            });

         //自定义展开按钮
        processor.append('text')
            .attrs({
                'class': 'processor-expand',
                'x': 80,
                'y': 20,
                'style': 'font-size:20px'
            })
            .text('+')
            .on('click',function(d){
                processor.selectAll('*').remove()
                paintBigProcessor(processor, processorData)
                var processorEntities = processor.datum();
                nfProcessor.set(processorEntities);
                getDragSelection();
                var dragSelection = d3.select('rect.drag-selection');
                if (dragSelection.empty()) {
                    return;
                }
                updateComponentsPosition(dragSelection);
                dragSelection.remove();
            });
    // -------------------------------------------------------------------------------------------------------------------------------------------------------
    };

    var paintBigProcessor = function(processor, processorData) {
        // processor border
        processor.append('rect')
            .attrs({
                'class': 'border',
                'width': function (d) {
                    return dimensions.width;
                },
                'height': function (d) {
                    return dimensions.height;
                },
                'fill': 'transparent',
                'stroke': 'transparent'
            });
        //processor body
        processor.append('rect')
            .attrs({
                'class': 'body',
                'width': function (d) {
                    return dimensions.width;
                },
                'height': function (d) {
                    return dimensions.height;
                },
                'filter': 'url(#component-drop-shadow)',
                'stroke-width': 0
            });

            // processor name
            processor.append('text')
                .attrs({
                    'x': 75,
                    'y': 18,
                    'width': 230,
                    'height': 14,
                    'class': 'processor-name'
                });

            // processor icon container
            processor.append('rect')
                .attrs({
                    'x': 0,
                    'y': 0,
                    'width': 50,
                    'height': 48,
                    'class': 'processor-icon-container'
                });

            // processor icon
            processor.append('text')
                .attrs({
                    'x': 9,
                    'y': 35,
                    'class': 'processor-icon'
                })
                .text('\ue826');
            var details = processor.append('g').attr('class', 'processor-canvas-details');
            var bigView = processor.append('g').attr('class', 'processor-canvas-big-processor');
            // run status icon
            details.append('text')
                .attrs({
                    'class': 'run-status-icon',
                    'x': 55,
                    'y': 23
                });

            // processor type
            details.append('text')
                .attrs({
                    'class': 'processor-type',
                    'x': 75,
                    'y': 32,
                    'width': 230,
                    'height': 12
                });

            // processor type
            details.append('text')
                .attrs({
                    'class': 'processor-bundle',
                    'x': 75,
                    'y': 45,
                    'width': 200,
                    'height': 12
                });

            // draw the processor statistics table

            // in
            details.append('rect')
                .attrs({
                    'width': function () {
                        return dimensions.width;
                    },
                    'height': 19,
                    'x': 0,
                    'y': 50,
                    'fill': '#f4f6f7',
                });

            // border
            details.append('rect')
                .attrs({
                    'width': function () {
                        return dimensions.width;
                    },
                    'height': 1,
                    'x': 0,
                    'y': 68,
                    'fill': '#c7d2d7',
                });

            // read/write
            details.append('rect')
                .attrs({
                    'width': function () {
                        return dimensions.width;
                    },
                    'height': 19,
                    'x': 0,
                    'y': 69,
                    'fill': '#ffffff',
                });

            // border
            details.append('rect')
                .attrs({
                    'width': function () {
                        return dimensions.width;
                    },
                    'height': 1,
                    'x': 0,
                    'y': 87,
                    'fill': '#c7d2d7',
                });

            // out
            details.append('rect')
                .attrs({
                    'width': function () {
                        return dimensions.width;
                    },
                    'height': 20,
                    'x': 0,
                    'y': 88,
                    'fill': '#f4f6f7',
                });

            // border
            details.append('rect')
                .attrs({
                    'width': function () {
                        return dimensions.width;
                    },
                    'height': 1,
                    'x': 0,
                    'y': 106,
                    'fill': '#c7d2d7',
                });

            // tasks/time
            details.append('rect')
                .attrs({
                    'width': function () {
                        return dimensions.width;
                    },
                    'height': 19,
                    'x': 0,
                    'y': 107,
                    'fill': '#ffffff',
                });

            // stats label container
            var processorStatsLabel = details.append('g')
                .attrs({
                    'transform': 'translate(10, 55)',
                });

            // in label
            processorStatsLabel.append('text')
                .attrs({
                    'width': 73,
                    'height': 10,
                    'y': 9,
                    'class': 'stats-label'
                })
                .text(nf._.msg('nf-processor.In'));

            // read/write label
            processorStatsLabel.append('text')
                .attrs({
                    'width': 73,
                    'height': 10,
                    'y': 27,
                    'class': 'stats-label'
                })
                .text(nf._.msg('nf-processor.ReadWrite'));

            // out label
            processorStatsLabel.append('text')
                .attrs({
                    'width': 73,
                    'height': 10,
                    'y': 46,
                    'class': 'stats-label'
                })
                .text(nf._.msg('nf-processor.Out'));

            // tasks/time label
            processorStatsLabel.append('text')
                .attrs({
                    'width': 73,
                    'height': 10,
                    'y': 65,
                    'class': 'stats-label'
                })
                .text(nf._.msg('nf-processor.TasksTime'));

            // stats value container
            var processorStatsValue = details.append('g')
                .attrs({
                    'transform': 'translate(85, 55)',
                });

            // in value
            var inText = processorStatsValue.append('text')
                .attrs({
                    'width': 180,
                    'height': 9,
                    'y': 9,
                    'class': 'processor-in stats-value'
                });

            // in count
            inText.append('tspan')
                .attrs({
                    'class': 'count'
                });

            // in size
            inText.append('tspan')
                .attrs({
                    'class': 'size'
                });

            // read/write value
            processorStatsValue.append('text')
                .attrs({
                    'width': 180,
                    'height': 10,
                    'y': 27,
                    'class': 'processor-read-write stats-value'
                });

            // out value
            var outText = processorStatsValue.append('text')
                .attrs({
                    'width': 180,
                    'height': 10,
                    'y': 46,
                    'class': 'processor-out stats-value'
                });

            // out count
            outText.append('tspan')
                .attrs({
                    'class': 'count'
                });

            // out size
            outText.append('tspan')
                .attrs({
                    'class': 'size'
                });

            // tasks/time value
            processorStatsValue.append('text')
                .attrs({
                    'width': 180,
                    'height': 10,
                    'y': 65,
                    'class': 'processor-tasks-time stats-value'
                });

            // stats value container
            var processorStatsInfo = details.append('g')
                .attrs({
                    'transform': 'translate(305, 55)',
                    'style': 'display:none'});

            // in info
            processorStatsInfo.append('text')
                .attrs({
                    'width': 25,
                    'height': 10,
                    'y': 9,
                    'class': 'stats-info'
                })
                .text(nf._.msg('nf-processor.5min-1'));

            // read/write info
            processorStatsInfo.append('text')
                .attrs({
                    'width': 25,
                    'height': 10,
                    'y': 27,
                    'class': 'stats-info'
                })
                .text(nf._.msg('nf-processor.5min-1'));

            // out info
            processorStatsInfo.append('text')
                .attrs({
                    'width': 25,
                    'height': 10,
                    'y': 46,
                    'class': 'stats-info'
                })
                .text(nf._.msg('nf-processor.5min-1'));

            // tasks/time info
            processorStatsInfo.append('text')
                .attrs({
                    'width': 25,
                    'height': 10,
                    'y': 65,
                    'class': 'stats-info'
                })
                .text('5 min');
            // --------
            // comments
            // --------

            details.append('path')
                .attrs({
                    'class': 'component-comments',
                    'transform': 'translate(' + (dimensions.width - 2) + ', ' + (dimensions.height - 10) + ')',
                    'd': 'm0,0 l0,8 l-8,0 z'
                });

            // active thread count
            // details.append('text')
            //     .attrs({
            //         'class': 'active-thread-count-icon',
            //         'y': 46
            //     })
            //     .text('\ue840');

            // active thread background
            details.append('text')
                .attrs({
                    'class': 'active-thread-count',
                    'y': 46
                });


            // bulletin background
            details.append('rect')
                .attrs({
                    'class': 'bulletin-background',
                    'x': function (d) {
                        return dimensions.width - 24;
                    },
                    'y': function(d) {
                        return dimensions.height - 24;
                    },
                    'width': 24,
                    'height': 24
                });

            // bulletin icon
            details.append('text')
                .attrs({
                    'class': 'bulletin-icon',
                    'x': function (d) {
                        return dimensions.width - 17;
                    },
                    'y': function (d) {
                        return dimensions.height - 7;
                    },
                })
                .text('\uf24a');

            // processor disabled modal
            processor.append('rect')
                .attrs({
                    'class': 'processor-disabled-modal',
                    'width': function (d) {
                        return dimensions.width;
                    },
                    'height': function (d) {
                        return dimensions.height;
                    },
                    'filter': 'url(#component-drop-shadow)',
                    'stroke-width': 0,
                    'opacity': '0.4',
                    'fill': '#CCCCCC',
                    'style': 'display:none',
                });

            //自定义收缩按钮
            processor.append('text')
                .attrs({
                    'class': 'processor-collapse',
                    'x': 319,
                    'y': 30,
                    'style': 'font-size:34px;'
                })
                .text('-')
                .on('click',function(d){
                    processor.selectAll('*').remove()
                    paintSmallProcessor(processor, processorData)
                    var processorEntities = processor.datum();
                    nfProcessor.set(processorEntities);
                    // get the drag selection
                    getDragSelection()
                    var dragSelection = d3.select('rect.drag-selection');
                    if (dragSelection.empty()) {
                        return;
                    }
                    updateComponentsPosition(dragSelection);
                    dragSelection.remove();
                });
            // // select
            // var selection = select();
            // // enter
            // var entered = renderProcessors(selection.enter(), false);
            // // update
            // var updated = selection.merge(entered);
            // updated.call(updateProcessors);
    // --------------------------------------------------------------------------------------------------------------------------------
    };

    /**
     * Renders the processors in the specified selection.
     *
     * @param {selection} entered           The selection of processors to be rendered
     * @param {boolean} selected             Whether the element should be selected
     * @return the entered selection
     */
    var renderProcessors = function (entered, selected) {
        if (entered.empty()) {
            return entered;
        }
        var processor = entered.append('g')
            .attrs({
                'id': function (d) {
                    return 'id-' + d.id;
                },
                'class': 'processor component'
            })
            // .classed('selected', selected)
            .call(nfCanvasUtils.position);


        // restricted icon background
        processor.append('circle')
            .attrs({
                'r': 9,
                'cx': 12,
                'cy': 12,
                'class': 'restricted-background'
            });

        // restricted icon
        processor.append('text')
            .attrs({
                'x': 7.75,
                'y': 17,
                'class': 'restricted'
            })
            .text('\uf132');

        // is primary icon background
        processor.append('circle')
            .attrs({
                'r': 9,
                'cx': 38,
                'cy': 36,
                'class': 'is-primary-background'
            });

        // is primary icon
        processor.append('text')
            .attrs({
                'x': 34.75,
                'y': 40,
                'class': 'is-primary'
            })
            .text('P')
            .append('title').text(function (d) {
                return 'This component is only scheduled to execute on the Primary Node';
            });

        // make processors selectable
        processor.call(nfSelectable.activate).call(nfContextMenu.activate).call(nfQuickSelect.activate);

        return processor;
    };

    /**
     * Updates the processors in the specified selection.
     *
     * @param {selection} updated       The processors to update
     */
    var updateProcessors = function (updated, index) {
        if (updated.empty()) {
            return;
        }
        // processor border authorization
        updated.select('rect.border')
            .classed('unauthorized', function (d) {
                return d.permissions.canRead === false;
            })
            .classed('ghost', function (d) {
                return d.permissions.canRead === true && d.component.extensionMissing === true;
            });

        // processor body authorization
        updated.select('rect.body')
            .classed('unauthorized', function (d) {
                return d.permissions.canRead === false;
            });

        updated.each(function (processorData) {
            var processor = d3.select(this);
            var details = processor.select('g.processor-canvas-details');

            // update the component behavior as appropriate
            nfCanvasUtils.editable(processor, nfConnectable, nfDraggable);
            // if this processor is visible, render everything
            if (processor.classed('visible') || true) {
                if (details.empty()) {
                    details = processor.append('g').attr('class', 'processor-canvas-details');
                    var bigView = processor.select('g.processor-canvas-big-processor');
                    if(bigView.empty()) {
                        processor.selectAll('*').remove()
                        paintSmallProcessor(processor, processorData)
                    } else {
                        processor.selectAll('*').remove()
                        paintBigProcessor(processor, processorData)
                    }
                }

                if (processorData.permissions.canRead) {
                    // update the processor name

                    processor.select('text.processor-name')
                        .each(function (d) {
                            var processorName = d3.select(this);

                            // reset the processor name to handle any previous state
                            processorName.text(null).selectAll('title').remove();
                            var name = d.component.name
                            if (!processor.select('text.processor-expand').empty() && d.component.type) {
                                var arr = d.component.type.split('.')
                                var typeName = arr[arr.length -1]
                                if (typeName !== name) {
                                    name = name + '(' + typeName + ')'
                                }
                            }
                            // apply ellipsis to the processor name as necessary
                            nfCanvasUtils.ellipsis(processorName, d.component.name);
                        }).append('title').text(function (d) {
                            var name = d.component.name
                            if (!processor.select('text.processor-expand').empty() && d.component.type) {
                                var arr = d.component.type.split('.')
                                var typeName = arr[arr.length -1]
                                if (typeName !== name) {
                                    name = name + '(' + typeName + ')'
                                }
                            }
                            return d.component.name
                        });

                    // update the processor type
                    processor.select('text.processor-type')
                        .each(function (d) {
                            var processorType = d3.select(this);

                            // reset the processor type to handle any previous state
                            processorType.text(null).selectAll('title').remove();

                            // apply ellipsis to the processor type as necessary
                            nfCanvasUtils.ellipsis(processorType, nfCommon.formatType(d.component));
                        }).append('title').text(function (d) {
                            return nfCommon.formatType(d.component);
                        });

                    // update the processor bundle
                    /**processor.select('text.processor-bundle')
                        .each(function (d) {
                            var processorBundle = d3.select(this);

                            // reset the processor type to handle any previous state
                            processorBundle.text(null).selectAll('title').remove();

                            // apply ellipsis to the processor type as necessary
                            nfCanvasUtils.ellipsis(processorBundle, nfCommon.formatBundle(d.component.bundle));
                        }).append('title').text(function (d) {
                            return nfCommon.formatBundle(d.component.bundle);
                        });
                    **/
                    // update the processor comments
                    processor.select('path.component-comments')
                        .style('visibility', nfCommon.isBlank(processorData.component.config.comments) ? 'hidden' : 'visible')
                        .each(function () {
                            // get the tip
                            var tip = d3.select('#comments-tip-' + processorData.id);

                            // if there are validation errors generate a tooltip
                            if (nfCommon.isBlank(processorData.component.config.comments)) {
                                // remove the tip if necessary
                                if (!tip.empty()) {
                                    tip.remove();
                                }
                            } else {
                                // create the tip if necessary
                                if (tip.empty()) {
                                    tip = d3.select('#processor-tooltips').append('div')
                                        .attr('id', function () {
                                            return 'comments-tip-' + processorData.id;
                                        })
                                        .attr('class', 'tooltip nifi-tooltip');
                                }

                                // update the tip
                                tip.text(processorData.component.config.comments);

                                // add the tooltip
                                nfCanvasUtils.canvasTooltip(tip, d3.select(this));
                            }
                        });
                } else {
                    // clear the processor name
                    processor.select('text.processor-name').text(null);

                    // clear the processor type
                    processor.select('text.processor-type').text(null);

                    // clear the processor bundle
                    processor.select('text.processor-bundle').text(null);

                    // clear the processor comments
                    processor.select('path.component-comments').style('visibility', 'hidden');

                    // clear tooltips
                    processor.call(removeTooltips);
                }

                // populate the stats
                processor.call(updateProcessorStatus);
            } else {
                if (processorData.permissions.canRead) {
                    // update the processor name
                    processor.select('text.processor-name')
                        .text(function (d) {
                            var name = d.component.name;
                            if (name.length > PREVIEW_NAME_LENGTH) {
                                return name.substring(0, PREVIEW_NAME_LENGTH) + String.fromCharCode(8230);
                            } else {
                                return name;
                            }
                        });
                } else {
                    // clear the processor name
                    processor.select('text.processor-name').text(null);
                }

                // remove the tooltips
                processor.call(removeTooltips);

                // remove the details if necessary
                if (!details.empty()) {
                    details.remove();
                }
            }

            // ---------------
            // processor color
            // ---------------

            //update the processor icon container
            processor.select('rect.processor-icon-container').classed('unauthorized', !processorData.permissions.canRead);

            //update the processor icon
            processor.select('text.processor-icon').classed('unauthorized', !processorData.permissions.canRead);

            //update the processor border
            processor.select('rect.border').classed('unauthorized', !processorData.permissions.canRead);

            // use the specified color if appropriate
            if (processorData.permissions.canRead) {
                if (nfCommon.isDefinedAndNotNull(processorData.component.style['background-color'])) {
                    var color = processorData.component.style['background-color'];

                    //update the processor icon container
                    processor.select('rect.processor-icon-container')
                        .style('fill', function (d) {
                            return color;
                        });

                    //update the processor border
                    processor.select('rect.border')
                        .style('stroke', function (d) {
                            return color;
                        });
                }
            }

            // update the processor color
            processor.select('text.processor-icon')
                .style('fill', function (d) {

                    // get the default color
                    var color = nfProcessor.defaultIconColor();

                    if (!d.permissions.canRead) {
                        return color;
                    }

                    // use the specified color if appropriate
                    if (nfCommon.isDefinedAndNotNull(d.component.style['background-color'])) {
                        color = d.component.style['background-color'];

                        //special case #ffffff implies default fill
                        if (color.toLowerCase() === '#ffffff') {
                            color = nfProcessor.defaultIconColor();
                        } else {
                            color = nfCommon.determineContrastColor(
                                nfCommon.substringAfterLast(
                                    color, '#'));
                        }
                    }

                    return color;
                });

            // restricted component indicator
            processor.select('circle.restricted-background').style('visibility', showRestricted);
            processor.select('text.restricted').style('visibility', showRestricted);

            // is primary component indicator
            processor.select('circle.is-primary-background').style('visibility', showIsPrimary);
            processor.select('text.is-primary').style('visibility', showIsPrimary);
        });
    };

    /**
     * Returns whether the resticted indicator should be shown for a given component
     * @param d
     * @returns {*}
     */
    var showRestricted = function (d) {
        if (!d.permissions.canRead) {
            return 'hidden';
        }
        return d.component.restricted ? 'visible' : 'hidden';
    };

    /**
     * Returns whether the is primary indicator should be shown for a given component
     * @param d
     * @returns {*}
     */
    var showIsPrimary = function (d) {
        return nfClusterSummary.isClustered() && d.status.aggregateSnapshot.executionNode === 'PRIMARY' ? 'visible' : 'hidden';
    };

    /**
     * Updates the stats for the processors in the specified selection.
     *
     * @param {selection} updated           The processors to update
     */
    var updateProcessorStatus = function (updated) {
        if (updated.empty()) {
            return;
        }

        updated.select('rect.processor-disabled-modal')
            .attrs({
                'style': function (d) {
                    if(d.status.aggregateSnapshot.runStatus === 'Disabled') {
                        return 'display:block'
                    } else {
                        return 'display:none'
                    }
                }
            })

        updated.select('text.io-tooltip-icon')
            .each(function(d) {
                // get the tip
                var tip = d3.select('#processor-io-tip-' + d.id);

                // if there are validation errors generate a tooltip
                if ((d.permissions.canRead && !nfCommon.isEmpty(d.component.validationErrors)) || true) {
                    // create the tip if necessary
                    if (tip.empty()) {
                        tip = d3.select('#processor-tooltips').append('div')
                            .attr('id', function () {
                                return 'processor-io-tip-' + d.id;
                            })
                            .attr('class', 'tooltip nifi-tooltip');
                    }

                    // update the tip
                    tip.html(function () {
                        $html = ''
                        if(d.component && d.component.type) {
                        var arr = d.component.type.split('.')
                        var typeName = arr[arr.length -1]

                        var $html = '<span class="io-tooltip-information">'+
                                    '<span style="display:block;font-weight:bold;font-size:14px;margin-bottom:5px">'+ typeName +'</span>'+
                                    '<li>'+
                                    '<span>'+nf._.msg('nf-processor.In')+':</span><span>'+nfCommon.substringBeforeFirst(d.status.aggregateSnapshot.input, ' ')+' ' + nfCommon.substringAfterFirst(d.status.aggregateSnapshot.input, ' ')+'</span>'+
                                    '</li>'+
                                    '<li>'+
                                    '<span>'+nf._.msg('nf-processor.ReadWrite')+':</span><span>'+d.status.aggregateSnapshot.read + ' / ' + d.status.aggregateSnapshot.written+'</span>'+
                                    '</li>'+
                                    '<li>'+
                                    '<span>'+nf._.msg('nf-processor.Out')+':</span><span>'+nfCommon.substringBeforeFirst(d.status.aggregateSnapshot.output, ' ')+' ' + nfCommon.substringAfterFirst(d.status.aggregateSnapshot.output, ' ')+'</span>'+
                                    '</li>'+
                                    '<li>'+
                                    '<span>'+nf._.msg('nf-processor.TasksTime')+':</span><span>'+d.status.aggregateSnapshot.tasks + ' / ' + d.status.aggregateSnapshot.tasksDuration+'</span>'+
                                    '</li>'+
                                    '</span>';

                        return $('<div></div>').append($html).html();
                    }
                    });
                    // add the tooltip
                    nfCanvasUtils.canvasTooltip(tip, d3.select(this));
                } else {
                    // remove the tip if necessary
                    if (!tip.empty()) {
                        tip.remove();
                    }
                }
            })


        // update the run status
        updated.select('text.run-status-icon')
            .attrs({
                'fill': function (d) {
                    var fill = '#728e9b';

                    if (d.status.aggregateSnapshot.runStatus === 'Invalid') {
                        fill = '#cf9f5d';
                    } else if (d.status.aggregateSnapshot.runStatus === 'Running') {
                        fill = '#7dc7a0';
                    } else if (d.status.aggregateSnapshot.runStatus === 'Stopped') {
                        fill = '#d18686';
                    }

                    return fill;
                },
                'font-family': function (d) {
                    var family = 'FontAwesome';
                    if (d.status.aggregateSnapshot.runStatus === 'Disabled') {
                        family = 'flowfont';
                    }
                    return family;
                }
            })
            .text(function (d) {
                var img = '';
                if (d.status.aggregateSnapshot.runStatus === 'Disabled') {
                    img = '\ue806';
                } else if (d.status.aggregateSnapshot.runStatus === 'Invalid') {
                    img = '\uf071';
                } else if (d.status.aggregateSnapshot.runStatus === 'Running') {
                    img = '\uf04b';
                } else if (d.status.aggregateSnapshot.runStatus === 'Stopped') {
                    img = '\uf04d';
                }
                return img;
            })
            .each(function (d) {
                // get the tip
                var tip = d3.select('#run-status-tip-' + d.id);

                // if there are validation errors generate a tooltip
                if (d.permissions.canRead && !nfCommon.isEmpty(d.component.validationErrors)) {
                    // create the tip if necessary
                    if (tip.empty()) {
                        tip = d3.select('#processor-tooltips').append('div')
                            .attr('id', function () {
                                return 'run-status-tip-' + d.id;
                            })
                            .attr('class', 'tooltip nifi-tooltip');
                    }

                    // update the tip
                    tip.html(function () {
                        var list = nfCommon.formatUnorderedList(d.component.validationErrors);
                        if (list === null || list.length === 0) {
                            return '';
                        } else {
                            return $('<div></div>').append(list).html();
                        }
                    });

                    // add the tooltip
                    nfCanvasUtils.canvasTooltip(tip, d3.select(this));
                } else {
                    // remove the tip if necessary
                    if (!tip.empty()) {
                        tip.remove();
                    }
                }
            });

        // in count value
        updated.select('text.processor-in tspan.count')
            .text(function (d) {
                return nfCommon.substringBeforeFirst(d.status.aggregateSnapshot.input, ' ');
            });

        // in size value
        updated.select('text.processor-in tspan.size')
            .text(function (d) {
                return ' ' + nfCommon.substringAfterFirst(d.status.aggregateSnapshot.input, ' ');
            });

        // read/write value
        updated.select('text.processor-read-write')
            .text(function (d) {
                return d.status.aggregateSnapshot.read + ' / ' + d.status.aggregateSnapshot.written;
            });

        // out count value
        updated.select('text.processor-out tspan.count')
            .text(function (d) {
                return nfCommon.substringBeforeFirst(d.status.aggregateSnapshot.output, ' ');
            });

        // out size value
        updated.select('text.processor-out tspan.size')
            .text(function (d) {
                return ' ' + nfCommon.substringAfterFirst(d.status.aggregateSnapshot.output, ' ');
            });

        // tasks/time value
        updated.select('text.processor-tasks-time')
            .text(function (d) {
                return d.status.aggregateSnapshot.tasks + ' / ' + d.status.aggregateSnapshot.tasksDuration;
            });

        updated.each(function (d) {
            var processor = d3.select(this);

            // -------------------
            // active thread count
            // -------------------

            nfCanvasUtils.activeThreadCount(processor, d);

            // ---------
            // bulletins
            // ---------

            processor.select('rect.bulletin-background').classed('has-bulletins', function () {
                return !nfCommon.isEmpty(d.status.aggregateSnapshot.bulletins);
            });

            nfCanvasUtils.bulletins(processor, d, function () {
                return d3.select('#processor-tooltips');
            }, 286);
        });
    };

    /**
     * Removes the processors in the specified selection.
     *
     * @param {selection} removed
     */
    var removeProcessors = function (removed) {
        if (removed.empty()) {
            return;
        }

        removed.call(removeTooltips).remove();
    };

    /**
     * Removes the tooltips for the processors in the specified selection.
     *
     * @param {selection} removed
     */
    var removeTooltips = function (removed) {
        removed.each(function (d) {
            // remove any associated tooltips
            $('#run-status-tip-' + d.id).remove();
            $('#bulletin-tip-' + d.id).remove();
            $('#comments-tip-' + d.id).remove();
        });
    };

    var nfProcessor = {
        /**
         * Initializes of the Processor handler.
         *
         * @param nfConnectableRef   The nfConnectable module.
         * @param nfDraggableRef   The nfDraggable module.
         * @param nfSelectableRef   The nfSelectable module.
         * @param nfContextMenuRef   The nfContextMenu module.
         * @param nfQuickSelectRef   The nfQuickSelect module.
         */
        init: function (nfConnectableRef, nfDraggableRef, nfSelectableRef, nfContextMenuRef, nfQuickSelectRef) {
            nfConnectable = nfConnectableRef;
            nfDraggable = nfDraggableRef;
            nfSelectable = nfSelectableRef;
            nfContextMenu = nfContextMenuRef;
            nfQuickSelect = nfQuickSelectRef;

            processorMap = d3.map();
            removedCache = d3.map();
            addedCache = d3.map();

            // create the processor container
            processorContainer = d3.select('#canvas').append('g')
                .attrs({
                    'pointer-events': 'all',
                    'class': 'processors'
                });
        },

        /**
         * Adds the specified processor entity.
         *
         * @param processorEntities       The processor
         * @param options           Configuration options
         */
        add: function (processorEntities, options) {
            var selectAll = false;
            if (nfCommon.isDefinedAndNotNull(options)) {
                selectAll = nfCommon.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
            }

            // get the current time
            var now = new Date().getTime();

            var add = function (processorEntity) {
                addedCache.set(processorEntity.id, now);
                // add the processor
                processorMap.set(processorEntity.id, $.extend({
                    type: 'Processor',
                    dimensions: smallDimensions,
                }, processorEntity));
            };

            // determine how to handle the specified processor
            if ($.isArray(processorEntities)) {
                $.each(processorEntities, function (_, processorEntity) {
                    add(processorEntity);
                });
            } else if (nfCommon.isDefinedAndNotNull(processorEntities)) {
                add(processorEntities);
            }

            // select
            var selection = select();



            // enter
            var entered = renderProcessors(selection.enter(), selectAll);

            // update
             updateProcessors(selection.merge(entered));
        },

        /**
         * Populates the graph with the specified processors.
         *
         * @argument {object | array} processorEntities                The processors to add
         * @argument {object} options                Configuration options
         */
        set: function (processorEntities, options) {
            var selectAll = false;
            var transition = false;
            var overrideRevisionCheck = false;
            if (nfCommon.isDefinedAndNotNull(options)) {
                selectAll = nfCommon.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
                transition = nfCommon.isDefinedAndNotNull(options.transition) ? options.transition : transition;
                overrideRevisionCheck = nfCommon.isDefinedAndNotNull(options.overrideRevisionCheck) ? options.overrideRevisionCheck : overrideRevisionCheck;
            }

            var set = function (proposedProcessorEntity) {
                var currentProcessorEntity = processorMap.get(proposedProcessorEntity.id);

                // set the processor if appropriate due to revision and wasn't previously removed
                if ((nfClient.isNewerRevision(currentProcessorEntity, proposedProcessorEntity) && !removedCache.has(proposedProcessorEntity.id)) || overrideRevisionCheck === true) {

                    var ele = d3.select('#id-' + proposedProcessorEntity.id);
                    var bigView = ele.select('g.processor-canvas-big-processor');
                    if(bigView.empty()){
                        var tmpDimensions = smallDimensions
                    } else {
                        var tmpDimensions = dimensions
                    }
                    processorMap.set(proposedProcessorEntity.id, $.extend({
                        type: 'Processor',
                        dimensions: tmpDimensions
                    }, proposedProcessorEntity));
                }
            };

            // determine how to handle the specified processor
            if ($.isArray(processorEntities)) {
                $.each(processorMap.keys(), function (_, key) {
                    var currentProcessorEntity = processorMap.get(key);
                    var isPresent = $.grep(processorEntities, function (proposedProcessorEntity) {
                        return proposedProcessorEntity.id === currentProcessorEntity.id;
                    });

                    // if the current processor is not present and was not recently added, remove it
                    if (isPresent.length === 0 && !addedCache.has(key)) {
                        processorMap.remove(key);
                    }
                });
                $.each(processorEntities, function (_, processorEntity) {
                    set(processorEntity);
                });
            } else if (nfCommon.isDefinedAndNotNull(processorEntities)) {
                set(processorEntities);
            }

            // select
            var selection = select();


            // enter
            var entered = renderProcessors(selection.enter(), selectAll);

            // update
            var updated = selection.merge(entered);
            updated.call(updateProcessors);
            updated.call(nfCanvasUtils.position, transition);

            // exit
            selection.exit().call(removeProcessors);
        },

        /**
         * If the processor id is specified it is returned. If no processor id
         * specified, all processors are returned.
         *
         * @param {string} id
         */
        get: function (id) {
            if (nfCommon.isUndefined(id)) {
                return processorMap.values();
            } else {
                return processorMap.get(id);
            }
        },

        /**
         * If the processor id is specified it is refresh according to the current
         * state. If not processor id is specified, all processors are refreshed.
         *
         * @param {string} id      Optional
         */
        refresh: function (id) {
            if (nfCommon.isDefinedAndNotNull(id)) {
                 d3.select('#id-' + id).call(updateProcessors);
            } else {
                 d3.selectAll('g.processor').call(updateProcessors);
            }
        },

        /**
         * Positions the component.
         *
         * @param {string} id   The id
         */
        position: function (id) {
            d3.select('#id-' + id).call(nfCanvasUtils.position);
        },

        /**
         * Refreshes the components necessary after a pan event.
         */
        pan: function () {
             d3.selectAll('g.processor.entering, g.processor.leaving').call(updateProcessors);
        },

        /**
         * Reloads the processor state from the server and refreshes the UI.
         * If the processor is currently unknown, this function just returns.
         *
         * @param {string} id The processor id
         */
        reload: function (id) {
            if (processorMap.has(id)) {
                var processorEntity = processorMap.get(id);
                return $.ajax({
                    type: 'GET',
                    url: processorEntity.uri,
                    headers:{
                        Locale: locale,
                    },
                    dataType: 'json'
                }).done(function (response) {
                    nfProcessor.set(response);
                });
            }
        },

        /**
         * Removes the specified processor.
         *
         * @param {array|string} processorIds      The processors
         */
        remove: function (processorIds) {
            var now = new Date().getTime();
            if ($.isArray(processorIds)) {
                $.each(processorIds, function (_, processorId) {
                    removedCache.set(processorId, now);
                    processorMap.remove(processorId);
                });
            } else {
                removedCache.set(processorIds, now);
                processorMap.remove(processorIds);
            }

            // apply the selection and handle all removed processors
            select().exit().call(removeProcessors);
        },

        /**
         * Removes all processors.
         */
        removeAll: function () {
            nfProcessor.remove(processorMap.keys());
        },

        /**
         * Expires the caches up to the specified timestamp.
         *
         * @param timestamp
         */
        expireCaches: function (timestamp) {
            var expire = function (cache) {
                cache.each(function (entryTimestamp, id) {
                    if (timestamp > entryTimestamp) {
                        cache.remove(id);
                    }
                });
            };

            expire(addedCache);
            expire(removedCache);
        },

        /**
         * Returns the default fill color that should be used when drawing a processor.
         */
        defaultFillColor: function () {
            return '#FFFFFF';
        },

        /**
         * Returns the default icon color that should be used when drawing a processor.
         */
        defaultIconColor: function () {
            return '#ad9897';
        }
    };

    return nfProcessor;
}));
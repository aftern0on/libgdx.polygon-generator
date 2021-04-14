package com.sunlight.instruments;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.utils.Array;

/**
 * A class for implementing collision optimization
 * Combines all tile collisions with each other whenever possible, minimizing the load on the
 * processing of each individual collision on the location
 *
 * To generate complex collisions, you need to specify all the vertices of its shape
 * This class contains tools for getting all the boundaries and then getting them vertexes
 *
 * @author aftern0on
 * @version 1.1 */

public class Collision {
    /** Table of collision objects **/
    public static class CellList {
        private final Array<Region> regions; // Rectangular areas

        /**
         * Creating a table of cells, iterating the matrix, searching for objects
         * Override {@link #filter} to filter out unnecessary tiles
         * @param layer collision tilemap layer */
        public CellList(TiledMapTileLayer layer) {
            this.regions = new Array<>();
            Region[][] builds = new Region[layer.getHeight() + 2][layer.getWidth() + 2];
            TiledMapTileLayer.Cell cell;
            Region start = null;
            for (int y = layer.getHeight(); y > 0; y--) {
                for (int x = 1; x < layer.getWidth() + 2; x++) {
                    cell = layer.getCell(x - 1, y - 1);
                    // Does the current cell fit the conditions
                    // It fits: generate a new region or increase the width of the current one
                    // If it doesn't fit: reset the start variable to zero
                    if (filter(cell, x - 1, y - 1)) {
                        // Does the parent exist
                        // If exists: increasing the width of the parent ignoring the current cell
                        // If it doesn't exist: create a new region, assign the cell to the parent
                        if (start != null) start.width += 1;
                        else {
                            builds[y][x] = new Region(x - 1, y - 1);
                            start = builds[y][x];
                        }
                    } else {
                        // The cell does not fit the conditions and cannot be part of the polygon
                        // Did the region exist before this cell
                        // If exists: check if there is a suitable region for merging above
                        if (start != null) {
                            Region upper = builds[start.y + 2][start.x + 1];
                            if (upper != null) {
                                // Is the current region suitable for merging
                                // The region above must have the same width as the current region
                                if (start.width == upper.width) start.merge(upper);
                                else regions.add(start);
                            } else regions.add(start);
                            start = null;
                        }
                    }
                }
            }
        }

        /** @return rectangular areas obtained during generation **/
        public Array<Region> getRegions() {
            return regions;
        }

        /**
         * This can be used to filter the objects that will be added to the cell list
         * If it returns true during generation the object given to it will be added to list
         * You can also view cell properties to see what tags are on the object
         * By default, all existing objects are filtered
         * Override this function as you like
         *
         * @param cell the cell to be processed during generation
         * @return permission for the generator to add a cell to a cell list */
        public boolean filter(TiledMapTileLayer.Cell cell, int x, int y) {
            return (cell != null);
        }

        /**
         * Cell object, tile to be combined
         * Stores the original cell data from the layer **/
        public static class Region {
            private Region parent; // Parent region
            public int x, y, width, height; // Transform parameters

            /** The element used for calculating polygons **/
            public Region(int x, int y) {
                this.x = x;
                this.y = y;
                this.width = 1;
                this.height = 1;
                this.parent = null;
            }

            /**
             * Combining regions with each other through the parent region
             * Changing the height parameters of the parent region when merging
             * @param upper the upper region to merge with */
            private void merge(Region upper) {
                parent = upper.getParent();
                parent.height += 1;
            }

            /**
             * Recursive method for returning the first parent region, which is the base
             * @return the first element in region (top-left corner) **/
            private Region getParent() {
                return (parent == null) ? this : parent.getParent();
            }

            /**
             * @return polygon of the current region */
            public Polygon getPolygon() {
                return new Polygon(new float[] {
                        x, y + 1,                  // Top-left
                        x + width, y + 1,          // Top-right
                        x + width, y - height + 1, // Down-right
                        x, y - height + 1          // Down-left
                });
            }
        }
    }
}
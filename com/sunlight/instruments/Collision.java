package com.sunlight.instruments;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;

import com.sunlight.instruments.Constants.Direction;
import com.sunlight.instruments.Constants.Position;

/**
 * A class for implementing collision optimization.
 * Combines all tile collisions with each other whenever possible, minimizing the load on the
 * processing of each individual collision on the location.
 *
 * To generate complex collisions, you need to specify all the vertices of its shape.
 * This class contains tools for getting all the boundaries and then getting them
 * vertexes. This class can:
 *
 * 1. Get all the regions of the  and generate their polygons.
 * 2. Get all the areas and split them into as few rectangular areas as possible.
 *
 * @author aftern0on
 * @version 1.1 */

public class Collision {
    /** Table of collision objects **/
    public static class CellList extends Group {
        private final Array<Polygon> polygons; // Found polygons of regions
        private final Array<Region> regions; // Tile regions
        private final Array<Cell.Bound> bounds; // All found bounds
        private final Cell[][] cells; // Table for interacting with neighboring objects
        private HashMap<Constants.Position, Position[]> turns; // Turns for defining boundaries

        /** Creating a table of cells, iterating the matrix, searching for objects **/
        public CellList(TiledMapTileLayer layer) {
            setPosition(1, 1);
            cells = new Cell[layer.getHeight() + 3][layer.getWidth() + 3];
            bounds = new Array<>();
            regions = new Array<>();
            polygons = new Array<>();
            buildTurnPriority();

            // Creating cells
            for (int y = 1; y < layer.getHeight() + 1; y++) {
                for (int x = 1; x < layer.getWidth() + 1; x++) {
                    TiledMapTileLayer.Cell cell = layer.getCell(x - 1, y - 1);
                    // This is where the filtering is performed
                    if (filter(cell, x, y)) {
                        addCell(new Cell(this, x - 1, y - 1,
                                (cell != null) ? cell.getTile() : null));
                    }
                }
            }

            // Processing cells, searching for neighbors, generating bounds
            for (int y = 1; y < layer.getHeight() + 1; y++) {
                for (int x = 1; x < layer.getWidth() + 1; x++) {
                    if (cells[y][x] != null) {
                        cells[y][x].releaseAround(); // The cell searches for and gets all neighbors
                        cells[y][x].releaseBounds(); // Cell generates borders
                    }
                }
            }

            // Connecting all borders to each other
            for (int y = 1; y < layer.getHeight() + 1; y++)
                for (int x = 1; x < layer.getWidth() + 1; x++)
                    if (cells[y][x] != null)
                        for (Cell.Bound bound : cells[y][x].bounds.values())
                            if (bound != null)
                                for (Cell.Bound overlap : bounds)
                                    bound.intersect(overlap);



            createPolygons();
            foundRectangleRegions();
        }

        /**
         * This can be used to filter the objects that will be added to the cell list
         * If it returns true during generation the object given to it will be added to list
         * You can also view cell properties to see what tags are on the object
         * By default, all existing objects are filtered
         * You can override this function as you like
         *
         * @param cell the cell to be processed during generation
         * @return permission for the generator to add a cell to a cell list
         **/
        public boolean filter(TiledMapTileLayer.Cell cell, int x, int y) {
            return (cell != null);
        }

        /** Create a new region **/
        private void addRegion(Cell start) {
            Region region = new Region(start, this);
            region.searchRow();
            addActor(region);
            regions.add(region);
        }

        /** Search and merge all regions into larger ones **/
        private void foundRectangleRegions() {
            // Creating regions
            for (Cell[] row : cells) {
                for (Cell element : row) {
                    if (element != null) {
                        if (element.region == null) {
                            addRegion(element);
                        }
                    }
                }
            }

            // Iterate regions from top to bottom, search for the next regions, and merge them
            regions.reverse();
            for (Region parent : regions) {
                // Checking the region
                // If the region is not a child then it can be a parent
                if (parent.merged == null) {
                    // If the region is not a child then it can be a parent
                    parent.isParent = true;

                    // Search for suitable child regions for merging
                    // The search will continue until all child regions are found
                    boolean founded; // Is the child region found
                    do {
                        founded = false;
                        for (int index = 0; index < regions.size; index++) {
                            Region child = regions.get(index);
                            if (child != parent && child.merged == null) {
                                // Search for a suitable child region
                                // The child and parent regions must be the same width
                                // Also, the child region must be strictly under the parent region
                                if ((parent.start.getX() == child.start.getX()) &&
                                        (parent.end.getX() == child.end.getX()) &&
                                        (parent.end.getY() - 1 == child.start.getY())) {
                                    founded = true;
                                    parent.mergeRegions(child);
                                    break; // End of search, start of a new one
                                }
                            }
                        }
                    } while (founded);
                }
            }
        }

        /** Setting priorities for the correct construction of the shape **/
        private void buildTurnPriority() {
            turns = new HashMap<>();

            turns.put(Position.TOP, new Position[]
                    {Position.LEFT, Position.DOWN, Position.TOP, Position.RIGHT});
            turns.put(Position.RIGHT, new Position[]
                    {Position.TOP, Position.LEFT, Position.RIGHT, Position.DOWN});
            turns.put(Position.DOWN, new Position[]
                    {Position.RIGHT, Position.TOP, Position.DOWN, Position.LEFT});
            turns.put(Position.LEFT, new Position[]
                    {Position.DOWN, Position.RIGHT, Position.LEFT, Position.TOP});
        }

        /** Creating area polygons **/
        public void createPolygons() {
            // Passing through all the found areas
            for (Cell.Bound bound : bounds) {
                if (!bound.used) {
                    // Sequentially plotting a bound
                    Cell.Bound previous = null;
                    Cell.Bound current = bound;
                    Array<Float> temp = new Array<>();

                    // Generating a bound based on a built path by priority
                    // Puts the corresponding coordinates in the list and then generates a polygon
                    do {
                        // The first item will be marked as last used
                        // This is done to properly complete polygon generation
                        current.used = previous != null;

                        // Checking all turns
                        for (Position turn : turns.get(current.position)) {
                            Cell.Bound next = current.overlaps.get(turn);
                            if (next != null) {
                                // Choosing the right turn
                                if (!next.used) {
                                    // Checking whether a vertex is on the same line
                                    // In this case, do not need to create a vertex
                                    if (previous == null) {
                                        temp.add(current.end.x + getX());
                                        temp.add(current.end.y + getY());
                                    } else if (!(previous.end.x == next.end.x ||
                                            previous.end.y == next.end.y)) {
                                        temp.add(current.end.x + getX());
                                        temp.add(current.end.y + getY());
                                    }
                                    previous = current;
                                    current = next;
                                    break;
                                }
                            }
                        }
                    } while (!current.used);

                    // Converting Array<Float> to float[]
                    float[] vertices = new float[temp.size];
                    for (int index = 0; index < temp.size; index++) {
                        vertices[index] = temp.get(index);
                    }

                    // Creating a polygon and adding it to the list
                    Polygon area = new Polygon();
                    area.setVertices(vertices);
                    polygons.add(area);
                }
            }
        }

        /** Getting all polygons **/
        public Array<Polygon> getPolygons() {
            return polygons;
        }

        /** Getting all rectangular areas **/
        public Array<Region> getRegions() {
            Array<Region> parents = new Array<>();

            // Collecting all parent regions
            for (Region region : regions) if (region.isParent) parents.add(region);

            return parents;
        }

        /** Get a cell from the matrix by position **/
        private Cell getCell(int x, int y) {
            return cells[y + 1][x + 1];
        }

        /** Adds new cells to the matrix of objects whose collisions need to be merged **/
        private void addCell(Cell cell) {
            cells[(int) cell.getY() + 1][(int) cell.getX() + 1] = cell;
            addActor(cell);
        }

        /** Cell region object. It can be a parent: it contains the entire area when combined **/
        public static class Region extends Group {
            private boolean isParent; // Is the region a parent region?
            private Region merged; // The first region from which the subsequent merger started
            public Cell start, end; // Start and end points for determining size of this region
            public CellList list; // Parent Table

            public Region(Cell start, CellList list) {
                start.addToRegion(this);
                this.isParent = false;
                this.list = list;
                this.merged = null;
                this.start = start;
                this.end = start;
            }

            /** Merge regions **/
            private void mergeRegions(Region child) {
                child.merged = this;
                this.isParent = true;
                this.end = child.end;
            }

            /** Getting a region polygon **/
            public Polygon getPolygon() {
                // Defining vertices
                float[] vertices = new float[8];

                vertices[0] = start.getX() + list.getX();
                vertices[1] = start.getY() + list.getY() + 1; // Top-right

                vertices[2] = end.getX() + list.getX() + 1;
                vertices[3] = start.getY() + list.getY() + 1; // Top-left

                vertices[4] = end.getX() + list.getX() + 1;
                vertices[5] = end.getY() + list.getY(); // Lower-left

                vertices[6] = start.getX() + list.getX();
                vertices[7] = end.getY() + list.getY(); // Bottom-right

                // The returned polygon can be used to build the body
                return new Polygon(vertices);
            }

            /** Getting a row of blocks **/
            private void searchRow() {
                Cell right = end.around.get(Direction.CENTER_RIGHT);
                if (right != null) {
                    end = right;
                    right.addToRegion(this);
                    searchRow();
                }
            }
        }

        /** Cell object, tile to be combined **/
        public static class Cell extends Group {
            public TiledMapTile tile; // Tile object from Tiled
            private Region region; // The row to which the cell belongs
            private final HashMap<Direction, Cell> around; // Adjacent cells
            private final HashMap<Position, Bound> bounds; // The boundaries of the future polygon
            private final CellList list; // The matrix to which the cell belongs

            public Cell(CellList list, int x, int y, TiledMapTile tile) {
                this.tile = tile;
                this.list = list;
                this.region = null;
                this.around = new HashMap<>();
                this.bounds = new HashMap<>();

                setBounds(x, y, 1, 1);
            }

            /** Adding a cell to a region **/
            public void addToRegion(Region region) {
                this.region = region;
            }

            /** Getting all nearby objects **/
            public void releaseAround() {
                for (Direction direction : Direction.values()) {
                    Cell cell = list.getCell(
                            (int) (getX() + direction.direction[0]),
                            (int) (getY() - direction.direction[1]));
                    around.put(direction, cell);
                }
            }

            /** Generating borders **/
            public void releaseBounds() {
                // Resetting all boundaries
                for (Position position : Position.values())
                    bounds.put(position, null);

                // Defining boundaries
                // Borders cannot exist in a side adjacent to another cell
                if (around.get(Direction.TOP) == null)
                    addBound(new Bound(Position.TOP, this));

                if (around.get(Direction.CENTER_RIGHT) == null)
                    addBound(new Bound(Position.RIGHT, this));

                if (around.get(Direction.DOWN) == null)
                    addBound(new Bound(Position.DOWN, this));

                if (around.get(Direction.CENTER_LEFT) == null)
                    addBound(new Bound(Position.LEFT, this));
            }

            /** Add a bound to a cell **/
            public void addBound(Bound bound) {
//                addActor(bound);
                list.bounds.add(bound);
                bounds.put(bound.getPosition(), bound);
            }

            /** The object of the contour on which the collision will be constructed **/
            public static class Bound extends Actor {
                private final HashMap<Position, Bound> overlaps; // Crossed borders
                private final Position position; // Position of the face relative to the cell
                public final Rectangle rect; // Position of the border relative to the cell
                public boolean used; // Position of the border relative to the cell
                public Vector2 end; // End of the border segment

                public Bound(Position position, Cell cell) {
                    this.overlaps = new HashMap<>();
                    this.position = position;
                    this.rect = new Rectangle();
                    this.end = new Vector2(
                            cell.getX() + position.end.x,
                            cell.getY() + position.end.y);

                    this.used = false;

                    // Position of the border relative to the cell
                    setPosition(position.start.x, position.start.y);

                    // Border size
                    if (position == Position.TOP || position == Position.DOWN)
                        setSize(1.1f, 0.1f);
                    else
                        setSize(0.1f, 1.1f);
                    this.rect.set(
                            cell.getX() + getX(), cell.getY() + getY(),
                            getWidth(), getHeight());
                }

                // Getting the border position
                public Position getPosition() {
                    return position;
                }

                // Checking if the end of the current border intersects with this one
                public void intersect(Bound bound) {
                    if (Intersector.intersectSegmentRectangle(
                            end.x + 0.01f,
                            end.y + 0.01f,
                            end.x + 0.02f,
                            end.y + 0.02f, bound.rect) && !this.equals(bound)) {
                        overlaps.put(bound.position, bound);
                    }
                }
            }
        }
    }
}